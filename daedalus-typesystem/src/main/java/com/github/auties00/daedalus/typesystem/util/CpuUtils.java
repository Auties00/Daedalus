package com.github.auties00.daedalus.typesystem.util;

import java.lang.foreign.*;

/**
 * Static utility methods for detecting the features of the processor that select between the
 * code paths of performance critical algorithms.
 *
 * <p>The JDK exposes no API to query the instruction sets of the processor, or how fast they
 * are, so the features are detected by running the {@code CPUID} instruction once, when the
 * class is initialized, through the foreign function and memory API, which requires native
 * access.
 *
 * <p>{@link #isFastBmi2Supported()} detects whether the processor implements the {@code PEXT}
 * and {@code PDEP} instructions of the BMI2 instruction set in hardware:
 * {@link Integer#compress(int, int)}, {@link Long#compress(long, long)},
 * {@link Integer#expand(int, int)} and {@link Long#expand(long, long)} are intrinsified with
 * these instructions where they are available, but some processors implement them in microcode,
 * which is slower than the bitwise fallback that does not use them.
 *
 * <p>This class is thread safe.
 */
public final class CpuUtils {
    // CPUID leaf 7, subleaf 0 -> EBX (structured extended feature flags)
    //   push rbx; mov eax,7; xor ecx,ecx; cpuid; mov eax,ebx; pop rbx; ret
    private static final byte[] CPUID_LEAF7_EBX = {
            0x53,
            (byte) 0xB8, 0x07, 0x00, 0x00, 0x00,
            0x31, (byte) 0xC9,
            0x0F, (byte) 0xA2,
            (byte) 0x89, (byte) 0xD8,
            0x5B,
            (byte) 0xC3
    };

    // CPUID leaf 0 -> EBX (first 4 bytes of vendor string)
    //   push rbx; xor eax,eax; xor ecx,ecx; cpuid; mov eax,ebx; pop rbx; ret
    private static final byte[] CPUID_LEAF0_EBX = {
            0x53,
            0x31, (byte) 0xC0,
            0x31, (byte) 0xC9,
            0x0F, (byte) 0xA2,
            (byte) 0x89, (byte) 0xD8,
            0x5B,
            (byte) 0xC3
    };

    // CPUID leaf 1 -> EAX (family / model / stepping)
    //   push rbx; mov eax,1; xor ecx,ecx; cpuid; pop rbx; ret
    private static final byte[] CPUID_LEAF1_EAX = {
            0x53,
            (byte) 0xB8, 0x01, 0x00, 0x00, 0x00,
            0x31, (byte) 0xC9,
            0x0F, (byte) 0xA2,
            0x5B,
            (byte) 0xC3
    };

    // bit 8 of CPUID.(EAX=7,ECX=0):EBX
    private static final int BMI2_BIT = 1 << 8;

    // CPUID leaf 0 EBX for vendor identification
    private static final int VENDOR_INTEL_EBX = 0x756E6547; // "Genu"ineIntel
    private static final int VENDOR_AMD_EBX   = 0x68747541; // "Auth"enticAMD

    // AMD Zen 3 family, first generation with native PEXT/PDEP ALU hardware
    private static final int CPU_FAMILY_AMD_19H = 0x19;

    // CPUID EAX bit-field extraction constants (leaf 1)
    private static final int BASE_FAMILY_SHIFT     = 8;
    private static final int BASE_FAMILY_MASK      = 0xF;
    private static final int EXTENDED_FAMILY_SHIFT = 20;
    private static final int EXTENDED_FAMILY_MASK  = 0xFF;
    private static final int EXTENDED_FAMILY_PREFIX = 0xF;

    // mmap protection flags
    private static final int PROT_READ  = 1;
    private static final int PROT_WRITE = 2;
    private static final int PROT_EXEC  = 4;

    // mmap flags
    private static final int MAP_PRIVATE   = 0x02;
    // Linux
    private static final int MAP_ANONYMOUS = 0x20;
    // macOS
    private static final int MAP_ANON      = 0x1000;

    // mmap sentinel values
    private static final int MMAP_NO_FD     = -1;
    private static final long MMAP_NO_OFFSET = 0L;

    // Windows memory allocation constants
    private static final int MEM_COMMIT             = 0x1000;
    private static final int MEM_RESERVE            = 0x2000;
    private static final int MEM_COMMIT_RESERVE     = MEM_COMMIT | MEM_RESERVE;
    private static final int MEM_RELEASE            = 0x8000;
    private static final int PAGE_EXECUTE_READWRITE = 0x40;

    private static final long PAGE_SIZE = 4096;

    /**
     * Whether the processor implements {@code PEXT} and {@code PDEP} in hardware.
     */
    private static final boolean FAST_BMI2_SUPPORT;

    static {
        try {
            FAST_BMI2_SUPPORT = computeFastBmi2Support();
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * Detects whether the processor implements {@code PEXT} and {@code PDEP} in hardware.
     *
     * @return {@code true} if the processor is an x86 processor that implements them in hardware
     * @throws Throwable if the native functions used by the detection fail
     */
    // The BMI2 instruction set includes PEXT (parallel bits extract) and PDEP
    // (parallel bits deposit), which are used to intrinsify Integer/Long.compress
    // and Integer/Long.expand (added in JDK 19, see https://bugs.openjdk.org/browse/JDK-8283893).
    //
    // While all BMI2-capable CPUs can execute these instructions, PEXT and PDEP
    // are unique in that some vendors implement them via microcode rather than
    // native ALU hardware. The microcoded versions are significantly slower (high latency/low throughput)
    // than the manual bitwise fallback used in the Java implementation.
    // Conversely, all other BMI2 instructions (BZHI, MULX, RORX, SARX, SHRX, SHLX)
    // execute efficiently on every BMI2-capable CPU and are unaffected by this check.
    //
    // The logic in this method is based on official optimization guides from hardware vendors,
    // to guarantee that microcode implementations of PEXT/PDEP are not used.
    //
    // Mirrors my work at https://github.com/openjdk/jdk/pull/29809
    // TODO: Look for a way to do it without native access (will require runtime flag in the future)
    private static boolean computeFastBmi2Support() throws Throwable {
        var arch = System.getProperty("os.arch");
        if (!"amd64".equals(arch) && !"x86_64".equals(arch)) {
            return false; // BMI2 is x86-only
        }

        var os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return isSupportedOnWindows();
        } else if (os.contains("mac")) {
            return isSupportedOnUnix(MAP_ANON);
        } else {
            return isSupportedOnUnix(MAP_ANONYMOUS);
        }
    }

    /**
     * Detects the support on Linux and macOS, running the detection in a page mapped by
     * {@code mmap}.
     *
     * @param mapFlag the flag that requests an anonymous mapping on the current system
     * @return {@code true} if the processor implements the instructions in hardware
     * @throws Throwable if the native functions used by the detection fail
     */
    private static boolean isSupportedOnUnix(int mapFlag) throws Throwable {
        var linker = Linker.nativeLinker();
        var std = linker.defaultLookup();

        // void *mmap(void*, size_t, int prot, int flags, int fd, off_t)
        var mmap = linker.downcallHandle(
                std.find("mmap").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

        // int munmap(void*, size_t)
        var munmap = linker.downcallHandle(
                std.find("munmap").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

        var page = (MemorySegment)
                mmap.invoke(MemorySegment.NULL, PAGE_SIZE,
                        PROT_READ | PROT_WRITE | PROT_EXEC,
                        MAP_PRIVATE | mapFlag,
                        MMAP_NO_FD, MMAP_NO_OFFSET);

        try {
            page = page.reinterpret(PAGE_SIZE);
            return checkFastBmi2(linker, page);
        } finally {
            munmap.invoke(page, PAGE_SIZE);
        }
    }

    /**
     * Detects the support on Windows, running the detection in a page allocated by
     * {@code VirtualAlloc}.
     *
     * @return {@code true} if the processor implements the instructions in hardware
     * @throws Throwable if the native functions used by the detection fail
     */
    private static boolean isSupportedOnWindows() throws Throwable {
        var linker = Linker.nativeLinker();
        var kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());

        // LPVOID VirtualAlloc(LPVOID, SIZE_T, DWORD, DWORD)
        var virtualAlloc = linker.downcallHandle(
                kernel32.find("VirtualAlloc").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

        // BOOL VirtualFree(LPVOID, SIZE_T, DWORD)
        var virtualFree = linker.downcallHandle(
                kernel32.find("VirtualFree").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT));

        var page = (MemorySegment)
                virtualAlloc.invoke(MemorySegment.NULL, PAGE_SIZE,
                        MEM_COMMIT_RESERVE,
                        PAGE_EXECUTE_READWRITE);

        try {
            page = page.reinterpret(PAGE_SIZE);
            return checkFastBmi2(linker, page);
        } finally {
            virtualFree.invoke(page, 0L, MEM_RELEASE);
        }
    }

    /**
     * Detects the support by running {@code CPUID} stubs in the given executable page, and
     * checking the vendor and the family of the processor.
     *
     * @param linker the native linker
     * @param page the executable page the stubs are copied into
     * @return {@code true} if the processor implements the instructions in hardware
     * @throws Throwable if a stub cannot be invoked
     */
    private static boolean checkFastBmi2(Linker linker, MemorySegment page) throws Throwable {
        // Check BMI2 support via CPUID leaf 7
        var leaf7ebx = callCpuidStub(linker, page, CPUID_LEAF7_EBX);
        if ((leaf7ebx & BMI2_BIT) == 0) {
            return false;
        }

        // Identify CPU vendor via CPUID leaf 0
        var vendorEbx = callCpuidStub(linker, page, CPUID_LEAF0_EBX);

        if (vendorEbx == VENDOR_INTEL_EBX) {
            // All Intel CPUs with BMI2 (Haswell+) implement PEXT/PDEP natively.
            // 3-cycle latency, 1-per-cycle throughput on a dedicated ALU port.
            // Source: Intel Intrinsics Guide, https://www.intel.com/content/www/us/en/docs/intrinsics-guide/index.html
            return true;
        }

        if (vendorEbx == VENDOR_AMD_EBX) {
            // AMD added BMI2 in Excavator (Family 0x15, model 0x60+) but used
            // microcode for PEXT/PDEP through all of Zen 2 (Family 0x17).
            // Native ALU hardware support arrived with Zen 3 (Family 0x19).
            // Source: AMD Software Optimization Guide (doc #56665), Section 2.10.2,
            //         https://developer.amd.com/resources/developer-guides-manuals/
            var family = extractCpuFamily(linker, page);
            return family >= CPU_FAMILY_AMD_19H;
        }

        // Zhaoxin added BMI2 support in Lujiazui (KX-6000+).
        // Based on community benchmarks (https://uops.info/html-instr/PDEP_R64_R64_R64.html),
        // PEXT/PDEP performance is known to be similarly poor to pre-Zen3 AMD,
        // suggesting a microcode implementation.
        // This cannot be confirmed as Zhaoxin publishes no public optimization guide.

        // On VIA/Centaur CNS, BMI2 is implemented in hardware with PDEP/PEXT
        // executing at two per cycle (better than Haswell).
        // Intel acquired Centaur in 2021, and CNS never reached production,
        // so we don't check for it.
        return false;
    }

    /**
     * Returns the family of the processor, combining its base and extended families.
     *
     * @param linker the native linker
     * @param page the executable page the stub is copied into
     * @return the family of the processor
     * @throws Throwable if the stub cannot be invoked
     */
    private static int extractCpuFamily(Linker linker, MemorySegment page) throws Throwable {
        var eax = callCpuidStub(linker, page, CPUID_LEAF1_EAX);
        var baseFamily = (eax >> BASE_FAMILY_SHIFT) & BASE_FAMILY_MASK;
        if (baseFamily == EXTENDED_FAMILY_PREFIX) {
            return baseFamily + ((eax >> EXTENDED_FAMILY_SHIFT) & EXTENDED_FAMILY_MASK);
        }
        return baseFamily;
    }

    /**
     * Copies the given machine code into the given executable page and invokes it.
     *
     * @param linker the native linker
     * @param page the executable page
     * @param stub the machine code, which returns an {@code int}
     * @return the value returned by the machine code
     * @throws Throwable if the machine code cannot be invoked
     */
    private static int callCpuidStub(Linker linker, MemorySegment page, byte[] stub) throws Throwable {
        MemorySegment.copy(stub, 0, page, ValueLayout.JAVA_BYTE, 0, stub.length);
        var handle = linker.downcallHandle(page, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        return (int) handle.invoke();
    }

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private CpuUtils() {
        throw new UnsupportedOperationException("This is a utility class and it cannot be instantiated.");
    }

    /**
     * Returns whether the processor implements the {@code PEXT} and {@code PDEP} instructions
     * of the BMI2 instruction set in hardware.
     *
     * <p>A processor that implements them in microcode, like the ones of AMD before Zen 3, is
     * not considered to support them, as the microcode is slower than the bitwise fallback.
     *
     * @return {@code true} if the instructions are implemented in hardware
     */
    public static boolean isFastBmi2Supported() {
        return FAST_BMI2_SUPPORT;
    }
}
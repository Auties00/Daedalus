package com.github.auties00.daedalus.cli.tui;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Reads the state of the modifier keys, which a terminal cannot report together with a key.
 *
 * <p>A terminal sends the same character for Enter and for Shift and Enter, so the key press
 * that the TUI receives does not tell whether Shift was held. On Windows the state of the
 * keyboard can be asked at the moment a key press is handled, which is what this class does.
 * On the other platforms the state is not available and no modifier is ever reported, so
 * actions that depend on a modifier also need a key binding that does not.
 */
public final class ModifierKeys {
    private static final int VIRTUAL_KEY_SHIFT = 0x10;
    private static final int KEY_DOWN = 0x8000;
    private static final MethodHandle GET_ASYNC_KEY_STATE = findGetAsyncKeyState();

    private ModifierKeys() {
    }

    /**
     * Returns whether the state of the modifier keys can be read on this platform.
     *
     * @return {@code true} if {@link #isShiftPressed()} can report that Shift is held
     */
    public static boolean isSupported() {
        return GET_ASYNC_KEY_STATE != null;
    }

    /**
     * Returns whether the Shift key is held down right now.
     *
     * @return {@code true} if Shift is held down, and {@code false} if it is not or if the
     *         state cannot be read on this platform
     */
    public static boolean isShiftPressed() {
        if (GET_ASYNC_KEY_STATE == null) {
            return false;
        }

        try {
            var state = (short) GET_ASYNC_KEY_STATE.invokeExact(VIRTUAL_KEY_SHIFT);
            return (state & KEY_DOWN) != 0;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static MethodHandle findGetAsyncKeyState() {
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            return null;
        }

        try {
            var user32 = SymbolLookup.libraryLookup("user32", Arena.global());
            var function = user32.find("GetAsyncKeyState");
            if (function.isEmpty()) {
                return null;
            }

            return Linker.nativeLinker().downcallHandle(
                    function.get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_INT)
            );
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }
}

package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.manager.DaedalusConverterParametersManager;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusMixinScopeManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterArgument;
import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class DaedalusConverterGraphTest {

    private static final String TEST_SOURCE = """
            package test;

            import java.util.Optional;

            record PhoneNumber() {}

            public class Converters {
                public static <T> T unwrap(Optional<T> opt) {
                    return opt.orElse(null);
                }

                public static <T> Optional<T> wrap(T value) {
                    return Optional.ofNullable(value);
                }
            }

            class ScopedConverters {
            }

            class TimeConverters {
                public static java.time.Instant ofEpoch(Long value, java.util.concurrent.TimeUnit unit) {
                    return null;
                }

                public static Long toEpochSeconds(java.time.Instant value) {
                    return null;
                }

                public static int sizeOf(java.time.Instant value, java.util.concurrent.TimeUnit unit) {
                    return 0;
                }

                public static void write(java.time.Instant value, StringBuilder writer, java.util.concurrent.TimeUnit unit) {

                }
            }

            @interface TimeConvertersParameters {
                java.util.concurrent.TimeUnit unit();
            }

            class FallbackConverters {
                public static java.time.Instant parse(String value) {
                    return null;
                }
            }

            class WideConverters {
                public static String convert(String value, long number, char character, String text, Class<?> type, int[] numbers, float decimal, boolean flag) {
                    return value;
                }
            }

            @interface WideConvertersParameters {
                long number();
                char character();
                String text();
                Class<?> type();
                int[] numbers();
                float decimal();
                boolean flag();
            }

            enum Color {
                RED,
                GREEN
            }

            class UseSites {
                @TimeConvertersParameters(unit = java.util.concurrent.TimeUnit.SECONDS)
                java.time.Instant timestamp;

                @WideConvertersParameters(number = -5, character = '\\n', text = "a\\"b", type = String[].class, numbers = {1, 2}, decimal = 1.5f, flag = true)
                String wide;

                java.time.Instant plain;
            }
            """;

    private static DaedalusTypeManager types;
    private static TypeMirror stringType;
    private static TypeMirror integerType;
    private static TypeMirror longType;
    private static TypeMirror doubleType;
    private static TypeMirror numberType;
    private static TypeMirror optionalOfInteger;
    private static TypeMirror optionalOfLong;
    private static TypeMirror phoneNumberType;
    private static TypeMirror optionalOfPhoneNumber;
    private static TypeElement convertersElement;
    private static TypeElement scopedConvertersElement;
    private static TypeMirror primitiveLongType;
    private static ExecutableElement unwrapMethod;
    private static ExecutableElement wrapMethod;
    private static DaedalusConverterParametersManager parametersManager;
    private static TypeMirror instantType;
    private static TypeElement timeConvertersElement;
    private static TypeElement fallbackConvertersElement;
    private static TypeElement useSitesElement;
    private static ExecutableElement ofEpochMethod;
    private static ExecutableElement toEpochSecondsMethod;
    private static ExecutableElement sizeOfMethod;
    private static ExecutableElement writeMethod;
    private static ExecutableElement parseMethod;
    private static ExecutableElement wideConvertMethod;
    private static TypeMirror colorType;

    @TestFactory
    List<DynamicTest> converterGraphTests() {
        var tests = Collections.synchronizedList(new ArrayList<DynamicTest>());
        var source = new SimpleJavaFileObject(URI.create("string:///test/Converters.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return TEST_SOURCE;
            }
        };

        var compiler = ToolProvider.getSystemJavaCompiler();
        var task = compiler.getTask(
                null,
                null,
                null,
                // The type system provides the exceptions referenced by the converters that the graph synthesizes
                List.of("--enable-preview", "--source", "26", "-proc:only", "--class-path", typeSystemLocation()),
                null,
                List.of(source)
        );

        task.setProcessors(List.of(new AbstractProcessor() {
            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                if (roundEnv.processingOver()) {
                    return false;
                }

                var elements = processingEnv.getElementUtils();
                var typeUtils = processingEnv.getTypeUtils();
                var optionalElement = elements.getTypeElement(Optional.class.getName());

                types = new DaedalusTypeManager(processingEnv);
                stringType = elements.getTypeElement(String.class.getName()).asType();
                integerType = elements.getTypeElement(Integer.class.getName()).asType();
                longType = elements.getTypeElement(Long.class.getName()).asType();
                doubleType = elements.getTypeElement(Double.class.getName()).asType();
                numberType = elements.getTypeElement(Number.class.getName()).asType();
                optionalOfInteger = typeUtils.getDeclaredType(optionalElement, integerType);
                optionalOfLong = typeUtils.getDeclaredType(optionalElement, longType);
                phoneNumberType = elements.getTypeElement("test.PhoneNumber").asType();
                optionalOfPhoneNumber = typeUtils.getDeclaredType(optionalElement, phoneNumberType);
                convertersElement = elements.getTypeElement("test.Converters");
                scopedConvertersElement = elements.getTypeElement("test.ScopedConverters");
                primitiveLongType = typeUtils.getPrimitiveType(TypeKind.LONG);
                unwrapMethod = findMethod(convertersElement, "unwrap");
                wrapMethod = findMethod(convertersElement, "wrap");
                parametersManager = new DaedalusConverterParametersManager(processingEnv, types, Set.of(elements.getTypeElement(StringBuilder.class.getName()).asType()));
                instantType = elements.getTypeElement(Instant.class.getName()).asType();
                timeConvertersElement = elements.getTypeElement("test.TimeConverters");
                fallbackConvertersElement = elements.getTypeElement("test.FallbackConverters");
                useSitesElement = elements.getTypeElement("test.UseSites");
                ofEpochMethod = findMethod(timeConvertersElement, "ofEpoch");
                toEpochSecondsMethod = findMethod(timeConvertersElement, "toEpochSeconds");
                sizeOfMethod = findMethod(timeConvertersElement, "sizeOf");
                writeMethod = findMethod(timeConvertersElement, "write");
                parseMethod = findMethod(fallbackConvertersElement, "parse");
                wideConvertMethod = findMethod(elements.getTypeElement("test.WideConverters"), "convert");
                colorType = elements.getTypeElement("test.Color").asType();

                tests.add(dynamicTest("direct conversion finds a single-step path", DaedalusConverterGraphTest.this::testDirectConversion));
                tests.add(dynamicTest("two-step conversion chains through an intermediate type", DaedalusConverterGraphTest.this::testTwoStepChain));
                tests.add(dynamicTest("three-step conversion chains through two intermediate types", DaedalusConverterGraphTest.this::testThreeStepChain));
                tests.add(dynamicTest("returns empty when no converter path exists", DaedalusConverterGraphTest.this::testNoPath));
                tests.add(dynamicTest("returns empty when the graph has no converters", DaedalusConverterGraphTest.this::testEmptyGraph));
                tests.add(dynamicTest("returns empty when source and target are the same type with no converter", DaedalusConverterGraphTest.this::testSameSourceAndTarget));
                tests.add(dynamicTest("matches a converter whose declared input is a supertype of the actual source", DaedalusConverterGraphTest.this::testSupertypeInput));
                tests.add(dynamicTest("terminates when bidirectional converters form a cycle with no exit", DaedalusConverterGraphTest.this::testBidirectionalCycleNoExit));
                tests.add(dynamicTest("finds a path past a bidirectional cycle when an exit converter exists", DaedalusConverterGraphTest.this::testBidirectionalCycleWithExit));
                tests.add(dynamicTest("terminates when three converters form a triangle cycle with no exit", DaedalusConverterGraphTest.this::testTriangleCycle));
                tests.add(dynamicTest("terminates when a converter maps a type to itself", DaedalusConverterGraphTest.this::testSelfLoopConverter));
                tests.add(dynamicTest("discovers a path that mixes a deserializer step followed by a serializer step", DaedalusConverterGraphTest.this::testCrossDirectionDeserializerThenSerializer));
                tests.add(dynamicTest("discovers a path that mixes a serializer step followed by a deserializer step", DaedalusConverterGraphTest.this::testCrossDirectionSerializerThenDeserializer));
                tests.add(dynamicTest("resolves type arguments for a parametrized unwrap converter", DaedalusConverterGraphTest.this::testParametrizedUnwrap));
                tests.add(dynamicTest("resolves type arguments for a parametrized wrap converter", DaedalusConverterGraphTest.this::testParametrizedWrap));
                tests.add(dynamicTest("chains a concrete step with a parametrized unwrap step", DaedalusConverterGraphTest.this::testMultiHopWithParametrizedUnwrap));
                tests.add(dynamicTest("allows a converter whose owner matches a provided mixin", DaedalusConverterGraphTest.this::testLegalThroughMixin));
                tests.add(dynamicTest("rejects a converter whose owner matches no mixin, source, or target", DaedalusConverterGraphTest.this::testIllegalWithoutMatchingOwner));
                tests.add(dynamicTest("allows a converter whose owner matches the source type", DaedalusConverterGraphTest.this::testLegalThroughSourceType));
                tests.add(dynamicTest("allows a converter whose owner matches the target type", DaedalusConverterGraphTest.this::testLegalThroughTargetType));
                tests.add(dynamicTest("allows a converter whose owner matches the generated Spec name of a managed type", DaedalusConverterGraphTest.this::testLegalThroughManagedType));
                tests.add(dynamicTest("finds at least one valid path when multiple paths exist", DaedalusConverterGraphTest.this::testMultiplePaths));
                tests.add(dynamicTest("terminates in a fully connected graph with no reachable target", DaedalusConverterGraphTest.this::testDenseGraphWithCycles));
                tests.add(dynamicTest("finds a forward path and ignores a back-edge that would revisit an intermediate type", DaedalusConverterGraphTest.this::testBackEdgeIgnored));
                tests.add(dynamicTest("matches a converter accepting Number when the source is Integer (subtype assignability via mixin)", DaedalusConverterGraphTest.this::testSubtypeAssignabilityViaMixin));
                tests.add(dynamicTest("finds a cross-direction path from long to PhoneNumber through Optional with parametrized unwrap", DaedalusConverterGraphTest.this::testOriginalLimitationCrossDirectionWithParametrizedUnwrap));
                tests.add(dynamicTest("returns the shortest path even if a longer one uses converters linked first", DaedalusConverterGraphTest.this::testShortestPathPreferred));
                tests.add(dynamicTest("avoids a round trip from long through a wrapper back to java.lang.Long", DaedalusConverterGraphTest.this::testRoundTripDetourAvoided));
                tests.add(dynamicTest("prefers a longer path in the explicit tier over a shorter one in a scoped tier", DaedalusConverterGraphTest.this::testExplicitTierPrecedence));
                tests.add(dynamicTest("falls back to a scoped tier when the explicit tier has no path", DaedalusConverterGraphTest.this::testScopedTierFallback));
                tests.add(dynamicTest("accepts a final converter whose return type is a subtype of the target", DaedalusConverterGraphTest.this::testSubtypeReturnReachesTarget));
                tests.add(dynamicTest("rejects a final converter whose return type is a supertype of the target", DaedalusConverterGraphTest.this::testSupertypeReturnRejected));
                tests.add(dynamicTest("breaks ties between paths of the same length by link order", DaedalusConverterGraphTest.this::testDeterministicTie));
                tests.add(eagerTest("skips a converter whose parameters annotation is not supplied in favor of a longer path", DaedalusConverterGraphTest.this::testUnsuppliedArgumentsFallback));
                tests.add(eagerTest("uses a converter whose parameters annotation is supplied", DaedalusConverterGraphTest.this::testSuppliedArguments));
                tests.add(eagerTest("prefers a converter whose parameters annotation is supplied over a plain one of the same length", DaedalusConverterGraphTest.this::testSuppliedArgumentsPreferred));
                tests.add(dynamicTest("prefers the converter declared for the exact source type over one declared for a supertype", DaedalusConverterGraphTest.this::testExactConverterPreferred));
                tests.add(eagerTest("reports the paths that need a missing parameters annotation", DaedalusConverterGraphTest.this::testMissingArguments));
                tests.add(eagerTest("reports no path when none exists even ignoring the parameters annotations", DaedalusConverterGraphTest.this::testNotFoundIgnoringArguments));
                tests.add(eagerTest("requires the parameters annotation of the sizer attached to a serializer", DaedalusConverterGraphTest.this::testSizerArguments));
                tests.add(eagerTest("considers a concrete and a synthetic parameters annotation with the same name equal", DaedalusConverterGraphTest.this::testParametersElementEquality));
                tests.add(eagerTest("reads the arguments of a converter from the parameters annotation of a use site", DaedalusConverterGraphTest.this::testArgumentsFromUseSite));
                tests.add(eagerTest("renders every supported annotation value as a Java expression", DaedalusConverterGraphTest.this::testArgumentExpressions));
                tests.add(eagerTest("renders the invocation of converters with additional parameters", DaedalusConverterGraphTest.this::testInvocation));
                tests.add(eagerTest("links name converters for an unmanaged enum without reflection", DaedalusConverterGraphTest.this::testUnmanagedEnum));
                tests.add(eagerTest("links no name converters for an enum managed by an extension", DaedalusConverterGraphTest.this::testManagedEnum));
                tests.add(eagerTest("prefers a scoped mixin over the name converters of an unmanaged enum", DaedalusConverterGraphTest.this::testUnmanagedEnumFallback));

                return false;
            }

            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Set.of("*");
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latestSupported();
            }
        }));

        var success = task.call();
        assertTrue(success, "Compilation failed");
        assertFalse(tests.isEmpty(), "No tests were registered (processor did not run)");
        return tests;
    }

    // Path finding

    // Links a single converter from String to Integer and expects findPath to return that one arc
    private void testDirectConversion() {
        var graph = newGraph();
        var method = syntheticMethod(String.class.getName(), integerType, "toInteger", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Links String to Integer to Long and expects findPath(String, Long) to chain both converters in order
    private void testTwoStepChain() {
        var graph = newGraph();
        var m1 = syntheticMethod(String.class.getName(), integerType, "toInteger", stringType);
        var m2 = syntheticMethod(Integer.class.getName(), longType, "toLong", integerType);
        graph.link(stringType, integerType, m1);
        graph.link(integerType, longType, m2);

        var path = findPath(graph, stringType, longType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(m1, path.get(0).method());
        assertSame(m2, path.get(1).method());
    }

    // Links String to Integer to Long to Double and expects all three converters chained in order
    private void testThreeStepChain() {
        var graph = newGraph();
        var m1 = syntheticMethod(String.class.getName(), integerType, "toInteger", stringType);
        var m2 = syntheticMethod(Integer.class.getName(), longType, "toLong", integerType);
        var m3 = syntheticMethod(Long.class.getName(), doubleType, "toDouble", longType);
        graph.link(stringType, integerType, m1);
        graph.link(integerType, longType, m2);
        graph.link(longType, doubleType, m3);

        var path = findPath(graph, stringType, doubleType, DaedalusMixins.of(Set.of()));
        assertEquals(3, path.size());
        assertSame(m1, path.get(0).method());
        assertSame(m2, path.get(1).method());
        assertSame(m3, path.get(2).method());
    }

    // Links String to Integer only, then searches for String to Double which has no reachable path
    private void testNoPath() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));

        var path = findPath(graph, stringType, doubleType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Searches an empty graph, which has no converters at all
    private void testEmptyGraph() {
        var graph = newGraph();
        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Searches for a conversion from String to String with no converters linked
    private void testSameSourceAndTarget() {
        var graph = newGraph();
        var path = findPath(graph, stringType, stringType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links Integer to Number and expects it to match, since Integer is directly assignable to Number
    private void testSupertypeInput() {
        var graph = newGraph();
        var method = syntheticMethod(Integer.class.getName(), numberType, "toNumber", integerType);
        graph.link(integerType, numberType, method);

        var path = findPath(graph, integerType, numberType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Cycle detection

    // Links String to Integer and Integer back to String, then searches for String to Double.
    // Without cycle detection this would loop forever; expects termination with an empty result.
    private void testBidirectionalCycleNoExit() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));
        graph.link(integerType, stringType, syntheticMethod(Integer.class.getName(), stringType, "toString", integerType));

        var path = findPath(graph, stringType, doubleType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links String to Integer, Integer back to String (cycle), and Integer to Long (exit).
    // Expects the graph to find String to Integer to Long, ignoring the back-edge to String.
    private void testBidirectionalCycleWithExit() {
        var graph = newGraph();
        var m1 = syntheticMethod(String.class.getName(), integerType, "toInteger", stringType);
        var m3 = syntheticMethod(Integer.class.getName(), longType, "toLong", integerType);
        graph.link(stringType, integerType, m1);
        graph.link(integerType, stringType, syntheticMethod(Integer.class.getName(), stringType, "toString", integerType));
        graph.link(integerType, longType, m3);

        var path = findPath(graph, stringType, longType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(m1, path.get(0).method());
        assertSame(m3, path.get(1).method());
    }

    // Links String to Integer to Long to String (triangle cycle), then searches for String to Double.
    // Every reachable type loops back, so the result should be empty.
    private void testTriangleCycle() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));
        graph.link(integerType, longType, syntheticMethod(Integer.class.getName(), longType, "toLong", integerType));
        graph.link(longType, stringType, syntheticMethod(Long.class.getName(), stringType, "toString", longType));

        var path = findPath(graph, stringType, doubleType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links String to itself (identity converter), then searches for String to Integer.
    // The self-loop should not cause infinite recursion.
    private void testSelfLoopConverter() {
        var graph = newGraph();
        graph.link(stringType, stringType, syntheticMethod(String.class.getName(), stringType, "identity", stringType));

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Creates a fully connected graph between String, Integer, and Long (six bidirectional edges),
    // then searches for String to Double which is unreachable. Tests that cycle detection handles
    // multiple overlapping cycles without hanging.
    private void testDenseGraphWithCycles() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "si", stringType));
        graph.link(integerType, stringType, syntheticMethod(Integer.class.getName(), stringType, "is", integerType));
        graph.link(integerType, longType, syntheticMethod(Integer.class.getName(), longType, "il", integerType));
        graph.link(longType, integerType, syntheticMethod(Long.class.getName(), integerType, "li", longType));
        graph.link(longType, stringType, syntheticMethod(Long.class.getName(), stringType, "ls", longType));
        graph.link(stringType, longType, syntheticMethod(String.class.getName(), longType, "sl", stringType));

        var path = findPath(graph, stringType, doubleType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links String to Integer to Long (forward path) and Long back to Integer (back-edge).
    // Expects the forward path String to Integer to Long, with the back-edge pruned by the visited set.
    private void testBackEdgeIgnored() {
        var graph = newGraph();
        var m1 = syntheticMethod(String.class.getName(), integerType, "si", stringType);
        var m2 = syntheticMethod(Integer.class.getName(), longType, "il", integerType);
        graph.link(stringType, integerType, m1);
        graph.link(integerType, longType, m2);
        graph.link(longType, integerType, syntheticMethod(Long.class.getName(), integerType, "li", longType));

        var path = findPath(graph, stringType, longType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(m1, path.get(0).method());
        assertSame(m2, path.get(1).method());
    }

    // Cross-direction paths (unified serializer + deserializer graph)

    // Links Long to Optional<Integer> (deserializer direction) and Optional<Integer> to Integer
    // (serializer direction). In a split-graph model this path would be impossible because the two
    // converters would live in separate graphs. The unified graph finds both steps.
    private void testCrossDirectionDeserializerThenSerializer() {
        var graph = newGraph();
        var deser = syntheticMethod(Long.class.getName(), optionalOfInteger, "deserialize", longType);
        var ser = syntheticMethod(Optional.class.getName(), integerType, "unwrap", optionalOfInteger);
        graph.link(longType, optionalOfInteger, deser);
        graph.link(optionalOfInteger, integerType, ser);

        var path = findPath(graph, longType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(deser, path.get(0).method());
        assertSame(ser, path.get(1).method());
    }

    // Links Integer to Long (serializer direction) and Long to Optional<Integer> (deserializer
    // direction). The reverse of the previous test: serializer first, then deserializer.
    private void testCrossDirectionSerializerThenDeserializer() {
        var graph = newGraph();
        var ser = syntheticMethod(Integer.class.getName(), longType, "serialize", integerType);
        var deser = syntheticMethod(Long.class.getName(), optionalOfInteger, "deserialize", longType);
        graph.link(integerType, longType, ser);
        graph.link(longType, optionalOfInteger, deser);

        var path = findPath(graph, integerType, optionalOfInteger, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(ser, path.get(0).method());
        assertSame(deser, path.get(1).method());
    }

    // Parametrized converters (generic type argument resolution)

    // Links a parametrized unwrap method (Optional<T> to T) with Optional<Integer> as input.
    // The graph should resolve T to Integer and return a single-step path.
    private void testParametrizedUnwrap() {
        var graph = newGraph();
        var method = DaedalusMethodElement.of(unwrapMethod, true, "");
        graph.link(optionalOfInteger, unwrapMethod.getReturnType(), method);

        var path = findPath(graph, optionalOfInteger, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Links a parametrized wrap method (T to Optional<T>) with Integer as input.
    // The graph should resolve T to Integer and return a single-step path to Optional<Integer>.
    private void testParametrizedWrap() {
        var graph = newGraph();
        var method = DaedalusMethodElement.of(wrapMethod, true, "");
        graph.link(integerType, wrapMethod.getReturnType(), method);

        var path = findPath(graph, integerType, optionalOfInteger, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Chains a concrete deserializer (Long to Optional<Integer>) with a parametrized unwrap
    // (Optional<T> to T). The graph should resolve the generic second step to produce Integer.
    private void testMultiHopWithParametrizedUnwrap() {
        var graph = newGraph();
        var deser = syntheticMethod(Long.class.getName(), optionalOfInteger, "deserialize", longType);
        var unwrap = DaedalusMethodElement.of(unwrapMethod, true, "");
        graph.link(longType, optionalOfInteger, deser);
        graph.link(optionalOfInteger, unwrapMethod.getReturnType(), unwrap);

        var path = findPath(graph, longType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(deser, path.get(0).method());
        assertSame(unwrap, path.get(1).method());
    }

    // Path legality

    // Links a converter whose owner is "test.Converters" and passes that type as a mixin.
    // The converter should be legal because its owner matches the mixin.
    private void testLegalThroughMixin() {
        var graph = newGraph();
        var method = syntheticMethod("test.Converters", integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of(convertersElement)));
        assertEquals(1, path.size());
    }

    // Links a converter whose owner is "some.Unknown", which does not match the source type,
    // target type, or any mixin. The path should be rejected as illegal.
    private void testIllegalWithoutMatchingOwner() {
        var graph = newGraph();
        var method = syntheticMethod("some.Unknown", integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links a converter whose owner matches the qualified name of the source type (String).
    // The isPathLegalThroughInterpretedObject check on the from type should make it legal.
    private void testLegalThroughSourceType() {
        var graph = newGraph();
        var method = syntheticMethod(String.class.getName(), integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
    }

    // Links a converter whose owner matches the qualified name of the target type (Integer).
    // The isPathLegalThroughInterpretedObject check on the to type should make it legal.
    private void testLegalThroughTargetType() {
        var graph = newGraph();
        var method = syntheticMethod(Integer.class.getName(), integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
    }

    // Registers a test extension that marks String as a managed type, then links a converter
    // whose owner is the generated Spec class name for String. The isPathLegalThroughObject
    // check should make it legal because the owner matches the Spec name of the managed type.
    private void testLegalThroughManagedType() {
        DaedalusProcessorExtension extension = new TestExtension() {
            @Override
            public boolean isManagedType(TypeMirror type) {
                return types.isSameType(type, stringType);
            }
        };
        var graph = new DaedalusConverterGraph(types, parametersManager, List.of(extension));
        var method = syntheticMethod(String.class.getName() + "Spec", integerType, "encode", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
    }

    // Multiple paths and assignability

    // Links two paths from String to Long: a direct one and an indirect one through Integer.
    // Expects findPath to return at least one valid path ending at Long.
    private void testMultiplePaths() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));
        graph.link(integerType, longType, syntheticMethod(Integer.class.getName(), longType, "toLong", integerType));
        graph.link(stringType, longType, syntheticMethod(String.class.getName(), longType, "toLong", stringType));

        var path = findPath(graph, stringType, longType, DaedalusMixins.of(Set.of()));
        assertFalse(path.isEmpty());
        assertEquals(longType.toString(), path.getLast().returnType().toString());
    }

    // Links a converter from Number to String owned by a mixin, then searches from Integer
    // (a subtype of Number) to String. The converter's declared input is Number, but Integer
    // is assignable to Number so the converter should match via subtype assignability.
    private void testSubtypeAssignabilityViaMixin() {
        var graph = newGraph();
        var method = syntheticMethod("test.Converters", stringType, "convert", numberType);
        graph.link(numberType, stringType, method);

        var path = findPath(graph, integerType, stringType, DaedalusMixins.of(Set.of(convertersElement)));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Original limitation: cross-direction path with parametrized generic unwrap

    // Reproduces the original limitation that motivated the unified graph. Links a deserializer
    // from Long to Optional<PhoneNumber> and a parametrized serializer unwrap from Optional<T>
    // to T. In a split-graph model this path is impossible because the deserializer and serializer
    // live in separate graphs. The unified graph finds the two-step path and resolves T to
    // PhoneNumber through generic type inference.
    private void testOriginalLimitationCrossDirectionWithParametrizedUnwrap() {
        var graph = newGraph();
        var deser = syntheticMethod(Long.class.getName(), optionalOfPhoneNumber, "deserialize", longType);
        var unwrap = DaedalusMethodElement.of(unwrapMethod, true, "");
        graph.link(longType, optionalOfPhoneNumber, deser);
        graph.link(optionalOfPhoneNumber, unwrapMethod.getReturnType(), unwrap);

        var path = findPath(graph, longType, phoneNumberType, DaedalusMixins.of(Set.of()));
        assertEquals(2, path.size());
        assertSame(deser, path.get(0).method());
        assertSame(unwrap, path.get(1).method());
    }

    // Path selection

    // Links a two-step path from String to Long before a direct converter from String to Long.
    // The direct converter is linked last, but it forms the shortest path, so it must be returned.
    private void testShortestPathPreferred() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));
        graph.link(integerType, longType, syntheticMethod(Integer.class.getName(), longType, "toLong", integerType));
        var direct = syntheticMethod(String.class.getName(), longType, "toLong", stringType);
        graph.link(stringType, longType, direct);

        var path = findPath(graph, stringType, longType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(direct, path.getFirst().method());
    }

    // Links a round trip from Long to a wrapper type and back, like the OptionalLong overloads of
    // OptionalMixin, before a direct converter from Long to String, all owned by a mixin. Starting from
    // the primitive long, the round trip reaches java.lang.Long, which is a different search state than
    // long, so a search that returns any path could take it. The shortest path skips it.
    private void testRoundTripDetourAvoided() {
        var graph = newGraph();
        graph.link(longType, doubleType, syntheticMethod("test.Converters", doubleType, "wrap", longType));
        graph.link(doubleType, longType, syntheticMethod("test.Converters", longType, "unwrap", doubleType));
        var direct = syntheticMethod("test.Converters", stringType, "toString", longType);
        graph.link(longType, stringType, direct);

        var path = findPath(graph, primitiveLongType, stringType, DaedalusMixins.of(Set.of(convertersElement)));
        assertEquals(1, path.size());
        assertSame(direct, path.getFirst().method());
    }

    // Links a direct converter from Integer to String owned by a scoped mixin, and a two-step path
    // owned by an explicit mixin. The explicit tier has a path, so the scoped mixin is never
    // considered, even though its path is shorter.
    private void testExplicitTierPrecedence() {
        var graph = newGraph();
        graph.link(integerType, stringType, syntheticMethod("test.ScopedConverters", stringType, "convert", integerType));
        var first = syntheticMethod("test.Converters", doubleType, "toDouble", integerType);
        var second = syntheticMethod("test.Converters", stringType, "toString", doubleType);
        graph.link(integerType, doubleType, first);
        graph.link(doubleType, stringType, second);

        var tiers = DaedalusMixins.builder().add(convertersElement, TypeMixin.Scope.MANUAL).add(scopedConvertersElement, TypeMixin.Scope.PACKAGE).build();
        var path = findPath(graph, integerType, stringType, tiers);
        assertEquals(2, path.size());
        assertSame(first, path.get(0).method());
        assertSame(second, path.get(1).method());
    }

    // Links a converter from Integer to String owned by a scoped mixin only. The explicit tier has no
    // path, so the scoped tier is searched and its converter is returned.
    private void testScopedTierFallback() {
        var graph = newGraph();
        var scoped = syntheticMethod("test.ScopedConverters", stringType, "convert", integerType);
        graph.link(integerType, stringType, scoped);

        var tiers = DaedalusMixins.builder().add(convertersElement, TypeMixin.Scope.MANUAL).add(scopedConvertersElement, TypeMixin.Scope.PACKAGE).build();
        var path = findPath(graph, integerType, stringType, tiers);
        assertEquals(1, path.size());
        assertSame(scoped, path.getFirst().method());

        assertTrue(findPath(graph, integerType, stringType, DaedalusMixins.of(Set.of(convertersElement))).isEmpty());
    }

    // Links a converter from String to Integer and searches for Number. The converter returns a
    // subtype of the target, so its result can be used where a Number is expected.
    private void testSubtypeReturnReachesTarget() {
        var graph = newGraph();
        var method = syntheticMethod(String.class.getName(), integerType, "toInteger", stringType);
        graph.link(stringType, integerType, method);

        var path = findPath(graph, stringType, numberType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Links a converter from String to Number and searches for Integer. The converter returns a
    // supertype of the target, so its result cannot be used where an Integer is expected.
    private void testSupertypeReturnRejected() {
        var graph = newGraph();
        graph.link(stringType, numberType, syntheticMethod(String.class.getName(), numberType, "toNumber", stringType));

        var path = findPath(graph, stringType, integerType, DaedalusMixins.of(Set.of()));
        assertTrue(path.isEmpty());
    }

    // Links two converters from Integer to String. Both form a path of the same length, so the one
    // linked first must always be returned.
    private void testDeterministicTie() {
        var graph = newGraph();
        var first = syntheticMethod(Integer.class.getName(), stringType, "first", integerType);
        var second = syntheticMethod(Integer.class.getName(), stringType, "second", integerType);
        graph.link(integerType, stringType, first);
        graph.link(integerType, stringType, second);

        for (var attempt = 0; attempt < 32; attempt++) {
            var path = findPath(graph, integerType, stringType, DaedalusMixins.of(Set.of()));
            assertEquals(1, path.size());
            assertSame(first, path.getFirst().method());
        }
    }

    // Additional parameters

    // Links Long to Instant through TimeConverters#ofEpoch, which requires @TimeConvertersParameters, and a longer
    // path through String. Without the annotation, the longer path must be used.
    private void testUnsuppliedArgumentsFallback() {
        var graph = newGraph();
        var ofEpoch = concreteMethod(ofEpochMethod);
        var toText = syntheticMethod(Long.class.getName(), stringType, "toText", longType);
        var parse = concreteMethod(parseMethod);
        graph.link(longType, instantType, ofEpoch);
        graph.link(longType, stringType, toText);
        graph.link(stringType, instantType, parse);

        var path = findPath(graph, longType, instantType, DaedalusMixins.of(Set.of(timeConvertersElement, fallbackConvertersElement)));
        assertEquals(2, path.size());
        assertSame(toText, path.get(0).method());
        assertSame(parse, path.get(1).method());
    }

    // Same graph as above, but the use site supplies @TimeConvertersParameters, so the direct converter is used.
    private void testSuppliedArguments() {
        var graph = newGraph();
        var ofEpoch = concreteMethod(ofEpochMethod);
        graph.link(longType, instantType, ofEpoch);
        graph.link(longType, stringType, syntheticMethod(Long.class.getName(), stringType, "toText", longType));
        graph.link(stringType, instantType, concreteMethod(parseMethod));

        var annotation = parametersManager.getAnnotation(ofEpochMethod);
        var result = graph.findPath(longType, instantType, DaedalusMixins.of(Set.of(timeConvertersElement, fallbackConvertersElement)), Set.of(annotation));
        var found = assertInstanceOf(DaedalusConverterPath.Found.class, result);
        assertEquals(1, found.arcs().size());
        assertSame(ofEpoch, found.arcs().getFirst().method());
        assertEquals(List.of(annotation), found.arcs().getFirst().parametersAnnotations());
    }

    // Links a plain converter from Long to Instant before TimeConverters#ofEpoch, like the synthetic decode(int) of an
    // enum spec is linked before its decode(Integer, defaultValue). Both paths have the same length, so the plain one
    // wins by link order unless the use site supplies @TimeConvertersParameters, which must then select ofEpoch.
    private void testSuppliedArgumentsPreferred() {
        var graph = newGraph();
        var plain = syntheticMethod(Long.class.getName(), instantType, "toInstant", longType);
        var ofEpoch = concreteMethod(ofEpochMethod);
        graph.link(longType, instantType, plain);
        graph.link(longType, instantType, ofEpoch);

        var tiers = DaedalusMixins.of(Set.of(timeConvertersElement));
        var unsupplied = assertInstanceOf(DaedalusConverterPath.Found.class, graph.findPath(longType, instantType, tiers, Set.of()));
        assertSame(plain, unsupplied.arcs().getFirst().method());

        var supplied = graph.findPath(longType, instantType, tiers, Set.of(parametersManager.getAnnotation(ofEpochMethod)));
        var found = assertInstanceOf(DaedalusConverterPath.Found.class, supplied);
        assertSame(ofEpoch, found.arcs().getFirst().method());
    }

    // Links a converter from Number to String before one from Integer to String. Both paths from Integer have the same
    // length, but the converter declared for Integer must win, as it is the most specific one.
    private void testExactConverterPreferred() {
        var graph = newGraph();
        var fromNumber = syntheticMethod(String.class.getName(), stringType, "fromNumber", numberType);
        var fromInteger = syntheticMethod(String.class.getName(), stringType, "fromInteger", integerType);
        graph.link(numberType, stringType, fromNumber);
        graph.link(integerType, stringType, fromInteger);

        var path = findPath(graph, integerType, stringType, DaedalusMixins.of(Set.of()));
        assertEquals(1, path.size());
        assertSame(fromInteger, path.getFirst().method());

        var doublePath = findPath(graph, doubleType, stringType, DaedalusMixins.of(Set.of()));
        assertEquals(1, doublePath.size());
        assertSame(fromNumber, doublePath.getFirst().method());
    }

    // Only the converter that requires @TimeConvertersParameters exists: the search must report it as a candidate.
    private void testMissingArguments() {
        var graph = newGraph();
        var ofEpoch = concreteMethod(ofEpochMethod);
        graph.link(longType, instantType, ofEpoch);

        var result = graph.findPath(longType, instantType, DaedalusMixins.of(Set.of(timeConvertersElement)), Set.of());
        var missing = assertInstanceOf(DaedalusConverterPath.MissingArguments.class, result);
        assertEquals(1, missing.candidates().size());
        var arc = missing.candidates().getFirst().getFirst();
        assertSame(ofEpoch, arc.method());
        assertEquals(1, arc.parametersAnnotations().size());
        var annotation = arc.parametersAnnotations().getFirst();
        assertEquals("test.TimeConvertersParameters", annotation.qualifiedName());
        assertTrue(annotation.element().isPresent(), "the annotation is declared in the source, so it must be concrete");
    }

    // No converter leaves String, so the search must report that no path exists at all.
    private void testNotFoundIgnoringArguments() {
        var graph = newGraph();
        graph.link(longType, instantType, concreteMethod(ofEpochMethod));

        var result = graph.findPath(stringType, instantType, DaedalusMixins.of(Set.of(timeConvertersElement)), Set.of());
        assertInstanceOf(DaedalusConverterPath.NotFound.class, result);
    }

    // The serializer declares no additional parameter, but its sizer does: the serializer requires the sizer's annotation.
    private void testSizerArguments() {
        var graph = newGraph();
        var serializer = concreteMethod(toEpochSecondsMethod);
        var sizer = concreteMethod(sizeOfMethod);
        graph.linkSerializer(instantType, longType, serializer);
        graph.attachSizer(instantType, sizer);

        var tiers = DaedalusMixins.of(Set.of(timeConvertersElement));
        assertInstanceOf(DaedalusConverterPath.MissingArguments.class, graph.findPath(instantType, longType, tiers, Set.of()));

        var result = graph.findPath(instantType, longType, tiers, Set.of(parametersManager.getAnnotation(sizeOfMethod)));
        var found = assertInstanceOf(DaedalusConverterPath.Found.class, result);
        assertSame(serializer, found.arcs().getFirst().method());
        assertSame(sizer, found.arcs().getFirst().sizer());
    }

    // A concrete annotation and a synthetic one with the same qualified name must be interchangeable.
    private void testParametersElementEquality() {
        var concrete = DaedalusConverterParametersElement.of(timeConvertersParametersElement());
        var synthetic = DaedalusConverterParametersElement.of("test.TimeConvertersParameters");
        assertEquals(concrete, synthetic);
        assertEquals(synthetic, concrete);
        assertEquals(concrete.hashCode(), synthetic.hashCode());
        assertNotEquals(synthetic, DaedalusConverterParametersElement.of("test.OtherParameters"));
        assertTrue(Set.of(concrete).contains(synthetic));
    }

    // Reads the arguments of TimeConverters#ofEpoch and TimeConverters#write from the annotation of UseSites#timestamp.
    private void testArgumentsFromUseSite() {
        var timestamp = findField(useSitesElement, "timestamp");
        assertTrue(parametersManager.getSuppliedAnnotations(timestamp).contains(parametersManager.getAnnotation(ofEpochMethod)));
        assertEquals(
                List.of(new DaedalusConverterArgument.Value("unit", "java.util.concurrent.TimeUnit.SECONDS")),
                parametersManager.getArguments(concreteMethod(ofEpochMethod), timestamp)
        );

        var writeArguments = parametersManager.getArguments(concreteMethod(writeMethod), timestamp);
        assertEquals(2, writeArguments.size());
        var injected = assertInstanceOf(DaedalusConverterArgument.Injected.class, writeArguments.get(0));
        assertEquals("writer", injected.name());
        assertEquals(new DaedalusConverterArgument.Value("unit", "java.util.concurrent.TimeUnit.SECONDS"), writeArguments.get(1));

        assertEquals(List.of(), parametersManager.getArguments(concreteMethod(toEpochSecondsMethod), timestamp));

        var plain = findField(useSitesElement, "plain");
        assertFalse(parametersManager.getSuppliedAnnotations(plain).contains(parametersManager.getAnnotation(ofEpochMethod)));
        assertThrows(IllegalStateException.class, () -> parametersManager.getArguments(concreteMethod(ofEpochMethod), plain));
    }

    // Renders a long, a char, a String, a Class, an int array, a float and a boolean supplied by UseSites#wide.
    private void testArgumentExpressions() {
        var arguments = parametersManager.getArguments(concreteMethod(wideConvertMethod), findField(useSitesElement, "wide"));
        var expressions = arguments.stream()
                .map(argument -> ((DaedalusConverterArgument.Value) argument).expression())
                .toList();
        assertEquals(List.of("-5L", "'\\u000a'", "\"a\\\"b\"", "java.lang.String[].class", "new int[]{1, 2}", "1.5F", "true"), expressions);
    }

    // Renders static invocations with value and injected arguments.
    private void testInvocation() {
        var unit = new DaedalusConverterArgument.Value("unit", "java.util.concurrent.TimeUnit.SECONDS");
        assertEquals(
                "test.TimeConverters.ofEpoch(value, java.util.concurrent.TimeUnit.SECONDS)",
                concreteMethod(ofEpochMethod).invoke("value", List.of(unit), _ -> "stream")
        );
        assertEquals(
                "test.TimeConverters.write(value, stream, java.util.concurrent.TimeUnit.SECONDS)",
                concreteMethod(writeMethod).invoke("value", List.of(new DaedalusConverterArgument.Injected("writer", stringType), unit), _ -> "stream")
        );
        assertEquals(
                "java.lang.Long.toText(value)",
                syntheticMethod(Long.class.getName(), stringType, "toText", longType).invoke("value", List.of(), _ -> "stream")
        );
    }

    // Unmanaged enums

    // An enum that no extension manages is serialized with name() and deserialized with a switch over its constants,
    // so no converter needs to be declared for it, and Enum.valueOf, which is reflective, is never used.
    private void testUnmanagedEnum() {
        var graph = newGraph();

        var serializer = findPath(graph, colorType, stringType, DaedalusMixins.of(Set.of()));
        assertEquals(1, serializer.size());
        assertEquals("value.name()", serializer.getFirst().method().invoke("value", List.of(), _ -> "stream"));

        var deserializer = findPath(graph, stringType, colorType, DaedalusMixins.of(Set.of()));
        assertEquals(1, deserializer.size());
        var method = assertInstanceOf(DaedalusMethodElement.Inline.class, deserializer.getFirst().method());
        assertEquals(
                "switch (value) { case null -> null; case \"RED\" -> test.Color.RED; case \"GREEN\" -> test.Color.GREEN; "
                        + "case java.lang.String daedalusUnknownConstant -> throw com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException.malformedValue(test.Color.class, daedalusUnknownConstant, null); }",
                method.invoke("value", List.of(), _ -> "stream")
        );
        assertEquals(1, method.thrownTypes().size());
    }

    // The name converters of an unmanaged enum are a fallback: a scoped mixin that converts the enum, which is only in the
    // second tier, must win over them, although they would be legal in the first tier as they belong to the enum itself.
    private void testUnmanagedEnumFallback() {
        var graph = newGraph();
        var mixinConverter = syntheticMethod("test.TimeConverters", stringType, "colorText", colorType);
        graph.link(colorType, stringType, mixinConverter);

        var path = findPath(graph, colorType, stringType, DaedalusMixins.builder().add(timeConvertersElement, TypeMixin.Scope.PACKAGE).build());
        assertEquals(1, path.size());
        assertSame(mixinConverter, path.getFirst().method());
    }

    // An enum managed by an extension has converters of its own, so the graph must not link the name converters.
    private void testManagedEnum() {
        DaedalusProcessorExtension extension = new TestExtension() {
            @Override
            public boolean isManagedType(TypeMirror type) {
                return types.isSameType(type, colorType);
            }
        };
        var graph = new DaedalusConverterGraph(types, parametersManager, List.of(extension));
        assertInstanceOf(DaedalusConverterPath.NotFound.class, graph.findPath(stringType, colorType, DaedalusMixins.of(Set.of()), Set.of()));
        assertInstanceOf(DaedalusConverterPath.NotFound.class, graph.findPath(colorType, stringType, DaedalusMixins.of(Set.of()), Set.of()));
    }

    // Utilities

    // Runs the test while javac is still processing, as the tests that query the element utilities fail once the
    // compilation task has completed, and replays its outcome when the dynamic test is executed.
    private static DynamicTest eagerTest(String name, Executable body) {
        Throwable failure;
        try {
            body.execute();
            failure = null;
        } catch (Throwable throwable) {
            failure = throwable;
        }
        var result = failure;
        return dynamicTest(name, () -> {
            if (result != null) {
                throw result;
            }
        });
    }

    private static String typeSystemLocation() {
        try {
            return Path.of(TypeDeserializationException.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<DaedalusConverterArc> findPath(DaedalusConverterGraph graph, TypeMirror from, TypeMirror to, DaedalusMixins mixins) {
        return switch (graph.findPath(from, to, mixins, Set.of())) {
            case DaedalusConverterPath.Found(var arcs) -> arcs;
            case DaedalusConverterPath.MissingArguments _, DaedalusConverterPath.NotFound _ -> List.of();
        };
    }

    private static DaedalusMethodElement concreteMethod(ExecutableElement element) {
        return DaedalusMethodElement.of(element, false, "");
    }

    private static TypeElement timeConvertersParametersElement() {
        return (TypeElement) timeConvertersElement.getEnclosingElement()
                .getEnclosedElements()
                .stream()
                .filter(element -> element.getSimpleName().contentEquals("TimeConvertersParameters"))
                .findFirst()
                .orElseThrow();
    }

    private static VariableElement findField(TypeElement type, String name) {
        return ElementFilter.fieldsIn(type.getEnclosedElements())
                .stream()
                .filter(field -> field.getSimpleName().contentEquals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
    }

    private static DaedalusConverterGraph newGraph() {
        return new DaedalusConverterGraph(types, parametersManager, List.of());
    }

    private static DaedalusMethodElement syntheticMethod(String owner, TypeMirror returnType, String name, TypeMirror... params) {
        return DaedalusMethodElement.of(owner, Set.of(Modifier.PUBLIC, Modifier.STATIC), returnType, name, params);
    }

    private static ExecutableElement findMethod(TypeElement type, String name) {
        return type.getEnclosedElements()
                .stream()
                .filter(e -> e instanceof ExecutableElement && e.getSimpleName().contentEquals(name))
                .map(e -> (ExecutableElement) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method not found: " + name));
    }

    private static abstract class TestExtension implements DaedalusProcessorExtension {
        @Override
        public String name() {
            return "test";
        }

        @Override
        public Set<String> supportedAnnotationTypes() {
            return Set.of();
        }

        @Override
        public void init(ProcessingEnvironment processingEnv, DaedalusTypeManager types, DaedalusLogManager messages, DaedalusMixinScopeManager mixinScopeResolver) {

        }

        @Override
        public void runChecks(RoundEnvironment roundEnv) {

        }

        @Override
        public SequencedCollection<? extends DaedalusTypeElement> processObjects(RoundEnvironment roundEnv, DaedalusConverterGraph converterGraph) {
            return List.of();
        }

        @Override
        public void generateCode(SequencedCollection<? extends DaedalusTypeElement> objects) {

        }
    }
}

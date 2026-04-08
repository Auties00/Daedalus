package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusMixinScopeManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
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
    private static ExecutableElement unwrapMethod;
    private static ExecutableElement wrapMethod;

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
                List.of("--enable-preview", "--source", "26", "-proc:only"),
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
                unwrapMethod = findMethod(convertersElement, "unwrap");
                wrapMethod = findMethod(convertersElement, "wrap");

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

        var path = graph.findPath(stringType, integerType, Set.of());
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

        var path = graph.findPath(stringType, longType, Set.of());
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

        var path = graph.findPath(stringType, doubleType, Set.of());
        assertEquals(3, path.size());
        assertSame(m1, path.get(0).method());
        assertSame(m2, path.get(1).method());
        assertSame(m3, path.get(2).method());
    }

    // Links String to Integer only, then searches for String to Double which has no reachable path
    private void testNoPath() {
        var graph = newGraph();
        graph.link(stringType, integerType, syntheticMethod(String.class.getName(), integerType, "toInteger", stringType));

        var path = graph.findPath(stringType, doubleType, Set.of());
        assertTrue(path.isEmpty());
    }

    // Searches an empty graph, which has no converters at all
    private void testEmptyGraph() {
        var graph = newGraph();
        var path = graph.findPath(stringType, integerType, Set.of());
        assertTrue(path.isEmpty());
    }

    // Searches for a conversion from String to String with no converters linked
    private void testSameSourceAndTarget() {
        var graph = newGraph();
        var path = graph.findPath(stringType, stringType, Set.of());
        assertTrue(path.isEmpty());
    }

    // Links Integer to Number and expects it to match, since Integer is directly assignable to Number
    private void testSupertypeInput() {
        var graph = newGraph();
        var method = syntheticMethod(Integer.class.getName(), numberType, "toNumber", integerType);
        graph.link(integerType, numberType, method);

        var path = graph.findPath(integerType, numberType, Set.of());
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

        var path = graph.findPath(stringType, doubleType, Set.of());
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

        var path = graph.findPath(stringType, longType, Set.of());
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

        var path = graph.findPath(stringType, doubleType, Set.of());
        assertTrue(path.isEmpty());
    }

    // Links String to itself (identity converter), then searches for String to Integer.
    // The self-loop should not cause infinite recursion.
    private void testSelfLoopConverter() {
        var graph = newGraph();
        graph.link(stringType, stringType, syntheticMethod(String.class.getName(), stringType, "identity", stringType));

        var path = graph.findPath(stringType, integerType, Set.of());
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

        var path = graph.findPath(stringType, doubleType, Set.of());
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

        var path = graph.findPath(stringType, longType, Set.of());
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

        var path = graph.findPath(longType, integerType, Set.of());
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

        var path = graph.findPath(integerType, optionalOfInteger, Set.of());
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

        var path = graph.findPath(optionalOfInteger, integerType, Set.of());
        assertEquals(1, path.size());
        assertSame(method, path.getFirst().method());
    }

    // Links a parametrized wrap method (T to Optional<T>) with Integer as input.
    // The graph should resolve T to Integer and return a single-step path to Optional<Integer>.
    private void testParametrizedWrap() {
        var graph = newGraph();
        var method = DaedalusMethodElement.of(wrapMethod, true, "");
        graph.link(integerType, wrapMethod.getReturnType(), method);

        var path = graph.findPath(integerType, optionalOfInteger, Set.of());
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

        var path = graph.findPath(longType, integerType, Set.of());
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

        var path = graph.findPath(stringType, integerType, Set.of(convertersElement));
        assertEquals(1, path.size());
    }

    // Links a converter whose owner is "some.Unknown", which does not match the source type,
    // target type, or any mixin. The path should be rejected as illegal.
    private void testIllegalWithoutMatchingOwner() {
        var graph = newGraph();
        var method = syntheticMethod("some.Unknown", integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = graph.findPath(stringType, integerType, Set.of());
        assertTrue(path.isEmpty());
    }

    // Links a converter whose owner matches the qualified name of the source type (String).
    // The isPathLegalThroughInterpretedObject check on the from type should make it legal.
    private void testLegalThroughSourceType() {
        var graph = newGraph();
        var method = syntheticMethod(String.class.getName(), integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = graph.findPath(stringType, integerType, Set.of());
        assertEquals(1, path.size());
    }

    // Links a converter whose owner matches the qualified name of the target type (Integer).
    // The isPathLegalThroughInterpretedObject check on the to type should make it legal.
    private void testLegalThroughTargetType() {
        var graph = newGraph();
        var method = syntheticMethod(Integer.class.getName(), integerType, "convert", stringType);
        graph.link(stringType, integerType, method);

        var path = graph.findPath(stringType, integerType, Set.of());
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
        var graph = new DaedalusConverterGraph(types, List.of(extension));
        var method = syntheticMethod(String.class.getName() + "Spec", integerType, "encode", stringType);
        graph.link(stringType, integerType, method);

        var path = graph.findPath(stringType, integerType, Set.of());
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

        var path = graph.findPath(stringType, longType, Set.of());
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

        var path = graph.findPath(integerType, stringType, Set.of(convertersElement));
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

        var path = graph.findPath(longType, phoneNumberType, Set.of());
        assertEquals(2, path.size());
        assertSame(deser, path.get(0).method());
        assertSame(unwrap, path.get(1).method());
    }

    // Utilities

    private static DaedalusConverterGraph newGraph() {
        return new DaedalusConverterGraph(types, List.of());
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

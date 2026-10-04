package com.github.auties00.daedalus.protobuf.processor.manager;

import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldType;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.*;

/**
 * A resolver of the checked exceptions that the methods of the generated Spec classes must
 * declare in their {@code throws} clause.
 *
 * <p>Besides the {@link ProtobufSerializationException} and
 * {@link ProtobufDeserializationException} thrown by the binary writers and readers, a generated
 * method must declare every checked exception thrown by the code it invokes: the mixin converters
 * applied to each field, the constructor or factory method that builds a deserialized message, and
 * the methods of the Spec classes of nested messages and enums, which in turn declare the checked
 * exceptions of their own converters. As a consequence, the exceptions of a Spec class depend on
 * the exceptions of the Spec classes it references, and messages may reference each other
 * recursively.
 *
 * <p>This manager computes, for every protobuf type processed in the current round, three sets of
 * checked exceptions: the <em>outgoing</em> set, declared by the serialization and size calculation
 * methods, the <em>incoming</em> set, declared by the deserialization methods, and the
 * <em>building</em> set, declared by the terminal methods of the builder of a message, which build
 * the builders of its nested messages. The sets are computed as the least fixed point of the
 * references between the types, so recursive messages are supported. The base exceptions of each direction, {@link ProtobufSerializationException}
 * and {@link ProtobufDeserializationException}, are not part of the sets: they are added only to
 * the methods that read or write the wire format.
 *
 * <p>The Spec class of a message extends {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec},
 * and the Spec class of an enum extends {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec},
 * whose methods declare only {@link ProtobufSerializationException} or
 * {@link ProtobufDeserializationException}: the checked exceptions of the converters of a type are
 * wrapped in them, as described by {@link #wrapsOutgoing(ProtobufTypeElement)} and
 * {@link #wrapsIncoming(ProtobufTypeElement)}, so they do not propagate to the Spec classes of the
 * messages that reference it.
 *
 * <p>Converters that reference the Spec class of a type that is not processed in the current round,
 * for example because it was generated in a previous round or compiled in a dependency, are
 * resolved by reading the {@code throws} clause of the compiled Spec class.
 *
 * <p>This class is not thread safe.
 */
public final class ProtobufExceptionManager {
    /**
     * The base exception thrown by the methods that write the wire format.
     */
    private static final TypeName SERIALIZATION_EXCEPTION = ClassName.get(ProtobufSerializationException.class);

    /**
     * The base exception thrown by the methods that read the wire format.
     */
    private static final TypeName DESERIALIZATION_EXCEPTION = ClassName.get(ProtobufDeserializationException.class);

    /**
     * The utility used to look up the compiled Spec classes of types not processed in the current
     * round.
     */
    private final Elements elements;

    /**
     * The types processed in the current round, indexed by the qualified name of their Spec class.
     */
    private final Map<String, ProtobufTypeElement> types;

    /**
     * The outgoing checked exceptions of each type, indexed by the qualified name of its Spec class.
     */
    private final Map<String, Set<TypeName>> outgoing;

    /**
     * The incoming checked exceptions of each type, indexed by the qualified name of its Spec class.
     */
    private final Map<String, Set<TypeName>> incoming;

    /**
     * The checked exceptions thrown when building each message from its builder, indexed by the
     * qualified name of its Spec class.
     */
    private final Map<String, Set<TypeName>> building;

    /**
     * Constructs a new manager and computes the checked exceptions of the given types.
     *
     * @param elements the utility used to look up compiled Spec classes
     * @param objects the types processed in the current round; types that are not protobuf types
     *                are ignored
     */
    public ProtobufExceptionManager(Elements elements, Collection<? extends DaedalusTypeElement> objects) {
        this.elements = elements;
        this.types = new LinkedHashMap<>();
        this.outgoing = new HashMap<>();
        this.incoming = new HashMap<>();
        this.building = new HashMap<>();
        for (var object : objects) {
            if (object instanceof ProtobufTypeElement type) {
                var specName = specName(type);
                types.put(specName, type);
                outgoing.put(specName, new LinkedHashSet<>());
                incoming.put(specName, new LinkedHashSet<>());
                building.put(specName, new LinkedHashSet<>());
                if (type instanceof ProtobufEnumElement enumElement) {
                    // An enum only converts a constant to and from its index, through its converters if it declares them
                    enumElement.serializer().ifPresent(serializer -> outgoing.get(specName).addAll(thrownTypes(serializer)));
                    enumElement.deserializer().ifPresent(deserializer -> incoming.get(specName).addAll(thrownTypes(deserializer)));
                }
            }
        }
        computeFixedPoint();
    }

    /**
     * Returns the checked exceptions declared by the serialization methods of the Spec class of the
     * given type.
     *
     * <p>The serialization method of a message writes the wire format, so it declares
     * {@link ProtobufSerializationException}, which also wraps the checked exceptions of its
     * converters; the one of an enum only maps a constant to its index, so it declares
     * {@link ProtobufSerializationException} only if it wraps the checked exceptions of its
     * converters.
     *
     * @param type the type whose Spec class is generated
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> serializationExceptions(ProtobufTypeElement type) {
        if (type instanceof ProtobufMessageElement || wrapsOutgoing(type)) {
            return List.of(SERIALIZATION_EXCEPTION);
        }
        return List.of();
    }

    /**
     * Returns whether the methods of the Spec class of the given type that serialize, measure
     * or count the size table slots of its values must wrap the checked exceptions of the
     * converters they invoke in a {@link ProtobufSerializationException}.
     *
     * @param type the type whose Spec class is generated
     * @return {@code true} if the converters of the type throw checked exceptions
     */
    public boolean wrapsOutgoing(ProtobufTypeElement type) {
        return !outgoing.get(specName(type)).isEmpty();
    }

    /**
     * Returns whether the methods of the Spec class of the given type that deserialize its
     * values must wrap the checked exceptions of the converters and of the builder they invoke
     * in a {@link ProtobufDeserializationException}.
     *
     * @param type the type whose Spec class is generated
     * @return {@code true} if the converters of the type, or the builder of a message, throw
     *         checked exceptions
     */
    public boolean wrapsIncoming(ProtobufTypeElement type) {
        return !incoming.get(specName(type)).isEmpty();
    }

    /**
     * Returns the checked exceptions declared by the size calculation method of the Spec class of
     * the given type.
     *
     * <p>The size calculation method of a message declares {@link ProtobufSerializationException},
     * which wraps the checked exceptions of its converters, and is declared by the size
     * calculation methods of the Spec classes of its nested messages; the one of an enum
     * declares {@link ProtobufSerializationException} only if it wraps the checked exceptions
     * of its converters.
     *
     * @param type the type whose Spec class is generated
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> sizeExceptions(ProtobufTypeElement type) {
        return serializationExceptions(type);
    }

    /**
     * Returns the checked exceptions declared by the deserialization methods of the Spec class of
     * the given type.
     *
     * <p>The deserialization method of a message reads the wire format, so it declares
     * {@link ProtobufDeserializationException}, which also wraps the checked exceptions of its
     * converters and of its builder; the one of an enum only maps an index to a constant, so
     * it declares {@link ProtobufDeserializationException} only if it wraps the checked
     * exceptions of its converters.
     *
     * @param type the type whose Spec class is generated
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> deserializationExceptions(ProtobufTypeElement type) {
        if (type instanceof ProtobufMessageElement || wrapsIncoming(type)) {
            return List.of(DESERIALIZATION_EXCEPTION);
        }
        return List.of();
    }

    /**
     * Returns the checked exceptions declared by the terminal methods of the builder of the given
     * message.
     *
     * <p>Building a message invokes its constructor or factory method, the deserializer of its
     * unknown fields, and, for every field that holds the builder of a nested message, the
     * terminal method of that builder followed by the deserializers that convert the nested
     * message to the type of the field.
     *
     * @param message the message whose builder is generated
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> buildExceptions(ProtobufMessageElement message) {
        return List.copyOf(building.get(specName(message)));
    }

    /**
     * Returns the checked exceptions thrown by an invocation of the given deserializer.
     *
     * @param deserializer the deserializer, which may be a mixin method or the deserialization
     *                     method of a Spec class
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> deserializerExceptions(DaedalusMethodElement deserializer) {
        if (deserializer instanceof DaedalusMethodElement.Inline inline) {
            return thrownTypes(inline);
        }

        var element = deserializer.element();
        if (element.isPresent()) {
            return thrownTypes(element.get());
        }

        var type = types.get(deserializer.ownerName());
        if (type != null) {
            return deserializationExceptions(type);
        }

        return compiledThrownTypes(deserializer, false);
    }

    /**
     * Returns the checked exceptions thrown by the constructor or factory method that builds an
     * instance of the given message.
     *
     * <p>When the message has no factory method, the generated code invokes one of its
     * constructors, so the exceptions of every constructor are returned.
     *
     * @param message the message
     * @return the checked exceptions, in a deterministic order
     */
    public List<TypeName> constructionExceptions(ProtobufMessageElement message) {
        var factory = message.constructor();
        if (factory.isPresent()) {
            return factory.get()
                    .element()
                    .map(ProtobufExceptionManager::thrownTypes)
                    .orElse(List.of());
        }

        var result = new LinkedHashSet<TypeName>();
        for (var constructor : ElementFilter.constructorsIn(message.typeElement().getEnclosedElements())) {
            result.addAll(thrownTypes(constructor));
        }
        return List.copyOf(result);
    }

    /**
     * Computes the outgoing and incoming exceptions of every type by propagating the exceptions
     * of referenced Spec classes until no set changes.
     */
    private void computeFixedPoint() {
        var changed = true;
        while (changed) {
            changed = false;
            for (var entry : types.entrySet()) {
                if (!(entry.getValue() instanceof ProtobufMessageElement message)) {
                    continue; // Enums don't have converters
                }

                var messageOutgoing = new LinkedHashSet<TypeName>();
                var messageBuilding = new LinkedHashSet<TypeName>(constructionExceptions(message));
                message.unknownFieldsElement()
                        .flatMap(unknownFields -> unknownFields.deserializerMethod())
                        .ifPresent(deserializer -> messageBuilding.addAll(thrownTypes(deserializer)));
                for (var field : getFieldsAndCases(message)) {
                    var nestedMessage = field.type().builderMessage();
                    if (nestedMessage.isPresent()) {
                        var deserializers = field.type().deserializers();
                        for (var i = 1; i < deserializers.size(); i++) {
                            messageBuilding.addAll(converterExceptions(deserializers.get(i).delegate(), false));
                        }
                        messageBuilding.addAll(nestedBuildExceptions(nestedMessage.get()));
                    }
                }

                // Decoding a message builds it, and merging a field read in bulk serializes its previous value
                var messageIncoming = new LinkedHashSet<TypeName>(messageBuilding);
                for (var field : message.fields()) {
                    if (isConvertedBulk(field.type())) {
                        for (var serializer : field.type().serializers()) {
                            messageIncoming.addAll(converterExceptions(serializer.delegate(), true));
                        }
                    }
                }
                for (var converter : converters(message)) {
                    switch (converter) {
                        case DaedalusConverterElement.Attributed.Serializer serializer -> {
                            messageOutgoing.addAll(converterExceptions(serializer.delegate(), true));
                            if (serializer.sizer() != null) {
                                messageOutgoing.addAll(converterExceptions(serializer.sizer(), true));
                            }
                        }
                        case DaedalusConverterElement.Attributed.Deserializer deserializer ->
                                messageIncoming.addAll(converterExceptions(deserializer.delegate(), false));
                    }
                }
                messageOutgoing.remove(SERIALIZATION_EXCEPTION);
                messageIncoming.remove(SERIALIZATION_EXCEPTION);
                messageIncoming.remove(DESERIALIZATION_EXCEPTION);
                messageBuilding.remove(DESERIALIZATION_EXCEPTION);

                changed |= outgoing.get(entry.getKey()).addAll(messageOutgoing);
                changed |= incoming.get(entry.getKey()).addAll(messageIncoming);
                changed |= building.get(entry.getKey()).addAll(messageBuilding);
            }
        }
    }

    /**
     * Returns the checked exceptions thrown by building the given nested message from its builder,
     * as currently known by the fixed point computation.
     *
     * <p>The builder of a message that is not processed in the current round is compiled, so the
     * {@code throws} clause of its {@code build} method is read; if it cannot be found, no
     * exception is assumed.
     *
     * @param message the type of the nested message
     * @return the checked exceptions
     */
    private List<TypeName> nestedBuildExceptions(DeclaredType message) {
        var specName = DaedalusMethodGenerator.getSpecByType(message);
        var known = building.get(specName);
        if (known != null) {
            return List.copyOf(known);
        }

        var builder = elements.getTypeElement(DaedalusMethodGenerator.getBuilderByType(message));
        if (builder == null) {
            return List.of();
        }

        var result = new LinkedHashSet<TypeName>();
        for (var method : ElementFilter.methodsIn(builder.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals("build") && method.getParameters().isEmpty()) {
                result.addAll(thrownTypes(method));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the fields of the given message, replacing each oneof with its cases, which
     * hold the builders of their messages like fields.
     *
     * @param message the message
     * @return the fields and the cases of the oneofs of the message
     */
    private static List<ProtobufFieldElement> getFieldsAndCases(ProtobufMessageElement message) {
        var result = new ArrayList<ProtobufFieldElement>();
        for (var field : message.fields()) {
            if (field instanceof ProtobufFieldElement.OneOf oneOf) {
                result.addAll(oneOf.cases());
            } else {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Returns whether the given type is a repeated field read in bulk into a buffer that its
     * deserializers convert to the type of the field.
     *
     * @param type the type of the field
     * @return {@code true} if the field is read in bulk and converted from its buffer
     */
    private static boolean isConvertedBulk(ProtobufFieldType type) {
        var bulk = switch (type) {
            case ProtobufCollectionFieldType collectionType -> collectionType.bulk();
            case ProtobufArrayFieldType arrayType -> arrayType.bulk();
            default -> false;
        };
        return bulk && !type.deserializers().isEmpty();
    }

    /**
     * Returns the checked exceptions thrown by an invocation of the given converter, as currently
     * known by the fixed point computation.
     *
     * @param converter the converter
     * @param isOutgoing whether the converter serializes or computes a size, as opposed to
     *                   deserializing
     * @return the checked exceptions
     */
    private List<TypeName> converterExceptions(DaedalusMethodElement converter, boolean isOutgoing) {
        if (converter instanceof DaedalusMethodElement.Inline inline) {
            return thrownTypes(inline);
        }

        var element = converter.element();
        if (element.isPresent()) {
            return thrownTypes(element.get());
        }

        var specName = converter.ownerName();
        var type = types.get(specName);
        if (type != null) {
            // The Spec of a type wraps the checked exceptions of its converters in the base exceptions
            return isOutgoing ? serializationExceptions(type) : deserializationExceptions(type);
        }

        return compiledThrownTypes(converter, isOutgoing);
    }

    /**
     * Returns the checked exceptions declared by the compiled Spec method with the name of the given
     * synthetic converter.
     *
     * <p>If the Spec class cannot be found, for example because it is generated later in the same
     * round by another processor, only the base exception of the direction is assumed.
     *
     * @param converter the synthetic converter
     * @param isOutgoing whether the converter serializes or computes a size
     * @return the checked exceptions
     */
    private List<TypeName> compiledThrownTypes(DaedalusMethodElement converter, boolean isOutgoing) {
        var spec = elements.getTypeElement(converter.ownerName());
        if (spec == null) {
            return isOutgoing ? List.of() : List.of(DESERIALIZATION_EXCEPTION);
        }

        var result = new LinkedHashSet<TypeName>();
        for (var method : ElementFilter.methodsIn(spec.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals(converter.name())) {
                result.addAll(thrownTypes(method));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns every attributed converter applied to the fields of the given message, including the
     * converters of the element, key and value types of collections and maps.
     *
     * @param message the message
     * @return the attributed converters
     */
    private static List<DaedalusConverterElement.Attributed> converters(ProtobufMessageElement message) {
        var result = new ArrayList<DaedalusConverterElement.Attributed>();
        var queue = new ArrayDeque<DaedalusFieldType>();
        for (var field : message.fields()) {
            queue.add(field.type());
        }
        while (!queue.isEmpty()) {
            var type = queue.poll();
            if (type instanceof ProtobufFieldType protobufType) {
                for (var converter : protobufType.converters()) {
                    if (converter instanceof DaedalusConverterElement.Attributed attributed) {
                        result.add(attributed);
                    }
                }
            }
            queue.addAll(type.nestedTypes());
        }
        return result;
    }

    /**
     * Returns the checked exceptions declared by the given inline conversion.
     *
     * @param inline the inline conversion
     * @return the type names of the thrown exceptions
     */
    private static List<TypeName> thrownTypes(DaedalusMethodElement.Inline inline) {
        var result = new ArrayList<TypeName>();
        for (var thrownType : inline.thrownTypes()) {
            result.add(TypeName.get(thrownType));
        }
        return result;
    }

    private static List<TypeName> thrownTypes(ExecutableElement method) {
        var result = new ArrayList<TypeName>();
        for (var thrownType : method.getThrownTypes()) {
            result.add(TypeName.get(thrownType));
        }
        return result;
    }

    /**
     * Returns the qualified name of the Spec class generated for the given type.
     *
     * @param type the type
     * @return the qualified name of its Spec class
     */
    private static String specName(ProtobufTypeElement type) {
        return DaedalusMethodGenerator.getSpecByType(type.typeElement().asType());
    }
}

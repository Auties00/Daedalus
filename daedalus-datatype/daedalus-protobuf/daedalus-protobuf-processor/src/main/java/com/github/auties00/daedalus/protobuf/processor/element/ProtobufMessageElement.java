package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.model.*;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
 * A processed protobuf message element.
 *
 * <p>This class models types annotated with {@code @ProtobufMessage}, collecting
 * their field properties, builders, unknown fields wrapper, and deserializer.
 * Whether a nested message is encoded as a length prefixed message or as a group
 * is a property of the field that holds it, described by
 * {@link ProtobufFieldElement.Message#messageEncoding()}, and not of the message.
 */
public final class ProtobufMessageElement implements ProtobufTypeElement {

    private final ProtobufVersion version;

    private final Set<ClassModifier> builderModifiers;
    private final TypeElement typeElement;
    private final SortedMap<Integer, ProtobufFieldElement> properties;

    /**
     * The field that uses each index of this message, which is a oneof for every index of
     * its cases.
     */
    private final Map<Integer, ProtobufFieldElement> indexOwners;

    /**
     * The Java name of the field, or of the oneof, that uses each Protocol Buffers name of
     * this message, which identifies the field in the text format.
     */
    private final Map<java.lang.String, java.lang.String> nameOwners;
    private final List<ProtobufBuilderElement> builders;
    private final DaedalusMethodElement deserializer;
    private final SequencedSet<? extends ProtobufReservedFieldElement> reservedElements;
    private ProtobufUnknownFieldsElement unknownFieldsElement;

    /**
     * Creates a message element.
     *
     * @param typeElement the source type element
     * @param version the protobuf version for resolving VERSION_DEFAULT values
     * @param builderModifiers the modifiers of the builder of this message
     * @param deserializer the deserializer method, or {@code null} if none
     * @param reserved the reserved field elements
     */
    public ProtobufMessageElement(
            TypeElement typeElement,
            ProtobufVersion version,
            ClassModifier[] builderModifiers,
            DaedalusMethodElement deserializer,
            SequencedSet<? extends ProtobufReservedFieldElement> reserved
    ) {
        this.version = version;
        this.typeElement = typeElement;
        this.builderModifiers = builderModifiers.length == 0 ? EnumSet.noneOf(ClassModifier.class) : EnumSet.copyOf(Arrays.asList(builderModifiers));
        this.deserializer = deserializer;
        this.reservedElements = reserved;
        this.builders = new ArrayList<>();
        this.properties = new TreeMap<>();
        this.indexOwners = new HashMap<>();
        this.nameOwners = new HashMap<>();
    }

    /**
     * Returns the version or edition of Protocol Buffers that this message conforms to.
     *
     * @return the version
     */
    public ProtobufVersion version() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeElement typeElement() {
        return typeElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequencedCollection<ProtobufFieldElement> fields() {
        return Collections.unmodifiableSequencedCollection(properties.sequencedValues());
    }

    /**
     * Adds a field property to this message, unless its index is already used by another
     * field or by a case of a oneof.
     *
     * @param element the source element (field or method)
     * @param accessor the accessor element for reading the property
     * @param type the resolved protobuf field type
     * @param property the normalized field annotation metadata
     * @return the field that already uses the index, in which case the property is not added
     */
    public Optional<ProtobufFieldElement> addProperty(Element element, Element accessor, ProtobufFieldType type, ParsedProtobufField property) {
        var owner = indexOwners.get(property.index());
        if (owner != null) {
            return Optional.of(owner);
        }

        var fieldName = element.getSimpleName().toString();
        var textName = property.name().isEmpty() ? toSnakeCase(fieldName) : property.name();
        var result = createField(fieldName, textName, accessor, element instanceof ExecutableElement, type, property);
        properties.put(property.index(), result);
        indexOwners.put(property.index(), result);
        return Optional.empty();
    }

    /**
     * Records that the field with the given Java name uses the given Protocol Buffers name,
     * unless another field already uses it.
     *
     * <p>The Protocol Buffers name of a field identifies it in the text format, like its index
     * identifies it in the binary format, so it must be unique within a message. The names of
     * the cases of a oneof are the names of fields of the message.
     *
     * @param textName the Protocol Buffers name
     * @param fieldName the Java name of the field, or of the oneof that holds the case
     * @return the Java name of the field that already uses the name, in which case the name is
     *         not recorded
     */
    public Optional<java.lang.String> claimName(java.lang.String textName, java.lang.String fieldName) {
        return Optional.ofNullable(nameOwners.putIfAbsent(textName, fieldName));
    }

    /**
     * Returns the field that uses the given index, which is a oneof if the index is the one
     * of one of its cases.
     *
     * @param index the index
     * @return the field that uses the index, if any
     */
    public Optional<ProtobufFieldElement> indexOwner(int index) {
        return Optional.ofNullable(indexOwners.get(index));
    }

    /**
     * Adds a oneof to this message.
     *
     * <p>The caller must check that the indexes of the cases are used neither by another
     * field nor by another case, with {@link #indexOwner(int)}. The cases are ordered like
     * the {@linkplain ProtobufOneOfFieldType#caseTypes() types of the cases} of the oneof.
     *
     * @param element the source element (field or method)
     * @param accessor the accessor element for reading the oneof
     * @param textName the Protocol Buffers name of the oneof
     * @param type the resolved type of the oneof
     * @param cases the normalized annotations of the cases, ordered like their types
     * @param caseNames the Protocol Buffers names of the cases, ordered like their types
     * @return the oneof
     */
    public ProtobufFieldElement.OneOf addOneOf(Element element, Element accessor, java.lang.String textName, ProtobufOneOfFieldType type, List<ParsedProtobufField> cases, List<java.lang.String> caseNames) {
        var fieldName = element.getSimpleName().toString();
        var synthetic = element instanceof ExecutableElement;
        var caseElements = new ArrayList<ProtobufFieldElement>();
        var lowestIndex = Integer.MAX_VALUE;
        for (var i = 0; i < cases.size(); i++) {
            var caseAnnotation = cases.get(i);
            caseElements.add(createField(fieldName, caseNames.get(i), accessor, synthetic, type.caseTypes().get(i), caseAnnotation));
            lowestIndex = Math.min(lowestIndex, caseAnnotation.index());
        }

        var result = new ProtobufFieldElement.OneOf(lowestIndex, fieldName, textName, accessor, synthetic, List.copyOf(caseElements), type);
        properties.put(lowestIndex, result);
        for (var caseAnnotation : cases) {
            indexOwners.put(caseAnnotation.index(), result);
        }
        return result;
    }

    /**
     * Creates the element of a field, resolving the attributes of its annotation against
     * the version of this message.
     *
     * @param fieldName the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the accessor element for reading the field
     * @param synthetic whether the field is declared by a method
     * @param type the resolved protobuf field type
     * @param property the normalized field annotation metadata
     * @return the element of the field
     */
    private ProtobufFieldElement createField(java.lang.String fieldName, java.lang.String textName, Element accessor, boolean synthetic, ProtobufFieldType type, ParsedProtobufField property) {
        var repeated = type instanceof ProtobufCollectionFieldType || type instanceof ProtobufArrayFieldType;
        return switch (property) {
            case ParsedProtobufField.Scalar f -> new ProtobufFieldElement.Scalar(
                    f.index(), fieldName, textName, accessor, synthetic,
                    resolvePresence(f.fieldPresence(), repeated),
                    repeated && f.type().isPackable() ? f.repeatedFieldEncoding().resolve(version) : ProtobufRepeatedFieldEncoding.EXPANDED,
                    type
            );
            case ParsedProtobufField.String f -> new ProtobufFieldElement.String(
                    f.index(), fieldName, textName, accessor, synthetic,
                    resolvePresence(f.fieldPresence(), repeated),
                    ProtobufRepeatedFieldEncoding.EXPANDED,
                    f.utf8Validation().resolve(version),
                    type
            );
            case ParsedProtobufField.Bytes f -> new ProtobufFieldElement.Bytes(
                    f.index(), fieldName, textName, accessor, synthetic,
                    resolvePresence(f.fieldPresence(), repeated),
                    type
            );
            case ParsedProtobufField.Message f -> new ProtobufFieldElement.Message(
                    f.index(), fieldName, textName, accessor, synthetic,
                    resolveMessagePresence(f.fieldPresence(), repeated),
                    ProtobufRepeatedFieldEncoding.EXPANDED,
                    f.messageEncoding().resolve(version),
                    type
            );
            case ParsedProtobufField.Map f -> new ProtobufFieldElement.Map(
                    f.index(), fieldName, textName, accessor, synthetic,
                    f.utf8Validation().resolve(version),
                    type
            );
        };
    }

    /**
     * Resolves the presence of a field that is not a message.
     *
     * <p>A repeated field has no presence, so its presence is always resolved to
     * {@link ProtobufFieldPresence#EXPLICIT}, which never skips a value.
     *
     * @param presence the declared presence
     * @param repeated whether the field is repeated
     * @return the resolved presence
     */
    private ProtobufFieldPresence resolvePresence(ProtobufFieldPresence presence, boolean repeated) {
        return repeated ? ProtobufFieldPresence.EXPLICIT : presence.resolve(version);
    }

    /**
     * Resolves the presence of a message field.
     *
     * <p>The presence of a message is always tracked, so the default presence of proto3,
     * which is {@link ProtobufFieldPresence#IMPLICIT}, is resolved to
     * {@link ProtobufFieldPresence#EXPLICIT} for a message field. An explicitly implicit
     * message field is rejected during validation.
     *
     * @param presence the declared presence
     * @param repeated whether the field is repeated
     * @return the resolved presence
     */
    private ProtobufFieldPresence resolveMessagePresence(ProtobufFieldPresence presence, boolean repeated) {
        var resolved = resolvePresence(presence, repeated);
        return resolved == ProtobufFieldPresence.IMPLICIT ? ProtobufFieldPresence.EXPLICIT : resolved;
    }

    /**
     * Returns the {@code snake_case} form of the given Java name, which is the name that
     * the Protocol Buffers field it was generated from most likely had.
     *
     * <p>An underscore is inserted before an uppercase letter that follows a lowercase
     * letter or a digit, and before the last letter of a run of uppercase letters that is
     * followed by a lowercase letter, so that {@code URLValue} becomes {@code url_value}.
     *
     * @param name the Java name
     * @return the {@code snake_case} name
     */
    public static java.lang.String toSnakeCase(java.lang.String name) {
        var result = new StringBuilder(name.length() + 4);
        for (var i = 0; i < name.length(); i++) {
            var current = name.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                var previous = name.charAt(i - 1);
                var nextIsLower = i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if (Character.isLowerCase(previous) || Character.isDigit(previous) || (Character.isUpperCase(previous) && nextIsLower)) {
                    result.append('_');
                }
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

    /**
     * Adds a builder element with the full set of {@code @TypeBuilder} annotation attributes.
     *
     * @param className the builder class name
     * @param modifiers the class modifiers for the generated builder
     * @param buildMethodName the terminal build method name
     * @param settersMethodNameSegments the setter name segments with placeholders
     * @param parameters the builder parameters
     * @param executableElement the delegate method or constructor
     */
    public void addBuilder(
            String className,
            ClassModifier[] modifiers,
            String buildMethodName,
            String[] settersMethodNameSegments,
            SequencedCollection<ProtobufBuilderParameterElement> parameters,
            ExecutableElement executableElement
    ) {
        var builderElement = new ProtobufBuilderElement(className, modifiers, buildMethodName, settersMethodNameSegments, parameters, executableElement);
        builders.add(builderElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProtobufBuilderElement> builders() {
        return Collections.unmodifiableList(builders);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true}, as the builder of a message is always generated: the Spec
     * class of the message decodes into it.
     */
    @Override
    public boolean supportsDefaultBuilder() {
        return true;
    }

    /**
     * Returns the modifiers of the builder of this message.
     *
     * <p>Returns the values of {@link com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage#defaultBuilderModifiers()},
     * without duplicates. The methods of the Spec class that decode into the builder are
     * public only if the builder is.
     *
     * @return an unmodifiable set of modifiers
     */
    public Set<ClassModifier> builderModifiers() {
        return Collections.unmodifiableSet(builderModifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DaedalusMethodElement> constructor() {
        return Optional.ofNullable(deserializer);
    }

    /**
     * Returns the unknown fields element, if one is declared on this message.
     *
     * @return the unknown fields element, or empty if none
     */
    public Optional<ProtobufUnknownFieldsElement> unknownFieldsElement() {
        return Optional.ofNullable(unknownFieldsElement);
    }

    /**
     * Sets the unknown fields element for this message.
     *
     * @param unknownFieldsElement the unknown fields element
     */
    public void setUnknownFieldsElement(ProtobufUnknownFieldsElement unknownFieldsElement) {
        this.unknownFieldsElement = unknownFieldsElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequencedSet<? extends ProtobufReservedFieldElement> reservedElements() {
        return Collections.unmodifiableSequencedSet(reservedElements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIndexAllowed(int value) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Index)
                .allMatch(element -> ((ProtobufReservedFieldElement.Index) element).allows(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNameAllowed(String name) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Name)
                .allMatch(element -> ((ProtobufReservedFieldElement.Name) element).allows(name));
    }
}

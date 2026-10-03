package com.github.auties00.daedalus.protobuf.processor;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.processor.graph.DaedalusConverterGraph;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusMixinScopeManager;
import com.github.auties00.daedalus.processor.util.DaedalusMixinInstanceUtils;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.annotation.*;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;
import com.github.auties00.daedalus.protobuf.model.ProtobufFieldPresence;
import com.github.auties00.daedalus.protobuf.model.ProtobufRepeatedFieldEncoding;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.processor.diagnostic.ProtobufProcessorErrors;
import com.github.auties00.daedalus.protobuf.processor.element.*;
import com.github.auties00.daedalus.protobuf.processor.generator.enumeration.ProtobufEnumDeserializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.enumeration.ProtobufEnumSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufTextConverters;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSizeGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderMethodGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderTypeGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.enumeration.ProtobufEnumSpecGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.message.ProtobufMessageSpecGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufTypeManager;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufValidationManager;
import com.github.auties00.daedalus.protobuf.processor.element.ParsedProtobufField;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;
import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSize;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * A {@link DaedalusProcessorExtension} implementation for the Protocol Buffers data format.
 *
 * <p>This extension handles types annotated with {@link ProtobufMessage} and
 * {@link ProtobufEnum}, processing their properties, constants, converters, and
 * builders, and generating the corresponding Spec and Builder classes.
 *
 * <p>It is discovered at compile time via {@link java.util.ServiceLoader} by the
 * {@code DaedalusProcessor} and participates in the five-phase annotation processing
 * pipeline: validation, object processing, converter graph population, attribution,
 * and code generation.
 */
public class ProtobufProcessorExtension implements DaedalusProcessorExtension {
    /**
     * The minimum index of a field.
     */
    private static final int MIN_FIELD_INDEX = 1;

    /**
     * The maximum index of a field, which is the largest value that fits in the 29 bits
     * of a field tag.
     */
    private static final int MAX_FIELD_INDEX = 536_870_911;

    /**
     * The first index of the range that Protocol Buffers reserves for its implementation.
     */
    private static final int MIN_IMPLEMENTATION_FIELD_INDEX = 19_000;

    /**
     * The last index of the range that Protocol Buffers reserves for its implementation.
     */
    private static final int MAX_IMPLEMENTATION_FIELD_INDEX = 19_999;

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] PARSABLE_ANNOTATIONS = new Class[]{
            ProtobufMessage.class,
            ProtobufEnum.class
    };

    /**
     * The annotation processing environment.
     */
    private ProcessingEnvironment processingEnv;


    /**
     * The protobuf specific type utility.
     */
    private ProtobufTypeManager protobufTypes;

    /**
     * The compiler diagnostics utility.
     */
    private DaedalusLogManager messages;

    /**
     * The protobuf specific validation checks.
     */
    private ProtobufValidationManager checks;

    /**
     * The shared converter graph.
     */
    private DaedalusConverterGraph converterGraph;

    /**
     * The set of fully qualified type names that have already been linked into
     * the converter graph, used to avoid duplicate linking.
     */
    private Set<String> linkedTypes;

    /**
     * The mixins whose converters have already been linked into the converter graph,
     * used to link each mixin once, as the graph is shared by every field.
     */
    private Set<TypeElement> linkedMixins;

    /**
     * The mixin scope resolver for auto-applying scoped mixins.
     */
    private DaedalusMixinScopeManager mixinScopeResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "protobuf";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedAnnotationTypes() {
        return Set.of(
                ProtobufMessage.class.getCanonicalName(),
                ProtobufMessage.FloatField.class.getCanonicalName(),
                ProtobufMessage.DoubleField.class.getCanonicalName(),
                ProtobufMessage.BoolField.class.getCanonicalName(),
                ProtobufMessage.Int32Field.class.getCanonicalName(),
                ProtobufMessage.Sint32Field.class.getCanonicalName(),
                ProtobufMessage.Uint32Field.class.getCanonicalName(),
                ProtobufMessage.Fixed32Field.class.getCanonicalName(),
                ProtobufMessage.Sfixed32Field.class.getCanonicalName(),
                ProtobufMessage.Int64Field.class.getCanonicalName(),
                ProtobufMessage.Sint64Field.class.getCanonicalName(),
                ProtobufMessage.Uint64Field.class.getCanonicalName(),
                ProtobufMessage.Fixed64Field.class.getCanonicalName(),
                ProtobufMessage.Sfixed64Field.class.getCanonicalName(),
                ProtobufMessage.EnumField.class.getCanonicalName(),
                ProtobufMessage.StringField.class.getCanonicalName(),
                ProtobufMessage.BytesField.class.getCanonicalName(),
                ProtobufMessage.MessageField.class.getCanonicalName(),
                ProtobufMessage.MapField.class.getCanonicalName(),
                ProtobufMessage.OneOfField.class.getCanonicalName(),
                ProtobufMessage.UnknownFields.class.getCanonicalName(),
                ProtobufEnum.class.getCanonicalName(),
                ProtobufEnum.Constant.class.getCanonicalName(),
                ProtobufAccessor.class.getCanonicalName(),
                ProtobufReservedRange.class.getCanonicalName(),
                ProtobufBuilderParameter.Field.class.getCanonicalName(),
                ProtobufBuilderParameter.Synthetic.class.getCanonicalName()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ProcessingEnvironment processingEnv, DaedalusTypeManager types, DaedalusLogManager messages, DaedalusMixinScopeManager mixinScopeResolver) {
        this.processingEnv = processingEnv;
        this.protobufTypes = new ProtobufTypeManager(processingEnv);
        this.messages = messages;
        this.checks = new ProtobufValidationManager(protobufTypes, messages);
        this.linkedTypes = new HashSet<>();
        this.linkedMixins = new HashSet<>();
        this.mixinScopeResolver = mixinScopeResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isManagedType(TypeMirror type) {
        return protobufTypes.isObject(type);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A protobuf enum may convert its values to and from their index with its own
     * converters, which its generated Spec class delegates to.
     */
    @Override
    public boolean allowsDeclaredConverters(TypeMirror type) {
        return protobufTypes.isEnum(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runChecks(RoundEnvironment roundEnv) {
        checks.runChecks(roundEnv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequencedCollection<? extends DaedalusTypeElement> processObjects(
            RoundEnvironment roundEnv,
            DaedalusConverterGraph converterGraph
    ) {
        this.converterGraph = converterGraph;
        return Arrays.stream(PARSABLE_ANNOTATIONS)
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(entry -> entry instanceof TypeElement)
                .map(entry -> processElement((TypeElement) entry))
                .flatMap(Set::stream)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateCode(SequencedCollection<? extends DaedalusTypeElement> objects) {
        warnSlowPackedFields(objects);
        warnBinaryOnlyFields(objects);
        if (!checkNestedBuilders(objects)) {
            return;
        }

        TypeElement currentElement = null;
        var exceptions = new ProtobufExceptionManager(processingEnv.getElementUtils(), objects);
        try {
            for (var object : objects) {
                if (!(object instanceof ProtobufTypeElement protobufType)) {
                    continue;
                }
                currentElement = protobufType.typeElement();
                var packageName = processingEnv.getElementUtils().getPackageOf(protobufType.typeElement());
                switch (protobufType) {
                    case ProtobufMessageElement protobufMessage -> {
                        var specVisitor = new ProtobufMessageSpecGenerator(processingEnv.getFiler(), exceptions);
                        specVisitor.createClass(protobufMessage, packageName);
                        var typeGenerator = new ProtobufBuilderTypeGenerator(processingEnv.getFiler(), protobufTypes, exceptions);
                        typeGenerator.createClass(packageName.getQualifiedName().toString(), protobufMessage);
                        for (var builder : protobufMessage.builders()) {
                            // A builder without a name was reported by checkBuilderParameters
                            if (builder.name().isEmpty()) {
                                continue;
                            }

                            var methodGenerator = new ProtobufBuilderMethodGenerator(processingEnv.getFiler(), protobufTypes);
                            methodGenerator.createClass(packageName.getQualifiedName().toString(), protobufMessage, builder);
                        }
                    }
                    case ProtobufEnumElement protobufEnum -> {
                        var specVisitor = new ProtobufEnumSpecGenerator(processingEnv.getFiler(), exceptions);
                        specVisitor.createClass(protobufEnum, packageName);
                    }
                }
            }
        } catch (IOException throwable) {
            messages.printError(ProtobufProcessorErrors.generationFailed(currentElement, throwable), currentElement);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the four concrete I/O types used by generated protobuf spec classes:
     * {@link ProtobufBinaryReader}, {@link ProtobufBinaryWriter},
     * {@link ProtobufTextReader}, and
     * {@link ProtobufTextWriter}.
     */
    @Override
    public Set<TypeMirror> supportedIOTypes() {
        return Set.of(
                protobufTypes.getType(ProtobufBinaryReader.class),
                protobufTypes.getType(ProtobufBinaryWriter.class),
                protobufTypes.getType(ProtobufTextReader.class),
                protobufTypes.getType(ProtobufTextWriter.class)
        );
    }

    /**
     * Processes a single type element, dispatching to the appropriate handler
     * based on its element kind.
     *
     * @param object the type element to process
     * @return the set of processed protobuf objects, possibly empty
     */
    private Set<ProtobufTypeElement> processElement(TypeElement object) {
        // Any kind of type can be a protobuf enum, including sealed interfaces and abstract classes
        if (object.getAnnotation(ProtobufEnum.class) != null) {
            return isEnumVariant(object) ? Set.of() : processEnum(object);
        }

        if (object.getModifiers().contains(Modifier.ABSTRACT)) {
            return Set.of();
        }

        return switch (object.getKind()) {
            case ENUM -> processEnum(object);
            case RECORD, CLASS -> processObject(object);
            default -> Set.of();
        };
    }

    /**
     * Returns the element that declares the given field in the source code, which diagnostics
     * about the field are reported on.
     *
     * <p>The accessor of a record component is implicitly declared, so it has no position in
     * the source code: the record component is returned instead.
     *
     * @param field the field
     * @return the element that declares the field
     */
    private Element getDeclaration(ProtobufFieldElement field) {
        if (field.accessor() instanceof ExecutableElement method && isRecordComponentAccessor(method)) {
            var record = (TypeElement) method.getEnclosingElement();
            for (var component : record.getRecordComponents()) {
                if (method.equals(component.getAccessor())) {
                    return component;
                }
            }
        }
        return field.accessor();
    }

    /**
     * Checks that the builder of every message held by a field is accessible from the builder
     * of the message that declares the field, which holds it to merge the occurrences of the
     * field.
     *
     * <p>A builder is accessible if it is public, or if it is in the same package. Whether a
     * field holds the builder of a message is only known once the converters are attributed,
     * which happens before the code is generated.
     *
     * @param objects the processed objects
     * @return {@code true} if every builder is accessible, {@code false} if an error was
     *         reported, in which case no code can be generated
     */
    private boolean checkNestedBuilders(SequencedCollection<? extends DaedalusTypeElement> objects) {
        var elements = processingEnv.getElementUtils();
        var valid = true;
        for (var object : objects) {
            if (!(object instanceof ProtobufMessageElement message)) {
                continue;
            }

            var packageElement = elements.getPackageOf(message.typeElement());
            for (var field : getFieldsAndCases(message)) {
                var nestedMessage = field.type().builderMessage();
                if (nestedMessage.isEmpty()) {
                    continue;
                }

                var nestedElement = (TypeElement) nestedMessage.get().asElement();
                var nestedAnnotation = nestedElement.getAnnotation(ProtobufMessage.class);
                if (Arrays.asList(nestedAnnotation.defaultBuilderModifiers()).contains(ClassModifier.PUBLIC) || elements.getPackageOf(nestedElement).equals(packageElement)) {
                    continue;
                }

                var builderName = DaedalusMethodGenerator.getBuilderByType(nestedMessage.get());
                messages.printError(ProtobufProcessorErrors.fieldBuilderNotAccessible(field.name(), nestedMessage.get(), builderName), getDeclaration(field));
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Warns about every packed field that is not written and read in bulk, because its value
     * cannot be converted to and from an array of the primitive type of its wire values.
     *
     * <p>The outcome of the optional converters that decide it is only known once the
     * converters are attributed, which happens before the code is generated.
     *
     * @param objects the processed objects
     */
    private void warnSlowPackedFields(SequencedCollection<? extends DaedalusTypeElement> objects) {
        for (var object : objects) {
            if (!(object instanceof ProtobufMessageElement message)) {
                continue;
            }

            for (var field : message.fields()) {
                var slowType = switch (field.type()) {
                    case ProtobufCollectionFieldType collectionType when !collectionType.bulk() -> collectionType;
                    case ProtobufArrayFieldType arrayType when !arrayType.bulk() -> arrayType;
                    default -> null;
                };
                if (field.packed() && slowType != null) {
                    var bulkType = ProtobufArrayFieldType.getBulkComponentType(slowType.protobufType()) + "[]";
                    messages.printWarning(ProtobufProcessorErrors.fieldPackedNotBulk(slowType.descriptorElementType(), slowType.protobufType(), bulkType), getDeclaration(field));
                }
            }
        }
    }

    /**
     * Warns about every field whose value is converted by a converter that uses the binary
     * format directly and has no text overload, as the field cannot be written to, or read
     * from, the text format.
     *
     * <p>The converters of a field are only known once they are attributed, which happens
     * before the code is generated.
     *
     * @param objects the processed objects
     */
    private void warnBinaryOnlyFields(SequencedCollection<? extends DaedalusTypeElement> objects) {
        for (var object : objects) {
            if (!(object instanceof ProtobufMessageElement message)) {
                continue;
            }

            for (var field : getFieldsAndCases(message)) {
                var converter = ProtobufTextConverters.findBinaryOnlySerializer(field.type())
                        .or(() -> field.isSynthetic() ? Optional.empty() : ProtobufTextConverters.findBinaryOnlyDeserializer(field.type()));
                if (converter.isPresent()) {
                    var converterName = converter.get().ownerName() + "." + converter.get().name();
                    messages.printWarning(ProtobufProcessorErrors.fieldBinaryOnlyConverter(field.textName(), converterName), getDeclaration(field));
                }
            }
        }
    }

    /**
     * Returns whether the given protobuf enum is a permitted subtype of a sealed protobuf
     * enum, whose values it is part of: it is encoded through its parent, so it is not
     * processed on its own.
     *
     * @param object the protobuf enum type element
     * @return {@code true} if a direct supertype of the type is a protobuf enum
     */
    private boolean isEnumVariant(TypeElement object) {
        if (protobufTypes.isEnum(object.getSuperclass())) {
            return true;
        }
        for (var supertype : object.getInterfaces()) {
            if (protobufTypes.isEnum(supertype)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a class or record annotated with {@link ProtobufMessage}.
     *
     * @param typeElement the class or record type element
     * @return the set of processed protobuf objects
     */
    private Set<ProtobufTypeElement> processObject(TypeElement typeElement) {
        var messageAnnotation = typeElement.getAnnotation(ProtobufMessage.class);
        if (messageAnnotation != null) {
            var deserializer = getObjectDeserializer(typeElement)
                    .orElse(null);
            var reserved = ProtobufReservedFieldElement.of(messageAnnotation);
            var builderModifiers = checkBuilderModifiers(typeElement, messageAnnotation.defaultBuilderModifiers());
            var messageElement = new ProtobufMessageElement(typeElement, messageAnnotation.version(), builderModifiers, deserializer, reserved);
            var results = processObject(messageElement, messageElement.typeElement());
            linkMessage(typeElement.asType());
            checkFieldsConstructor(messageElement);
            checkBuilderParameters(messageElement);
            return results;
        }

        // An error will be printed by the preliminary checks
        return Set.of();
    }

    /**
     * Checks that the modifiers of the builder of the given message can be applied to a top
     * level class that the Spec class of the message instantiates, which are
     * {@link ClassModifier#PUBLIC} and {@link ClassModifier#FINAL}, and returns the ones that
     * can.
     *
     * <p>Every other modifier is reported as an error, and left out of the returned ones, so
     * that the message is still processed, and the messages that reference it do not report
     * errors caused by this one.
     *
     * @param typeElement the message
     * @param modifiers the modifiers of the builder of the message
     * @return the modifiers of the builder that are legal
     */
    private ClassModifier[] checkBuilderModifiers(TypeElement typeElement, ClassModifier[] modifiers) {
        var legal = new ArrayList<ClassModifier>();
        for (var modifier : modifiers) {
            switch (modifier) {
                case PUBLIC, FINAL -> legal.add(modifier);
                default -> messages.printError(ProtobufProcessorErrors.builderInvalidModifier(modifier), typeElement);
            }
        }
        return legal.toArray(ClassModifier[]::new);
    }

    /**
     * Checks that every builder declared on the given message with {@link TypeBuilder} has a
     * name, so that it does not clash with the builder that is always generated for the
     * message, and that every builder parameter annotated with
     * {@link ProtobufBuilderParameter.Field} maps to a field of the message.
     *
     * @param messageElement the protobuf message element whose builders are checked
     */
    private void checkBuilderParameters(ProtobufMessageElement messageElement) {
        for (var builder : messageElement.builders()) {
            if (builder.name().isEmpty()) {
                var builderName = DaedalusMethodGenerator.getBuilderByType(messageElement.typeElement().asType());
                messages.printError(ProtobufProcessorErrors.builderMissingName(builderName), builder.delegate());
            }

            for (var parameter : builder.parameters()) {
                if (!(parameter instanceof ProtobufBuilderParameterElement.Field field)) {
                    continue;
                }

                if (messageElement.indexOwner(field.fieldIndex()).isEmpty()) {
                    messages.printError(ProtobufProcessorErrors.builderUnknownFieldIndex(field.fieldIndex()), field.element());
                }
            }
        }
    }

    /**
     * Finds a method annotated with {@link TypeDeserializer} inside the given message type.
     *
     * @param message the message type element
     * @return the converter method, if found
     */
    private Optional<DaedalusMethodElement> getObjectDeserializer(TypeElement message) {
        DaedalusMethodElement deserializer = null;
        for (var element : message.getEnclosedElements()) {
            if (!(element instanceof ExecutableElement method)) {
                continue;
            }

            var annotation = method.getAnnotation(TypeDeserializer.class);
            if (annotation == null) {
                continue;
            }

            if (deserializer != null) {
                messages.printError(ProtobufProcessorErrors.messageDuplicatedDeserializer(method, deserializer.name()), method);
                continue;
            }

            deserializer = DaedalusMethodElement.of(method, protobufTypes.isParametrized(method), annotation.warning());
        }
        return Optional.ofNullable(deserializer);
    }

    /**
     * Recursively processes a protobuf message element and its superclass hierarchy,
     * collecting fields, methods, builders, serializers, and deserializers.
     *
     * @param messageElement the root protobuf message element
     * @param typeElement the current type element in the hierarchy
     * @return the set of protobuf objects discovered during processing
     */
    private Set<ProtobufTypeElement> processObject(ProtobufMessageElement messageElement, TypeElement typeElement) {
        Set<ProtobufTypeElement> results = new HashSet<>();

        // Add the element being processed only if we are not processing a super class
        if (messageElement.typeElement() == typeElement) {
            results.add(messageElement);
        }

        // If this element has a valid super class, process that as well under the same element
        protobufTypes.getDirectSuperClass(typeElement).ifPresent(superClass -> {
            var superResults = processObject(messageElement, superClass);
            results.addAll(superResults);
        });

        // We could run directly the processing on fields and methods
        // But supporting standalone getters requires either to run fields before methods or to run additional checks later
        List<VariableElement> fields = new ArrayList<>();
        List<ExecutableElement> methods = new ArrayList<>();
        for (var entry : typeElement.getEnclosedElements()) {
            switch (entry) {
                case VariableElement variableElement -> fields.add(variableElement);
                case ExecutableElement executableElement -> methods.add(executableElement);
                default -> {}
            }
        }

        // Does the actual processing
        for (var field : fields) {
            results.addAll(processObjectField(messageElement, field));
        }
        for (var method : methods) {
            processObjectMethod(messageElement, method);
        }

        return results;
    }

    /**
     * Processes a method in a class or record annotated with {@link ProtobufMessage},
     * handling field, builder, accessor, serializer, and deserializer annotations.
     *
     * <p>A method annotated with a field annotation declares a synthetic field, whose
     * value is the value returned by the method: it is serialized, but not deserialized.
     *
     * @param messageElement the enclosing protobuf message element
     * @param executableElement the method to process
     */
    private void processObjectMethod(ProtobufMessageElement messageElement, ExecutableElement executableElement) {
        // The accessor of a record component inherits the field annotation of the component
        var oneOfAnnotation = executableElement.getAnnotation(ProtobufMessage.OneOfField.class);
        if (oneOfAnnotation != null && !isRecordComponentAccessor(executableElement)) {
            processOneOfField(messageElement, executableElement, oneOfAnnotation);
            return;
        }

        var propertyAnnotation = ParsedProtobufField.of(executableElement);
        if (propertyAnnotation.isPresent() && !isRecordComponentAccessor(executableElement)) {
            processMessageProperty(messageElement, executableElement, propertyAnnotation.get());
            return;
        }

        var builder = executableElement.getAnnotation(TypeBuilder.class);
        if (builder != null) {
            var parameters = executableElement.getParameters()
                    .stream()
                    .map(param -> {
                        var fieldParam = param.getAnnotation(ProtobufBuilderParameter.Field.class);
                        if (fieldParam != null) {
                            return (ProtobufBuilderParameterElement) new ProtobufBuilderParameterElement.Field(param, fieldParam.index());
                        }
                        return (ProtobufBuilderParameterElement) new ProtobufBuilderParameterElement.Synthetic(param);
                    })
                    .toList();
            messageElement.addBuilder(
                    builder.name(),
                    builder.modifiers(),
                    builder.buildMethodName(),
                    builder.settersMethodName(),
                    parameters,
                    executableElement
            );
        } else {
            var getter = executableElement.getAnnotation(ProtobufAccessor.class);
            if (getter != null) {
                handleMessageGetter(messageElement, executableElement, getter);
            } else {
                processObjectSerializer(executableElement);
                processObjectSizer(executableElement);
                processObjectDeserializer(executableElement);
            }
        }
    }

    /**
     * Returns whether the given method is the accessor of a record component.
     *
     * @param method the method to check
     * @return {@code true} if the method is enclosed by a record and accesses one of its
     *         components
     */
    private boolean isRecordComponentAccessor(ExecutableElement method) {
        if (!(method.getEnclosingElement() instanceof TypeElement typeElement) || typeElement.getKind() != ElementKind.RECORD) {
            return false;
        }

        for (var component : typeElement.getRecordComponents()) {
            if (method.equals(component.getAccessor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Links a method annotated with {@link TypeDeserializer} into the deserializer graph.
     *
     * @param executableElement the method to process
     */
    private void processObjectDeserializer(ExecutableElement executableElement) {
        var deserializer = executableElement.getAnnotation(TypeDeserializer.class);
        if (deserializer == null) {
            return;
        }

        var from = executableElement.getParameters().getFirst().asType();
        var to = executableElement.getReturnType();
        var method = DaedalusMethodElement.of(executableElement, protobufTypes.isParametrized(executableElement), deserializer.warning());
        converterGraph.linkDeserializer(from, to, method);
    }

    /**
     * Links a method annotated with {@link TypeSerializer} into the serializer graph.
     *
     * @param executableElement the method to process
     */
    private void processObjectSerializer(ExecutableElement executableElement) {
        var serializer = executableElement.getAnnotation(TypeSerializer.class);
        if (serializer == null) {
            return;
        }

        // An instance serializer of the type of the value receives it as this, while a static one or one of a mixin instance receives it as its first parameter
        var from = DaedalusMixinInstanceUtils.receivesValueAsFirstParameter(executableElement) && !executableElement.getParameters().isEmpty()
                ? executableElement.getParameters().getFirst().asType()
                : executableElement.getEnclosingElement().asType();
        var to = executableElement.getReturnType();
        var method = DaedalusMethodElement.of(executableElement, protobufTypes.isParametrized(executableElement), serializer.warning());
        converterGraph.linkSerializer(from, to, method);
    }

    /**
     * Attaches a method annotated with {@link TypeSize} as the sizer for its declared
     * value type. The graph either updates an already-registered serializer node or
     * stores the sizer as an orphan to be consumed by a future serializer registration.
     *
     * <p>Mixin sizers are static and take the value type as their first parameter,
     * while non-mixin sizers are instance methods whose enclosing type is the
     * value type.
     *
     * @param executableElement the method to process
     */
    private void processObjectSizer(ExecutableElement executableElement) {
        var sizeAnnotation = executableElement.getAnnotation(TypeSize.class);
        if (sizeAnnotation == null) {
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        var valueType = protobufTypes.isMixin(enclosingType) && !executableElement.getParameters().isEmpty()
                ? executableElement.getParameters().getFirst().asType()
                : enclosingType;
        var method = DaedalusMethodElement.of(executableElement, protobufTypes.isParametrized(executableElement), "");
        converterGraph.attachSizer(valueType, method);
    }

    /**
     * Validates that a getter annotated with {@link ProtobufAccessor} has a matching property.
     *
     * @param messageElement the enclosing protobuf message element
     * @param executableElement the getter method
     * @param getter the accessor annotation
     */
    private void handleMessageGetter(ProtobufMessageElement messageElement, ExecutableElement executableElement, ProtobufAccessor getter) {
        if (hasMatchedProperty(messageElement, getter)) {
            return;
        }
        messages.printError(ProtobufProcessorErrors.accessorUnknownIndex(executableElement, getter.index(), messageElement.typeElement()), executableElement);
    }

    /**
     * Returns whether the given accessor annotation matches a property in the message.
     *
     * @param messageElement the protobuf message element
     * @param getter the accessor annotation
     * @return {@code true} if a matching property exists
     */
    private boolean hasMatchedProperty(ProtobufMessageElement messageElement, ProtobufAccessor getter) {
        return messageElement.indexOwner(getter.index())
                .filter(entry -> !entry.isSynthetic())
                .isPresent();
    }

    /**
     * Checks that the given message has a constructor whose parameters match its
     * properties in declaration order, optionally including unknown fields. If no
     * matching constructor is found, prints an error showing the expected signature
     * and all constructors that were found.
     *
     * @param message the protobuf message element
     */
    private void checkFieldsConstructor(ProtobufMessageElement message) {
        var unknownFieldsElement = message.unknownFieldsElement()
                .orElse(null);
        var fields = message.fields()
                .stream()
                .filter(field -> !field.isSynthetic())
                .toList();

        // A field that was rejected already reported why, and the expected constructor would not include it
        if (fields.size() != countDeclaredFields(message.typeElement())) {
            return;
        }

        var constructors = message.typeElement()
                .getEnclosedElements()
                .stream()
                .filter(entry -> entry.getKind() == ElementKind.CONSTRUCTOR)
                .map(entry -> (ExecutableElement) entry)
                .toList();
        var expectedParamCount = fields.size() + (unknownFieldsElement != null ? 1 : 0);
        for (var constructor : constructors) {
            var constructorParameters = constructor.getParameters();
            if (expectedParamCount != constructorParameters.size()) {
                continue;
            }

            var propertiesMatch = true;
            for (var i = 0; i < fields.size(); i++) {
                var property = fields.get(i);
                var parameter = constructorParameters.get(i);
                if (!protobufTypes.isAssignable(property.type().descriptorElementType(), parameter.asType())) {
                    propertiesMatch = false;
                    break;
                }
            }
            if (!propertiesMatch) {
                continue;
            }

            if (unknownFieldsElement != null) {
                var unknownFieldsParameter = constructorParameters.get(fields.size());
                if (!protobufTypes.isAssignable(unknownFieldsParameter.asType(), unknownFieldsElement.type())) {
                    continue;
                }
            }

            return;
        }

        List<String> expectedParameters = new ArrayList<>();
        for (var property : fields) {
            expectedParameters.add(property.type().descriptorElementType() + " " + property.name());
        }
        if (unknownFieldsElement != null) {
            expectedParameters.add(unknownFieldsElement.type() + " " + unknownFieldsElement.field().getSimpleName());
        }

        messages.printError(ProtobufProcessorErrors.messageMissingConstructor(message.typeElement(), expectedParameters, constructors), message.typeElement());
    }

    /**
     * Returns the number of fields, declared by the given type and by its superclasses,
     * that are annotated with a field annotation.
     *
     * @param typeElement the type whose fields are counted
     * @return the number of fields
     */
    private long countDeclaredFields(TypeElement typeElement) {
        var count = typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element instanceof VariableElement && !ParsedProtobufField.annotationNames(element).isEmpty())
                .count();
        return count + protobufTypes.getDirectSuperClass(typeElement)
                .map(this::countDeclaredFields)
                .orElse(0L);
    }

    /**
     * Processes a field in a class or record annotated with {@link ProtobufMessage},
     * handling property and unknown fields annotations.
     *
     * @param messageElement the enclosing protobuf message element
     * @param variableElement the field to process
     * @return the set of protobuf objects discovered during processing
     */
    private Set<ProtobufTypeElement> processObjectField(ProtobufMessageElement messageElement, VariableElement variableElement) {
        var oneOfAnnotation = variableElement.getAnnotation(ProtobufMessage.OneOfField.class);
        if (oneOfAnnotation != null) {
            return processOneOfField(messageElement, variableElement, oneOfAnnotation);
        }

        var propertyAnnotation = ParsedProtobufField.of(variableElement);
        if (propertyAnnotation.isPresent()) {
            return processMessageProperty(messageElement, variableElement, propertyAnnotation.get());
        }

        var unknownFieldsAnnotation = variableElement.getAnnotation(ProtobufMessage.UnknownFields.class);
        if (unknownFieldsAnnotation != null) {
            processMessageUnknownFields(messageElement, variableElement, unknownFieldsAnnotation);
        }

        return Set.of();
    }

    /**
     * Processes a field annotated with {@link ProtobufMessage.UnknownFields}.
     *
     * @param messageElement the enclosing protobuf message element
     * @param variableElement the annotated field
     * @param unknownFieldsAnnotation the unknown fields annotation
     */
    private void processMessageUnknownFields(ProtobufMessageElement messageElement, VariableElement variableElement, ProtobufMessage.UnknownFields unknownFieldsAnnotation) {
        var existingUnknownFields = messageElement.unknownFieldsElement();
        if (existingUnknownFields.isPresent()) {
            messages.printError(ProtobufProcessorErrors.unknownFieldsDuplicated(variableElement, existingUnknownFields.get().field()), variableElement);
            return;
        }

        var unknownFields = processUnknownFieldsField(variableElement, unknownFieldsAnnotation);
        if (unknownFields.isEmpty()) {
            return;
        }

        messageElement.setUnknownFieldsElement(unknownFields.get());
        var mixins = protobufTypes.getMixins(unknownFieldsAnnotation);
        linkMixins(mixins);
    }

    /**
     * Processes the type of an unknown fields field, validating that the
     * field's declared type is compatible with
     * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields}
     * either directly or via a registered conversion mixin, and that it can be read.
     *
     * <p>A field whose type is not a store needs a mixin with a static method that converts
     * it to a store, and one that converts a store to it.
     *
     * @param variableElement         the annotated field
     * @param unknownFieldsAnnotation the unknown fields annotation
     * @return the unknown fields element, if valid
     */
    private Optional<ProtobufUnknownFieldsElement> processUnknownFieldsField(VariableElement variableElement, ProtobufMessage.UnknownFields unknownFieldsAnnotation) {
        var unknownFieldsType = variableElement.asType();
        if (!(unknownFieldsType instanceof DeclaredType)) {
            messages.printError(ProtobufProcessorErrors.unknownFieldsNotDeclaredType(variableElement), variableElement);
            return Optional.empty();
        }

        var accessor = getUnknownFieldsAccessor(variableElement);
        if (accessor.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.unknownFieldsMissingAccessor(variableElement), variableElement);
            return Optional.empty();
        }

        // Every format collects its unknown fields in a ProtobufUnknownFields, which the field must be able to hold
        var storeType = protobufTypes.getType(ProtobufUnknownFields.class);
        var mixins = protobufTypes.getMixins(unknownFieldsAnnotation);
        if (protobufTypes.isAssignable(storeType, unknownFieldsType)) {
            return Optional.of(new ProtobufUnknownFieldsElement(variableElement, accessor.get(), unknownFieldsType, null, null));
        }

        var serializer = findMixinConversion(mixins, unknownFieldsType, storeType);
        var deserializer = findMixinConversion(mixins, storeType, unknownFieldsType);
        if (serializer.isEmpty() || deserializer.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.unknownFieldsNotConvertible(variableElement, storeType), variableElement);
            return Optional.empty();
        }

        return Optional.of(new ProtobufUnknownFieldsElement(variableElement, accessor.get(), unknownFieldsType, serializer.get(), deserializer.get()));
    }

    /**
     * Finds the element that reads the unknown fields of a message, which is the field
     * itself unless it is private, or a getter named after it otherwise.
     *
     * @param variableElement the annotated field
     * @return the accessor, if found
     */
    private Optional<? extends Element> getUnknownFieldsAccessor(VariableElement variableElement) {
        if (!variableElement.getModifiers().contains(Modifier.PRIVATE)) {
            return Optional.of(variableElement);
        }

        var methods = variableElement.getEnclosingElement()
                .getEnclosedElements()
                .stream()
                .filter(entry -> entry instanceof ExecutableElement)
                .map(entry -> (ExecutableElement) entry)
                .filter(element -> !element.getModifiers().contains(Modifier.PRIVATE))
                .toList();
        return inferAccessor(variableElement, methods);
    }

    /**
     * Finds a static method of the given mixins, annotated with {@link TypeSerializer} or
     * {@link TypeDeserializer}, that converts a value of the given type to the other one.
     *
     * @param mixins the mixins to search
     * @param from   the type of the value to convert
     * @param to     the type of the converted value
     * @return the method, if found
     */
    private Optional<ExecutableElement> findMixinConversion(Set<TypeElement> mixins, TypeMirror from, TypeMirror to) {
        for (var mixin : mixins) {
            for (var enclosed : mixin.getEnclosedElements()) {
                if (!(enclosed instanceof ExecutableElement method) || method.getKind() != ElementKind.METHOD || !DaedalusMixinInstanceUtils.receivesValueAsFirstParameter(method) || method.getParameters().size() != 1) {
                    continue;
                }
                if (method.getAnnotation(TypeSerializer.class) == null && method.getAnnotation(TypeDeserializer.class) == null) {
                    continue;
                }
                if (protobufTypes.isAssignable(from, method.getParameters().getFirst().asType()) && protobufTypes.isAssignable(method.getReturnType(), to)) {
                    return Optional.of(method);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Processes an element with a protobuf field annotation, validating
     * its type, index, name, presence, accessor, and converters.
     *
     * <p>The element is either a field, or a method that declares a synthetic field.
     *
     * @param messageElement the enclosing protobuf message element
     * @param element the annotated field or method
     * @param propertyAnnotation the normalized field annotation
     * @return the set of protobuf objects discovered during linking
     */
    private Set<ProtobufTypeElement> processMessageProperty(ProtobufMessageElement messageElement, Element element, ParsedProtobufField propertyAnnotation) {
        if (!checkPropertyAnnotation(messageElement, element, propertyAnnotation)) {
            return Set.of();
        }

        Optional<? extends Element> accessor = element instanceof VariableElement variableElement
                ? getAccessor(variableElement, Set.of(propertyAnnotation.index()))
                : Optional.of(element);
        if (accessor.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.fieldMissingAccessor(element, propertyAnnotation.index()), element);
            return Set.of();
        }

        var accessorType = getAccessorType(accessor.get());
        var variableType = element instanceof ExecutableElement method ? method.getReturnType() : element.asType();
        var type = getPropertyType(element, variableType, accessorType, propertyAnnotation, isPacked(messageElement, propertyAnnotation));
        if (type.isEmpty() || !checkPropertyType(messageElement, element, variableType, propertyAnnotation, type.get())) {
            return Set.of();
        }

        var javaName = element.getSimpleName().toString();
        var textName = propertyAnnotation.name().isEmpty() ? ProtobufMessageElement.toSnakeCase(javaName) : propertyAnnotation.name();
        if (!messageElement.isNameAllowed(textName)) {
            messages.printError(ProtobufProcessorErrors.fieldReservedName(textName), element);
        } else if (!messageElement.isNameAllowed(javaName)) {
            messages.printError(ProtobufProcessorErrors.fieldReservedName(javaName), element);
        }

        if (!messageElement.isIndexAllowed(propertyAnnotation.index())) {
            messages.printError(ProtobufProcessorErrors.fieldReservedIndex(propertyAnnotation.index()), element);
        }

        var error = messageElement.addProperty(element, accessor.get(), type.get(), propertyAnnotation);
        if (error.isPresent()) {
            messages.printError(ProtobufProcessorErrors.fieldDuplicatedIndex(element, error.get().name(), propertyAnnotation.index()), element);
            return Set.of();
        }

        var nameOwner = messageElement.claimName(textName, javaName);
        if (nameOwner.isPresent()) {
            messages.printError(ProtobufProcessorErrors.fieldDuplicatedName(element, nameOwner.get(), textName), element);
            return Set.of();
        }

        // The resolved mixins also include the scoped and imported ones, not only the explicit ones
        linkMixins(type.get().mixins().all());
        return linkType(variableType);
    }

    /**
     * Returns the fields of the given message, replacing each oneof with its cases.
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
     * Processes an element annotated with {@link ProtobufMessage.OneOfField}, validating its
     * sealed type and its cases.
     *
     * <p>Each case is validated like a field of the same kind whose type is the permitted
     * subtype that holds the case. Every type that the sealed type of the oneof permits,
     * expanding the permitted types that are sealed in turn, must be held by exactly one
     * case: a sealed class that is not abstract is itself one of these types.
     *
     * @param messageElement the enclosing protobuf message element
     * @param element the annotated field or method
     * @param annotation the oneof annotation
     * @return the set of protobuf objects discovered during linking
     */
    private Set<ProtobufTypeElement> processOneOfField(ProtobufMessageElement messageElement, Element element, ProtobufMessage.OneOfField annotation) {
        var annotations = ParsedProtobufField.annotationNames(element);
        if (annotations.size() > 1) {
            messages.printError(ProtobufProcessorErrors.fieldConflictingAnnotations(annotations), element);
            return Set.of();
        }

        if (element instanceof ExecutableElement method
                && (method.getModifiers().contains(Modifier.PRIVATE) || method.getModifiers().contains(Modifier.STATIC) || !method.getParameters().isEmpty() || method.getReturnType().getKind() == TypeKind.VOID)) {
            messages.printError(ProtobufProcessorErrors.fieldSyntheticMethod(), element);
            return Set.of();
        }

        var variableType = element instanceof ExecutableElement method ? method.getReturnType() : element.asType();
        if (!(variableType instanceof DeclaredType declaredType)
                || !(declaredType.asElement() instanceof TypeElement sealedElement)
                || !sealedElement.getModifiers().contains(Modifier.SEALED)) {
            messages.printError(ProtobufProcessorErrors.oneOfNotSealed(variableType), element);
            return Set.of();
        }

        var cases = ParsedProtobufField.cases(annotation);
        if (cases.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.oneOfEmpty(variableType), element);
            return Set.of();
        }

        var valid = true;
        var javaName = element.getSimpleName().toString();
        var textName = annotation.name().isEmpty() ? ProtobufMessageElement.toSnakeCase(javaName) : annotation.name();
        if (!annotation.name().isEmpty() && !isValidFieldName(annotation.name())) {
            messages.printError(ProtobufProcessorErrors.fieldInvalidName(annotation.name()), element);
            valid = false;
        } else if (!messageElement.isNameAllowed(textName)) {
            messages.printError(ProtobufProcessorErrors.fieldReservedName(textName), element);
            valid = false;
        }

        var indexes = new LinkedHashSet<Integer>();
        for (var oneOfCase : cases) {
            if (!indexes.add(oneOfCase.field().index())) {
                messages.printError(ProtobufProcessorErrors.oneOfDuplicatedCaseIndex(oneOfCase.field().index()), element);
                valid = false;
            }
        }

        Optional<? extends Element> accessor = element instanceof VariableElement variableElement
                ? getAccessor(variableElement, indexes)
                : Optional.of(element);
        if (accessor.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.fieldMissingAccessor(element, indexes.getFirst()), element);
            return Set.of();
        }

        var permittedTypes = new LinkedHashSet<TypeElement>();
        collectOneOfTypes(sealedElement, permittedTypes);
        var caseTypes = new HashMap<TypeElement, Integer>();
        List<ResolvedOneOfCase> resolvedCases = new ArrayList<>();
        for (var oneOfCase : cases) {
            var field = oneOfCase.field();
            var index = field.index();
            var caseType = protobufTypes.getMirroredType(oneOfCase.type());
            var caseElement = caseType instanceof DeclaredType caseDeclaredType && caseDeclaredType.asElement() instanceof TypeElement typeElement ? typeElement : null;
            if (caseElement == null || !permittedTypes.contains(caseElement)) {
                messages.printError(ProtobufProcessorErrors.oneOfCaseNotPermitted(index, caseType, variableType), element);
                valid = false;
                continue;
            }

            // The type of the case is covered even if the rest of the case is not valid
            var previousIndex = caseTypes.putIfAbsent(caseElement, index);
            if (previousIndex != null) {
                messages.printError(ProtobufProcessorErrors.oneOfDuplicatedCaseType(caseType, previousIndex, index), element);
                valid = false;
                continue;
            }

            if (!checkOneOfCaseAnnotation(messageElement, element, field)) {
                valid = false;
                continue;
            }

            var owner = messageElement.indexOwner(index);
            if (owner.isPresent()) {
                messages.printError(ProtobufProcessorErrors.fieldDuplicatedIndex(element, owner.get().name(), index), element);
                valid = false;
                continue;
            }

            var caseName = field.name().isEmpty() ? ProtobufMessageElement.toSnakeCase(caseElement.getSimpleName().toString()) : field.name();
            if (!messageElement.isNameAllowed(caseName)) {
                messages.printError(ProtobufProcessorErrors.fieldReservedName(caseName), element);
                valid = false;
            }

            var nameOwner = messageElement.claimName(caseName, javaName);
            if (nameOwner.isPresent()) {
                messages.printError(ProtobufProcessorErrors.fieldDuplicatedName(element, nameOwner.get(), caseName), element);
                valid = false;
            }

            var type = getPropertyType(element, caseType, caseType, field, false);
            if (type.isEmpty()) {
                valid = false;
                continue;
            }

            if (!(type.get() instanceof ProtobufSimpleFieldType simpleType)) {
                messages.printError(ProtobufProcessorErrors.oneOfRepeatedCase(index, caseType), element);
                valid = false;
                continue;
            }

            if (!checkPropertyType(messageElement, element, caseType, field, simpleType)) {
                valid = false;
                continue;
            }

            resolvedCases.add(new ResolvedOneOfCase(field, caseName, caseType, simpleType));
        }

        var missingTypes = new ArrayList<TypeMirror>();
        for (var permittedType : permittedTypes) {
            if (!caseTypes.containsKey(permittedType)) {
                missingTypes.add(permittedType.asType());
            }
        }
        if (!missingTypes.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.oneOfMissingCases(variableType, missingTypes), element);
            valid = false;
        }

        if (!valid) {
            return Set.of();
        }

        resolvedCases = orderOneOfCases(resolvedCases);
        var oneOfType = new ProtobufOneOfFieldType(declaredType, resolvedCases.stream().map(ResolvedOneOfCase::type).toList());
        messageElement.addOneOf(
                element,
                accessor.get(),
                textName,
                oneOfType,
                resolvedCases.stream().map(ResolvedOneOfCase::field).toList(),
                resolvedCases.stream().map(ResolvedOneOfCase::name).toList()
        );

        var results = new HashSet<ProtobufTypeElement>();
        for (var resolvedCase : resolvedCases) {
            linkMixins(resolvedCase.type().mixins().all());
            results.addAll(linkType(resolvedCase.javaType()));
        }
        return results;
    }

    /**
     * Orders the cases of a oneof by index, except that a case precedes every case whose type
     * is one of its supertypes, so that a value is matched against its most specific case.
     *
     * <p>A case type can only be a supertype of another one if it is a sealed class that is
     * not abstract.
     *
     * @param cases the cases of the oneof
     * @return the ordered cases
     */
    private List<ResolvedOneOfCase> orderOneOfCases(List<ResolvedOneOfCase> cases) {
        var byIndex = new ArrayList<>(cases);
        byIndex.sort(Comparator.comparingInt(resolvedCase -> resolvedCase.field().index()));
        var result = new ArrayList<ResolvedOneOfCase>();
        for (var resolvedCase : byIndex) {
            var position = result.size();
            for (var i = 0; i < result.size(); i++) {
                if (protobufTypes.isAssignable(resolvedCase.javaType(), result.get(i).javaType())) {
                    position = i;
                    break;
                }
            }
            result.add(position, resolvedCase);
        }
        return result;
    }

    /**
     * A case of a oneof whose type is resolved.
     *
     * @param field the normalized annotation of the case
     * @param name the Protocol Buffers name of the case
     * @param javaType the permitted subtype that holds the case
     * @param type the resolved type of the case
     */
    private record ResolvedOneOfCase(ParsedProtobufField field, String name, TypeMirror javaType, ProtobufSimpleFieldType type) {

    }

    /**
     * Collects the types that a value of the given sealed type may have, which are the types
     * that it permits, expanding the permitted types that are sealed in turn.
     *
     * <p>A class that is not abstract is collected even if it is sealed, as a value may be an
     * instance of the class itself. An enum is never expanded, as the anonymous classes of
     * its constants are not types of their own.
     *
     * @param type the type to collect
     * @param result the collected types
     */
    private void collectOneOfTypes(TypeElement type, Set<TypeElement> result) {
        var sealed = type.getModifiers().contains(Modifier.SEALED) && type.getKind() != ElementKind.ENUM;
        var instantiable = type.getKind() != ElementKind.INTERFACE && !type.getModifiers().contains(Modifier.ABSTRACT);
        if (!sealed || instantiable) {
            result.add(type);
        }

        if (!sealed) {
            return;
        }

        for (var permitted : type.getPermittedSubclasses()) {
            if (permitted instanceof DeclaredType permittedType && permittedType.asElement() instanceof TypeElement permittedElement) {
                collectOneOfTypes(permittedElement, result);
            }
        }
    }

    /**
     * Checks the attributes of the annotation of a case of a oneof that do not depend on its
     * type, reporting an error for each one that is not valid.
     *
     * @param messageElement the enclosing protobuf message element
     * @param element the field or method annotated with the oneof
     * @param field the normalized annotation of the case
     * @return {@code true} if the case can be processed
     */
    private boolean checkOneOfCaseAnnotation(ProtobufMessageElement messageElement, Element element, ParsedProtobufField field) {
        var valid = true;
        var index = field.index();
        if (index < MIN_FIELD_INDEX || index > MAX_FIELD_INDEX) {
            messages.printError(ProtobufProcessorErrors.fieldIndexOutOfRange(index, MIN_FIELD_INDEX, MAX_FIELD_INDEX), element);
            valid = false;
        } else if (index >= MIN_IMPLEMENTATION_FIELD_INDEX && index <= MAX_IMPLEMENTATION_FIELD_INDEX) {
            messages.printError(ProtobufProcessorErrors.fieldIndexInImplementationRange(index, MIN_IMPLEMENTATION_FIELD_INDEX, MAX_IMPLEMENTATION_FIELD_INDEX), element);
            valid = false;
        } else if (!messageElement.isIndexAllowed(index)) {
            messages.printError(ProtobufProcessorErrors.fieldReservedIndex(index), element);
            valid = false;
        }

        if (!field.name().isEmpty() && !isValidFieldName(field.name())) {
            messages.printError(ProtobufProcessorErrors.fieldInvalidName(field.name()), element);
            valid = false;
        }

        return valid;
    }

    /**
     * Checks the attributes of a field annotation that do not depend on the resolved type
     * of the field, reporting an error for each one that is not valid.
     *
     * @param messageElement the enclosing protobuf message element
     * @param element the annotated field or method
     * @param propertyAnnotation the normalized field annotation
     * @return {@code true} if the field can be processed
     */
    private boolean checkPropertyAnnotation(ProtobufMessageElement messageElement, Element element, ParsedProtobufField propertyAnnotation) {
        var valid = true;
        var annotations = ParsedProtobufField.annotationNames(element);
        if (annotations.size() > 1) {
            messages.printError(ProtobufProcessorErrors.fieldConflictingAnnotations(annotations), element);
            valid = false;
        }

        var index = propertyAnnotation.index();
        if (index < MIN_FIELD_INDEX || index > MAX_FIELD_INDEX) {
            messages.printError(ProtobufProcessorErrors.fieldIndexOutOfRange(index, MIN_FIELD_INDEX, MAX_FIELD_INDEX), element);
            valid = false;
        } else if (index >= MIN_IMPLEMENTATION_FIELD_INDEX && index <= MAX_IMPLEMENTATION_FIELD_INDEX) {
            messages.printError(ProtobufProcessorErrors.fieldIndexInImplementationRange(index, MIN_IMPLEMENTATION_FIELD_INDEX, MAX_IMPLEMENTATION_FIELD_INDEX), element);
            valid = false;
        }

        if (!propertyAnnotation.name().isEmpty() && !isValidFieldName(propertyAnnotation.name())) {
            messages.printError(ProtobufProcessorErrors.fieldInvalidName(propertyAnnotation.name()), element);
            valid = false;
        }

        if (element instanceof ExecutableElement method
                && (method.getModifiers().contains(Modifier.PRIVATE) || method.getModifiers().contains(Modifier.STATIC) || !method.getParameters().isEmpty() || method.getReturnType().getKind() == TypeKind.VOID)) {
            messages.printError(ProtobufProcessorErrors.fieldSyntheticMethod(), element);
            valid = false;
        }

        var version = messageElement.version();
        switch (propertyAnnotation.fieldPresence()) {
            case LEGACY_REQUIRED -> {
                if (version == ProtobufVersion.PROTOBUF_3) {
                    messages.printError(ProtobufProcessorErrors.fieldRequiredNotSupported(version), element);
                    valid = false;
                }
            }
            case IMPLICIT -> {
                if (version == ProtobufVersion.PROTOBUF_2) {
                    messages.printError(ProtobufProcessorErrors.fieldImplicitNotSupported(version), element);
                    valid = false;
                } else if (propertyAnnotation instanceof ParsedProtobufField.Message) {
                    messages.printError(ProtobufProcessorErrors.fieldImplicitMessage(), element);
                    valid = false;
                }
            }
            case EXPLICIT, VERSION_DEFAULT -> {}
        }

        return valid;
    }

    /**
     * Checks the attributes of a field annotation that depend on the resolved type of the
     * field, reporting an error for each one that is not valid.
     *
     * <p>The presence of a field follows the rules of the official Protocol Buffers
     * implementation, and a field whose Java type cannot represent every state that its
     * presence allows is rejected:
     * <ul>
     * <li>a repeated or map field has no presence, so it cannot declare one;
     * <li>a required field must be able to be missing, so it cannot be primitive;
     * <li>a field with explicit presence must distinguish a field that is not set from a
     *     field set to its default value, so it cannot be primitive either;
     * <li>a field with implicit presence that is not on the wire holds the value with
     *     index {@code 0}, which a closed enum may not have.
     * </ul>
     *
     * @param messageElement the enclosing protobuf message element
     * @param element the annotated field or method
     * @param variableType the declared type of the field
     * @param propertyAnnotation the normalized field annotation
     * @param type the resolved type of the field
     * @return {@code true} if the field can be processed
     */
    private boolean checkPropertyType(ProtobufMessageElement messageElement, Element element, TypeMirror variableType, ParsedProtobufField propertyAnnotation, ProtobufFieldType type) {
        var valid = true;
        var repeated = type instanceof ProtobufCollectionFieldType || type instanceof ProtobufArrayFieldType;
        var declaredPresence = propertyAnnotation.fieldPresence();
        if (repeated || type instanceof ProtobufMapFieldType) {
            if (declaredPresence == ProtobufFieldPresence.LEGACY_REQUIRED) {
                messages.printError(ProtobufProcessorErrors.fieldRequiredRepeated(), element);
                valid = false;
            } else if (declaredPresence != ProtobufFieldPresence.VERSION_DEFAULT) {
                messages.printError(ProtobufProcessorErrors.fieldPresenceRepeated(declaredPresence), element);
                valid = false;
            }
        } else if (variableType instanceof PrimitiveType primitiveType && !(element instanceof ExecutableElement)) {
            // A synthetic field is only written, so it never needs to represent a field that is not set
            switch (declaredPresence.resolve(messageElement.version())) {
                case LEGACY_REQUIRED -> {
                    messages.printError(ProtobufProcessorErrors.fieldRequiredPrimitive(), element);
                    valid = false;
                }
                case EXPLICIT -> {
                    var boxedType = processingEnv.getTypeUtils().boxedClass(primitiveType).asType();
                    messages.printError(ProtobufProcessorErrors.fieldExplicitPrimitive(variableType, boxedType, messageElement.version()), element);
                    valid = false;
                }
                case IMPLICIT, VERSION_DEFAULT -> {}
            }
        }

        var enumField = propertyAnnotation instanceof ParsedProtobufField.Scalar scalar && scalar.type() == ProtobufType.ENUM;
        if (!repeated && enumField
                && declaredPresence.resolve(messageElement.version()) == ProtobufFieldPresence.IMPLICIT
                && isClosedEnum(variableType)) {
            messages.printError(ProtobufProcessorErrors.fieldImplicitClosedEnum(variableType), element);
            valid = false;
        }

        if (propertyAnnotation.packed() && !repeated) {
            messages.printError(ProtobufProcessorErrors.fieldPackedNotRepeated(), element);
            valid = false;
        }

        // A Java enum cannot hold the values of an open enum that have no constant
        var enumType = getEnumValueType(type);
        if (enumField && enumType instanceof DeclaredType declaredEnumType
                && declaredEnumType.asElement().getKind() == ElementKind.ENUM && isOpenEnum(enumType)) {
            messages.printWarning(ProtobufProcessorErrors.fieldOpenJavaEnum(enumType), element);
        }

        return valid;
    }

    /**
     * Returns whether a repeated field with the given annotation is packed, once its encoding is
     * resolved against the version of its message.
     *
     * @param messageElement the enclosing protobuf message element
     * @param propertyAnnotation the normalized field annotation
     * @return {@code true} if the field is a packable scalar whose resolved encoding is packed
     */
    private static boolean isPacked(ProtobufMessageElement messageElement, ParsedProtobufField propertyAnnotation) {
        return propertyAnnotation instanceof ParsedProtobufField.Scalar scalar
                && scalar.type().isPackable()
                && scalar.repeatedFieldEncoding().resolve(messageElement.version()) == ProtobufRepeatedFieldEncoding.PACKED;
    }

    /**
     * Returns the type of the values of the given field type, which is the type of the
     * elements of a repeated field.
     *
     * @param type the field type
     * @return the type of the values
     */
    private static TypeMirror getEnumValueType(ProtobufFieldType type) {
        return switch (type) {
            case ProtobufCollectionFieldType collectionType -> collectionType.valueType().descriptorElementType();
            case ProtobufArrayFieldType arrayType -> arrayType.valueType().descriptorElementType();
            case ProtobufMapFieldType mapType -> mapType.valueType().descriptorElementType();
            case ProtobufSimpleFieldType simpleType -> simpleType.descriptorElementType();
            case ProtobufOneOfFieldType oneOfType -> oneOfType.descriptorElementType();
        };
    }

    /**
     * Returns whether the given type is an enum annotated with {@link ProtobufEnum} that is
     * open, once its type is resolved against its version.
     *
     * @param type the type to check
     * @return {@code true} if the type is an open protobuf enum
     */
    private boolean isOpenEnum(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return false;
        }

        var annotation = declaredType.asElement().getAnnotation(ProtobufEnum.class);
        return annotation != null && annotation.type().resolve(annotation.version()) == ProtobufEnumType.OPEN;
    }

    /**
     * Returns whether the given type is an enum annotated with {@link ProtobufEnum} that is
     * closed, once its type is resolved against its version.
     *
     * @param type the type to check
     * @return {@code true} if the type is a closed protobuf enum
     */
    private boolean isClosedEnum(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return false;
        }

        var annotation = declaredType.asElement().getAnnotation(ProtobufEnum.class);
        return annotation != null && annotation.type().resolve(annotation.version()) == ProtobufEnumType.CLOSED;
    }

    /**
     * Returns whether the given name is a valid Protocol Buffers identifier.
     *
     * @param name the name to check
     * @return {@code true} if the name starts with a letter or an underscore, followed by
     *         letters, digits or underscores
     */
    private static boolean isValidFieldName(String name) {
        for (var i = 0; i < name.length(); i++) {
            var character = name.charAt(i);
            var letter = (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || character == '_';
            var digit = character >= '0' && character <= '9';
            if (!letter && (i == 0 || !digit)) {
                return false;
            }
        }
        return !name.isEmpty();
    }

    /**
     * Links the given type into the converter graph, processing its serializers,
     * deserializers, and type arguments recursively.
     *
     * @param variableType the type to link
     * @return the set of protobuf objects discovered during linking
     */
    private Set<ProtobufTypeElement> linkType(TypeMirror variableType) {
        // The elements of an array field are linked like the ones of a collection
        if (variableType instanceof ArrayType arrayType) {
            return linkType(arrayType.getComponentType());
        }

        if (protobufTypes.isMessage(variableType)) {
            linkMessage(variableType);
            return Set.of();
        }

        if (protobufTypes.isEnum(variableType)) {
            linkEnum(variableType);
            return Set.of();
        }

        if (!(variableType instanceof DeclaredType declaredType) || !(declaredType.asElement() instanceof TypeElement typeElement)) {
            return Set.of();
        }

        for (var typeArgument : declaredType.getTypeArguments()) {
            linkType(typeArgument);
        }

        for (var entry : typeElement.getTypeParameters()) {
            if (entry.asType().getKind() != TypeKind.TYPEVAR) {
                linkType(entry.asType());
            }
        }

        Set<ProtobufTypeElement> results = new HashSet<>();
        var name = typeElement.getQualifiedName().toString();
        if (linkedTypes.add(name)) {
            for (var entry : typeElement.getEnclosedElements()) {
                if (entry instanceof ExecutableElement element) {
                    processObjectSerializer(element);
                    processObjectSizer(element);
                    processObjectDeserializer(element);
                }
            }
        }
        return results;
    }

    /**
     * Links the encode and decode converters for a protobuf enum type. Enums are
     * encoded as varint integers and use the built-in
     * {@link com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator}
     * for size calculation, so no sizer is registered.
     *
     * @param type the enum type
     */
    private void linkEnum(TypeMirror type) {
        var specName = DaedalusMethodGenerator.getSpecByType(type);
        var specInstance = ProtobufMessageSpecGenerator.getInstanceExpression(specName);
        var targetType = protobufTypes.getType(int.class);
        var serializer = DaedalusMethodElement.ofMixinInstance(specName, specInstance, targetType, ProtobufEnumSerializationGenerator.METHOD_NAME, type);
        converterGraph.linkSerializer(type, targetType, serializer);
        var deserializer = DaedalusMethodElement.ofMixinInstance(specName, specInstance, type, ProtobufEnumDeserializationGenerator.METHOD_NAME, targetType);
        converterGraph.linkDeserializer(targetType, type, deserializer);
    }

    /**
     * Links the encode and decode converters for a protobuf message type, including
     * recursive linking of its type arguments. The converters are instance methods of the
     * Spec of the message, invoked on the instance returned by its {@code global()} method,
     * and the synthetic {@code sizeOfBinary} method is registered as the sizer for the
     * message type so that length prefixes can be written before the message body.
     *
     * @param type the message type
     */
    private void linkMessage(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            for (var typeArgument : declaredType.getTypeArguments()) {
                linkType(typeArgument);
            }
        }
        var specName = DaedalusMethodGenerator.getSpecByType(type);
        var specInstance = ProtobufMessageSpecGenerator.getInstanceExpression(specName);

        // public void encodeBinary(<type>, ProtobufBinaryWriter)
        var serializer = DaedalusMethodElement.ofMixinInstance(
                specName,
                specInstance,
                protobufTypes.getType(void.class),
                ProtobufSerializationGenerator.METHOD_NAME,
                type,
                protobufTypes.getType(ProtobufBinaryWriter.class)
        );
        converterGraph.linkSerializer(
                type,
                protobufTypes.getType(void.class),
                serializer
        );

        // public int sizeOfBinary(<type>)
        var sizer = DaedalusMethodElement.ofMixinInstance(
                specName,
                specInstance,
                protobufTypes.getType(int.class),
                ProtobufSizeGenerator.METHOD_NAME,
                type
        );
        converterGraph.attachSizer(type, sizer);

        // public <type> decodeBinary(ProtobufBinaryReader)
        var deserializer = DaedalusMethodElement.ofMixinInstance(
                specName,
                specInstance,
                type,
                ProtobufDeserializationGenerator.METHOD_NAME,
                protobufTypes.getType(ProtobufBinaryReader.class)
        );
        converterGraph.linkDeserializer(
                protobufTypes.getType(ProtobufBinaryReader.class),
                type,
                deserializer
        );
    }

    /**
     * Links all serializer and deserializer methods from the given mixin types
     * into the converter graphs, along with the types they convert to and from.
     *
     * <p>A mixin can convert to a type that is not processed in this compilation,
     * such as a message declared in a dependency. Linking that type registers its
     * own converters, like the methods of its spec, so that a conversion path can
     * continue past the mixin.
     *
     * <p>Each mixin is linked once, as the converter graph is shared by every field.
     *
     * @param mixins the mixin type elements
     */
    private void linkMixins(Set<TypeElement> mixins) {
        for (var mixin : mixins) {
            if (!linkedMixins.add(mixin)) {
                continue;
            }

            for (var element : mixin.getEnclosedElements()) {
                if (element instanceof ExecutableElement method) {
                    processObjectSerializer(method);
                    processObjectSizer(method);
                    processObjectDeserializer(method);
                    linkConverterTypes(method);
                }
            }
        }
    }

    /**
     * Links the type on the other side of the given converter method: the
     * return type of a {@link TypeSerializer}, or the first parameter type of a
     * {@link TypeDeserializer}.
     *
     * <p>Methods that are not converters are ignored.
     *
     * @param method the method to inspect
     */
    private void linkConverterTypes(ExecutableElement method) {
        if (method.getAnnotation(TypeSerializer.class) != null) {
            linkType(method.getReturnType());
        }

        if (method.getAnnotation(TypeDeserializer.class) != null && !method.getParameters().isEmpty()) {
            linkType(method.getParameters().getFirst().asType());
        }
    }

    /**
     * Returns the type of the given accessor element.
     *
     * @param accessor the accessor element (field or method)
     * @return the type of the accessor
     */
    private TypeMirror getAccessorType(Element accessor) {
        return switch (accessor) {
            case VariableElement element -> element.asType();
            case ExecutableElement element -> element.getReturnType();
            default -> throw new IllegalStateException("Unexpected value: " + accessor);
        };
    }

    /**
     * Returns the default value for a collection type by first looking for a default
     * constructor and then falling back to annotation based default values.
     *
     * @param invoker the element used for error reporting
     * @param collectionType the collection type
     * @param mixins the mixin type elements
     * @return the default value expression, if found
     */
    private Optional<String> getCollectionDefaultValue(Element invoker, TypeMirror collectionType, Set<TypeElement> mixins) {
        return protobufTypes.getDefaultConstructor(collectionType)
                .map(typeElement -> "new %s()".formatted(typeElement.getQualifiedName()))
                .or(() -> getDefaultValue(invoker, collectionType, mixins));
    }

    /**
     * Returns the default value for the given type by searching the type itself,
     * its mixins, and falling back to primitive defaults.
     *
     * @param invoker the element used for error reporting
     * @param type the type for which to find a default value
     * @param mixins the mixin type elements
     * @return the default value expression, if found
     */
    private Optional<String> getDefaultValue(Element invoker, TypeMirror type, Set<TypeElement> mixins) {
        if (type instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement classType) {
            var selfDefaultValue = getDefaultValueFromAnnotation(invoker, type, classType);
            if (selfDefaultValue.isPresent()) {
                return selfDefaultValue;
            }
        }

        for (var mixin : mixins) {
            var mixinDefaultValue = getDefaultValueFromAnnotation(invoker, type, mixin);
            if (mixinDefaultValue.isPresent()) {
                return mixinDefaultValue;
            }
        }

        return switch (type.getKind()) {
            case INT, CHAR, SHORT, BYTE -> Optional.of("0");
            case BOOLEAN -> Optional.of("false");
            case FLOAT -> Optional.of("0f");
            case DOUBLE -> Optional.of("0d");
            case LONG -> Optional.of("0l");
            default -> Optional.empty();
        };
    }

    /**
     * Returns the default value for the given type by searching a provider type
     * for elements annotated with {@link TypeDefaultValue}.
     *
     * @param invoker the element used for error reporting
     * @param type the type for which to find a default value
     * @param provider the type element to search for default value providers
     * @return the default value expression, if found
     */
    private Optional<String> getDefaultValueFromAnnotation(Element invoker, TypeMirror type, TypeElement provider) {
        List<Element> candidates = new ArrayList<>();
        for (var element : provider.getEnclosedElements()) {
            var annotation = element.getAnnotation(TypeDefaultValue.class);
            if (annotation != null && protobufTypes.isAssignable(getDefaultValueType(element), type)) {
                candidates.add(element);
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        var bestMatch = candidates.getFirst();
        var bestMatchType = getDefaultValueType(bestMatch);
        for (var i = 1; i < candidates.size(); i++) {
            var candidate = candidates.get(i);
            var candidateType = getDefaultValueType(candidate);
            if (protobufTypes.isSameType(bestMatchType, candidateType)) {
                messages.printError(ProtobufProcessorErrors.defaultValueDuplicated(candidate, bestMatch, bestMatchType, invoker), invoker);
            }

            if (protobufTypes.isAssignable(bestMatchType, candidateType)) {
                bestMatch = candidate;
                bestMatchType = candidateType;
            }
        }

        return Optional.ofNullable(getDefaultValueCaller(bestMatch));
    }

    /**
     * Returns the type of a default value provider element.
     *
     * @param bestMatch the element to inspect
     * @return the return type (for methods) or field type (for variables), or {@code null}
     */
    private TypeMirror getDefaultValueType(Element bestMatch) {
        return switch (bestMatch) {
            case ExecutableElement executableElement -> executableElement.getReturnType();
            case VariableElement variableElement -> variableElement.asType();
            default -> null;
        };
    }

    /**
     * Returns the fully qualified call expression for the given default value provider element.
     *
     * @param bestMatch the element to inspect
     * @return the call expression, or {@code null}
     */
    private String getDefaultValueCaller(Element bestMatch) {
        var bestMatchOwner = (TypeElement) bestMatch.getEnclosingElement();
        return switch (bestMatch) {
            case ExecutableElement executableElement -> bestMatchOwner.getQualifiedName() + "." + executableElement.getSimpleName() + "()";
            case VariableElement variableElement -> bestMatchOwner.getQualifiedName() + "." + variableElement.getSimpleName();
            default -> null;
        };
    }

    /**
     * Determines the protobuf property type for the given element, handling
     * normal types, collections, and maps.
     *
     * @param invoker the element used for error reporting
     * @param elementType the type of the field element
     * @param accessorType the type of the accessor
     * @param property the property annotation
     * @return the resolved property type, if valid
     */
    private Optional<? extends ProtobufFieldType> getPropertyType(Element invoker, TypeMirror elementType, TypeMirror accessorType, ParsedProtobufField property, boolean packed) {
        var explicitMixins = protobufTypes.getMirroredTypes(property.mixins());
        var enclosingType = protobufTypes.getEnclosingTypeElement(invoker);
        var mixins = mixinScopeResolver.getApplicableMixins(enclosingType, explicitMixins);
        if (!(property instanceof ParsedProtobufField.Value valueProperty)) {
            return getConcreteMapType((ParsedProtobufField.Map) property, invoker, elementType, mixins);
        }

        // A single bytes field is held by a byte[], so every other array is a repeated field
        if (elementType instanceof ArrayType arrayType && (valueProperty.type() != ProtobufType.BYTES || arrayType.getComponentType().getKind() != TypeKind.BYTE)) {
            return getConcreteArrayType(invoker, valueProperty, arrayType, mixins, packed);
        }

        if (protobufTypes.isAssignable(elementType, Collection.class)) {
            return getConcreteCollectionType(invoker, valueProperty, elementType, mixins, packed);
        }

        var defaultValue = getDefaultValue(invoker, elementType, mixins.all())
                .orElse("null");
        var implementation = new ProtobufSimpleFieldType(
                valueProperty.type(),
                elementType,
                accessorType,
                defaultValue,
                mixins
        );
        createUnattributedSerializer(invoker, implementation);
        createUnattributedDeserializer(invoker, implementation);
        return Optional.of(implementation);
    }

    /**
     * Resolves the concrete collection type for a property, including its type
     * parameter and default value.
     *
     * @param invoker the element used for error reporting
     * @param property the property annotation
     * @param elementType the type of the field element
     * @param mixins the mixins that apply to the field
     * @param packed whether the field is packed, so that it is a candidate for the bulk path
     * @return the resolved collection property type, if valid
     */
    private Optional<? extends ProtobufFieldType> getConcreteCollectionType(Element invoker, ParsedProtobufField.Value property, TypeMirror elementType, DaedalusMixins mixins, boolean packed) {
        var collectionTypeParameter = protobufTypes.getTypeParameter(elementType, protobufTypes.getType(Collection.class), 0);
        if (collectionTypeParameter.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.fieldCollectionTypeUnknown(invoker, elementType), invoker);
            return Optional.empty();
        }

        var collectionDefaultValue = getCollectionDefaultValue(invoker, elementType, mixins.all());
        if (collectionDefaultValue.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.fieldCollectionDefaultUnknown(invoker, elementType), invoker);
            return Optional.empty();
        }

        var collectionTypeParameterType = new ProtobufSimpleFieldType(
                property.type(),
                collectionTypeParameter.get(),
                collectionTypeParameter.get(),
                null,
                mixins
        );
        createUnattributedSerializer(invoker, collectionTypeParameterType);
        createUnattributedDeserializer(invoker, collectionTypeParameterType);

        var type = new ProtobufCollectionFieldType(
                elementType,
                collectionTypeParameterType,
                collectionDefaultValue.get(),
                packed,
                mixins
        );
        if (packed) {
            createBulkConverters(invoker, elementType, type);
        }
        return Optional.of(type);
    }

    /**
     * Resolves the type of a repeated field held by an array.
     *
     * <p>Like the elements of a collection, the elements of the array are converted from the
     * component type of the array to the wire type of the field.
     *
     * @param invoker the element used for error reporting
     * @param property the normalized field annotation
     * @param arrayType the array type of the field
     * @param mixins the mixins that apply to the field
     * @param packed whether the field is packed, so that it is a candidate for the bulk path
     * @return the resolved array field type, if valid
     */
    private Optional<ProtobufArrayFieldType> getConcreteArrayType(Element invoker, ParsedProtobufField.Value property, ArrayType arrayType, DaedalusMixins mixins, boolean packed) {
        var componentType = arrayType.getComponentType();
        if (!isReifiable(componentType)) {
            messages.printError(ProtobufProcessorErrors.fieldArrayNotReifiable(arrayType), invoker);
            return Optional.empty();
        }

        var valueType = new ProtobufSimpleFieldType(property.type(), componentType, componentType, null, mixins);
        createUnattributedSerializer(invoker, valueType);
        createUnattributedDeserializer(invoker, valueType);
        var type = new ProtobufArrayFieldType(arrayType, valueType, packed, mixins);
        if (packed) {
            createBulkConverters(invoker, arrayType, type);
        }
        return Optional.of(type);
    }

    /**
     * Creates the optional converters of a packed field from its type to the array of the
     * primitive type of its wire values, and from that array to its type.
     *
     * <p>If both converters are resolved, the field is written and read in bulk; otherwise,
     * its elements are converted one by one. No converter is needed if the type of the field
     * is already that array, and no deserializer is needed by a synthetic field, which is
     * only written.
     *
     * @param invoker the element that declares the field
     * @param fieldType the type of the field
     * @param type the type of the field to add the converters to
     */
    private void createBulkConverters(Element invoker, TypeMirror fieldType, ProtobufValueFieldType type) {
        var componentKind = switch (type.protobufType()) {
            case FLOAT -> TypeKind.FLOAT;
            case DOUBLE -> TypeKind.DOUBLE;
            case BOOL -> TypeKind.BOOLEAN;
            case ENUM, INT32, SINT32, UINT32, FIXED32, SFIXED32 -> TypeKind.INT;
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> TypeKind.LONG;
            default -> throw new IllegalStateException("Internal bug: %s fields cannot be packed".formatted(type.protobufType()));
        };
        var typeUtils = processingEnv.getTypeUtils();
        var bulkType = typeUtils.getArrayType(typeUtils.getPrimitiveType(componentKind));
        if (!protobufTypes.isAssignable(fieldType, bulkType)) {
            type.addConverter(new DaedalusConverterElement.Unattributed(invoker, fieldType, bulkType, type.mixins(), DaedalusConverterElement.Unattributed.Type.SERIALIZER, true));
        }
        if (!(invoker instanceof ExecutableElement) && !protobufTypes.isAssignable(bulkType, fieldType)) {
            type.addConverter(new DaedalusConverterElement.Unattributed(invoker, bulkType, fieldType, type.mixins(), DaedalusConverterElement.Unattributed.Type.DESERIALIZER, true));
        }
    }

    /**
     * Returns whether the given type is reifiable, so that an array of it can be created.
     *
     * @param type the type to check
     * @return {@code true} if the type is a primitive, a declared type without type arguments,
     *         or an array of a reifiable type
     */
    private static boolean isReifiable(TypeMirror type) {
        return switch (type) {
            case ArrayType arrayType -> isReifiable(arrayType.getComponentType());
            case DeclaredType declaredType -> declaredType.getTypeArguments().isEmpty();
            default -> type.getKind().isPrimitive();
        };
    }

    /**
     * Finds the accessor for a property field, either the field itself if it has
     * sufficient visibility, or a matching getter method.
     *
     * @param fieldElement the field element
     * @param indexes the indexes of the field, which are the indexes of the cases of a oneof
     * @return the accessor element, if found
     */
    private Optional<? extends Element> getAccessor(VariableElement fieldElement, Set<Integer> indexes) {
        if (!fieldElement.getModifiers().contains(Modifier.PRIVATE)) {
            return Optional.of(fieldElement);
        }

        var methods = fieldElement.getEnclosingElement()
                .getEnclosedElements()
                .stream()
                .filter(entry -> entry instanceof ExecutableElement)
                .map(entry -> (ExecutableElement) entry)
                .filter(element -> !element.getModifiers().contains(Modifier.PRIVATE))
                .toList();
        return methods.stream()
                .filter(entry -> isProtobufGetter(entry, indexes))
                .findFirst()
                .or(() -> inferAccessor(fieldElement, methods));
    }

    /**
     * Infers a getter method for the given field by matching method names.
     *
     * @param fieldElement the field element
     * @param methods the candidate methods
     * @return the inferred getter method, if found
     */
    private Optional<ExecutableElement> inferAccessor(VariableElement fieldElement, List<ExecutableElement> methods) {
        var fieldName = fieldElement.getSimpleName().toString();
        return methods.stream()
                .filter(entry -> isProtobufGetter(entry, fieldName))
                .findFirst();
    }

    /**
     * Returns whether the given method matches as a getter for the field name.
     *
     * @param entry the method element
     * @param fieldName the field name
     * @return {@code true} if the method is a matching getter
     */
    private boolean isProtobufGetter(ExecutableElement entry, String fieldName) {
        var methodName = entry.getSimpleName().toString();
        return entry.getParameters().isEmpty() && (methodName.equalsIgnoreCase("get" + fieldName) || methodName.equalsIgnoreCase(fieldName));
    }

    /**
     * Returns whether the given method is annotated with {@link ProtobufAccessor}
     * and matches one of the given indexes.
     *
     * @param entry the method element
     * @param indexes the indexes of the field
     * @return {@code true} if the method is a matching getter
     */
    private boolean isProtobufGetter(ExecutableElement entry, Set<Integer> indexes) {
        var annotation = entry.getAnnotation(ProtobufAccessor.class);
        return annotation != null && indexes.contains(annotation.index());
    }

    /**
     * Resolves the concrete map type for a property, including its key type,
     * value type, and default value.
     *
     * @param invoker the element used for error reporting
     * @param elementType the type of the field element
     * @param mixins the mixins that apply to the field
     * @return the resolved map property type, if valid
     */
    private Optional<ProtobufMapFieldType> getConcreteMapType(ParsedProtobufField.Map mapField, Element invoker, TypeMirror elementType, DaedalusMixins mixins) {
        var mapType = protobufTypes.getType(Map.class);
        var mapKeyProtobufType = mapField.mapKeyType();
        var mapValueProtobufType = mapField.mapValueType();

        if (!protobufTypes.isAssignable(elementType, Map.class)) {
            messages.printError(ProtobufProcessorErrors.fieldMapNotMap(elementType), invoker);
            return Optional.empty();
        }

        // Only integral, bool and string keys are allowed, as floating point and bytes keys are not comparable
        switch (mapKeyProtobufType) {
            case INT32, SINT32, UINT32, FIXED32, SFIXED32, INT64, SINT64, UINT64, FIXED64, SFIXED64, BOOL, STRING -> {}
            default -> {
                messages.printError(ProtobufProcessorErrors.fieldInvalidMapKeyType(mapKeyProtobufType), invoker);
                return Optional.empty();
            }
        }

        var keyTypeParameter = protobufTypes.getTypeParameter(elementType, mapType, 0)
                .orElse(null);
        if (keyTypeParameter == null) {
            messages.printError(ProtobufProcessorErrors.fieldMapKeyTypeUnknown(invoker, elementType), invoker);
            return Optional.empty();
        }

        var keyEntry = new ProtobufSimpleFieldType(
                mapKeyProtobufType,
                keyTypeParameter,
                keyTypeParameter,
                null,
                mixins
        );
        createUnattributedSerializer(invoker, keyEntry);
        createUnattributedDeserializer(invoker, keyEntry);

        var valueTypeParameter = protobufTypes.getTypeParameter(elementType, mapType, 1)
                .orElse(null);
        if (valueTypeParameter == null) {
            messages.printError(ProtobufProcessorErrors.fieldMapValueTypeUnknown(invoker, elementType), invoker);
            return Optional.empty();
        }

        var valueDefaultValue = getDefaultValue(invoker, valueTypeParameter, mixins.all())
                .orElse("null");
        var valueEntry = new ProtobufSimpleFieldType(
                mapValueProtobufType,
                valueTypeParameter,
                valueTypeParameter,
                valueDefaultValue,
                mixins
        );
        createUnattributedSerializer(invoker, valueEntry);
        createUnattributedDeserializer(invoker, valueEntry);

        var mapDefaultValue = getCollectionDefaultValue(invoker, elementType, mixins.all());
        if (mapDefaultValue.isEmpty()) {
            messages.printError(ProtobufProcessorErrors.fieldMapDefaultUnknown(invoker, elementType), invoker);
            return Optional.empty();
        }

        return Optional.of(new ProtobufMapFieldType(
                elementType,
                keyEntry,
                valueEntry,
                mapDefaultValue.get(),
                mixins
        ));
    }

    /**
     * Creates an unattributed serializer converter element for the given property type,
     * if a conversion from the accessor type to the protobuf wire type is needed.
     *
     * @param invoker the element used for error reporting
     * @param implementation the property type
     */
    private void createUnattributedSerializer(Element invoker, ProtobufValueFieldType implementation) {
        createUnattributedSerializer(invoker, implementation.accessorType(), implementation);
    }

    /**
     * Creates an unattributed serializer converter element from the given source type
     * to the protobuf wire type of the property, if a conversion is needed.
     *
     * @param invoker the element used for error reporting
     * @param from the source type
     * @param implementation the property type
     */
    private void createUnattributedSerializer(Element invoker, TypeMirror from, ProtobufValueFieldType implementation) {
        var to = switch (implementation.protobufType()) {
            case FLOAT -> protobufTypes.getType(float.class);
            case DOUBLE -> protobufTypes.getType(double.class);
            case BOOL -> protobufTypes.getType(boolean.class);
            case STRING -> protobufTypes.getType(String.class);
            case BYTES -> protobufTypes.getType(byte[].class);
            case INT32, SINT32, UINT32, FIXED32, SFIXED32, ENUM -> protobufTypes.getType(int.class);
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> protobufTypes.getType(long.class);
            case MESSAGE -> protobufTypes.getType(void.class);
        };
        if (!protobufTypes.isAssignable(from, to)) {
            var unattributed = new DaedalusConverterElement.Unattributed(invoker, from, to, implementation.mixins(), DaedalusConverterElement.Unattributed.Type.SERIALIZER);
            implementation.addConverter(unattributed);
        }
    }

    /**
     * Creates an unattributed deserializer converter element for the given property type,
     * if a conversion from the protobuf wire type to the descriptor element type is needed.
     *
     * <p>A synthetic field, declared by a method, is only serialized, so no deserializer
     * is created for it.
     *
     * @param invoker the element used for error reporting, which declares the field
     * @param implementation the property type
     */
    private void createUnattributedDeserializer(Element invoker, ProtobufValueFieldType implementation) {
        if (invoker instanceof ExecutableElement) {
            return;
        }

        createUnattributedDeserializer(invoker, implementation.descriptorElementType(), implementation);
    }

    /**
     * Creates an unattributed deserializer converter element from the protobuf wire type
     * to the given target type, if a conversion is needed.
     *
     * @param invoker the element used for error reporting
     * @param to the target type
     * @param implementation the property type
     */
    private void createUnattributedDeserializer(Element invoker, TypeMirror to, ProtobufValueFieldType implementation) {
        var from = switch (implementation.protobufType()) {
            case FLOAT -> protobufTypes.getType(float.class);
            case DOUBLE -> protobufTypes.getType(double.class);
            case BOOL -> protobufTypes.getType(boolean.class);
            case STRING -> protobufTypes.getType(String.class);
            case BYTES -> protobufTypes.getType(byte[].class);
            case INT32, SINT32, UINT32, FIXED32, SFIXED32, ENUM -> protobufTypes.getType(int.class);
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> protobufTypes.getType(long.class);
            case MESSAGE -> protobufTypes.getType(ProtobufBinaryReader.class);
        };
        // A deserializer is only needed if the wire type cannot be assigned to the field, like a String to a CharSequence
        if (!protobufTypes.isAssignable(from, to)) {
            var unattributed = new DaedalusConverterElement.Unattributed(invoker, from, to, implementation.mixins(), DaedalusConverterElement.Unattributed.Type.DESERIALIZER);
            implementation.addConverter(unattributed);
        }
    }

    /**
     * Processes an enum annotated with {@link ProtobufEnum}, creating its element,
     * linking it into the converter graph, and processing its constants.
     *
     * @param enumTypeElement the enum type element
     * @return the set of processed protobuf objects
     */
    private Set<ProtobufTypeElement> processEnum(TypeElement enumTypeElement) {
        var enumElement = createEnumElement(enumTypeElement);
        if (enumElement.isEmpty()) {
            return Set.of();
        }
        linkEnum(enumTypeElement.asType());
        return Set.of(enumElement.get());
    }

    /**
     * Returns whether the given field is a constant of the given protobuf enum.
     *
     * <p>The constants of a Java enum are its enum constants. The constants of any other
     * type are its {@code static final} fields annotated with {@link ProtobufEnum.Constant}.
     *
     * @param enumTypeElement the protobuf enum type element
     * @param variableElement the field to check
     * @return {@code true} if the field is a constant of the enum
     */
    private boolean isEnumConstant(TypeElement enumTypeElement, VariableElement variableElement) {
        if (enumTypeElement.getKind() == ElementKind.ENUM) {
            return variableElement.getKind() == ElementKind.ENUM_CONSTANT;
        }

        if (variableElement.getAnnotation(ProtobufEnum.Constant.class) == null) {
            return false;
        }

        var modifiers = variableElement.getModifiers();
        var constant = enumTypeElement.getKind() == ElementKind.INTERFACE
                || (modifiers.contains(Modifier.STATIC) && modifiers.contains(Modifier.FINAL));
        if (!constant || !protobufTypes.isAssignable(variableElement.asType(), enumTypeElement.asType())) {
            messages.printError(ProtobufProcessorErrors.enumIllegalConstant(variableElement.getSimpleName().toString(), enumTypeElement.getSimpleName().toString()), variableElement);
            return false;
        }

        return true;
    }

    /**
     * Finds the methods that convert the values of the given protobuf enum to and from their
     * index, if the type declares them instead of constants.
     *
     * <p>The serializer is an instance method annotated with {@link TypeSerializer} that takes
     * no parameters and returns an {@code int}, and the deserializer is a static method
     * annotated with {@link TypeDeserializer} that takes an {@code int} and returns the type.
     *
     * @param enumElement the protobuf enum element
     * @return {@code true} if the type declares both methods
     */
    private boolean processEnumConverters(ProtobufEnumElement enumElement) {
        ExecutableElement serializer = null;
        ExecutableElement deserializer = null;
        var intType = protobufTypes.getType(int.class);
        var enumType = enumElement.typeElement().asType();
        for (var entry : enumElement.typeElement().getEnclosedElements()) {
            if (!(entry instanceof ExecutableElement method) || method.getKind() != ElementKind.METHOD) {
                continue;
            }

            if (method.getAnnotation(TypeSerializer.class) != null) {
                if (method.getModifiers().contains(Modifier.STATIC) || !method.getParameters().isEmpty() || !protobufTypes.isAssignable(method.getReturnType(), intType)) {
                    messages.printError(ProtobufProcessorErrors.enumIllegalSerializer(method.getSimpleName().toString()), method);
                } else {
                    serializer = method;
                }
            }

            if (method.getAnnotation(TypeDeserializer.class) != null) {
                if (!method.getModifiers().contains(Modifier.STATIC) || method.getParameters().size() != 1 || !protobufTypes.isAssignable(intType, method.getParameters().getFirst().asType()) || !protobufTypes.isAssignable(method.getReturnType(), enumType)) {
                    messages.printError(ProtobufProcessorErrors.enumIllegalDeserializer(method.getSimpleName().toString(), enumElement.typeElement().getSimpleName().toString()), method);
                } else {
                    deserializer = method;
                }
            }
        }

        if (serializer == null && deserializer == null) {
            return false;
        }

        if (serializer == null || deserializer == null) {
            messages.printError(ProtobufProcessorErrors.enumMissingConverter(enumElement.typeElement().getSimpleName().toString()), serializer != null ? serializer : deserializer);
            return false;
        }

        enumElement.setConverters(serializer, deserializer);
        return true;
    }

    /**
     * Creates a protobuf enum element from the given enum type, resolving
     * its metadata and reserved elements.
     *
     * @param enumTypeElement the enum type element
     * @return the protobuf enum element, if valid
     */
    private Optional<ProtobufEnumElement> createEnumElement(TypeElement enumTypeElement) {
        var enumAnnotation = enumTypeElement.getAnnotation(ProtobufEnum.class);
        if (enumAnnotation == null) {
            return Optional.empty();
        }

        var reserved = ProtobufReservedFieldElement.of(enumAnnotation);
        var version = enumAnnotation.version();
        var enumType = enumAnnotation.type();
        var enumElement = new ProtobufEnumElement(enumTypeElement, version, enumType, reserved);

        var conflicts = new TreeMap<Integer, SequencedCollection<String>>();
        var textNames = new HashMap<String, String>();
        var javaEnum = enumTypeElement.getKind() == ElementKind.ENUM;
        for (var entry : enumTypeElement.getEnclosedElements()) {
            if (!(entry instanceof VariableElement variableElement) || !isEnumConstant(enumTypeElement, variableElement)) {
                continue;
            }

            var name = variableElement.getSimpleName().toString();
            var annotation = variableElement.getAnnotation(ProtobufEnum.Constant.class);
            var index = annotation == null
                    ? enumElement.fields().size()
                    : annotation.index();
            if (!enumElement.isIndexAllowed(index)) {
                messages.printError(ProtobufProcessorErrors.fieldReservedIndex(index), variableElement);
            }

            var textName = annotation == null || annotation.name().isEmpty() ? name : annotation.name();
            if (annotation != null && !annotation.name().isEmpty() && !isValidFieldName(textName)) {
                messages.printError(ProtobufProcessorErrors.fieldInvalidName(textName), variableElement);
            } else if(!enumElement.isNameAllowed(textName)) {
                messages.printError(ProtobufProcessorErrors.fieldReservedName(textName), variableElement);
            }

            var nameOwner = textNames.putIfAbsent(textName, name);
            if (nameOwner != null) {
                messages.printError(ProtobufProcessorErrors.enumDuplicatedName(variableElement, nameOwner, textName), variableElement);
            }

            // The first constant of an open enum is its default value, which must be encoded as zero
            if (enumElement.fields().isEmpty() && enumElement.enumType() == ProtobufEnumType.OPEN && index != 0) {
                messages.printError(ProtobufProcessorErrors.enumOpenWithoutZero(name, index), variableElement);
            }

            var type = variableElement.asType();
            var mixins = annotation != null
                    ? protobufTypes.getMirroredTypes(annotation::mixins)
                    : Set.<TypeElement>of();
            var defaultValue = getDefaultValue(variableElement, type, mixins)
                    .orElse("null");
            var constantType = new ProtobufSimpleFieldType(ProtobufType.ENUM, type, type, defaultValue, DaedalusMixins.of(mixins));
            var constant = new ProtobufEnumConstantElement(index, name, textName, variableElement, constantType);
            var existingConstant = enumElement.addField(constant);
            if (existingConstant.isEmpty()) {
                continue;
            }

            conflicts.compute(index, (_, names) -> {
                if(names == null) {
                    names = new LinkedList<>();
                    names.add(existingConstant.get().name());
                }
                names.add(name);
                return names;
            });
        }

        if (!processEnumConverters(enumElement) && !javaEnum && enumElement.fields().isEmpty()) {
            messages.printError(ProtobufProcessorErrors.enumWithoutValues(enumTypeElement.getSimpleName().toString()), enumTypeElement);
        }

        conflicts.forEach((index, names) -> messages.printError(ProtobufProcessorErrors.enumDuplicatedIndex(enumTypeElement, index, names), enumTypeElement));

        return Optional.of(enumElement);
    }
}

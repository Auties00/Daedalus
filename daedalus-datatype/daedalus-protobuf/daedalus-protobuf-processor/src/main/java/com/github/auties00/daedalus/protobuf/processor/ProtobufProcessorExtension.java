package com.github.auties00.daedalus.protobuf.processor;

import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.processor.graph.DaedalusConverterGraph;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.annotation.*;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufBuilderParameterElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufObjectElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufReservedFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufUnknownFieldsElement;
import com.github.auties00.daedalus.protobuf.processor.generator.*;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufTypeManager;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufValidationManager;
import com.github.auties00.daedalus.protobuf.processor.metadata.ProtobufEnumMetadata;
import com.github.auties00.daedalus.protobuf.processor.metadata.ProtobufFieldMetadata;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.sun.source.tree.*;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.IntStream;

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
     * The compiler tree utilities for accessing AST nodes.
     */
    private Trees trees;

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
     * The cached type mirror for the {@code int} primitive type.
     */
    private TypeMirror intType;

    /**
     * The cached type mirror for the {@link ProtobufBinaryWriter} type.
     */
    private TypeMirror outputStreamType;

    /**
     * The cached type mirror for the serialized form of a protobuf message.
     */
    private TypeMirror serializedMessageType;

    /**
     * The set of fully qualified type names that have already been linked into
     * the converter graph, used to avoid duplicate linking.
     */
    private Set<String> linkedTypes;

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
                ProtobufMessage.UnknownFields.class.getCanonicalName(),
                ProtobufMessage.UnknownFields.Setter.class.getCanonicalName(),
                ProtobufEnum.class.getCanonicalName(),
                ProtobufEnum.Constant.class.getCanonicalName(),
                ProtobufAccessor.class.getCanonicalName(),
                ProtobufReservedRange.class.getCanonicalName(),
                ProtobufFieldParameter.class.getCanonicalName(),
                ProtobufSyntheticParameter.class.getCanonicalName()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ProcessingEnvironment processingEnv, DaedalusTypeManager types, DaedalusLogManager messages) {
        this.processingEnv = processingEnv;
        this.protobufTypes = new ProtobufTypeManager(processingEnv);
        this.messages = messages;
        this.checks = new ProtobufValidationManager(protobufTypes, messages);
        this.trees = Trees.instance(processingEnv);
        this.intType = protobufTypes.getType(int.class);
        this.outputStreamType = protobufTypes.getType(ProtobufBinaryWriter.class);
        this.serializedMessageType = protobufTypes.getType(void.class);
        this.linkedTypes = new HashSet<>();
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
     */
    @Override
    public void runChecks(RoundEnvironment roundEnv) {
        checks.runChecks(roundEnv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DaedalusTypeElement> processObjects(
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
    public void generateCode(List<? extends DaedalusTypeElement> objects) {
        TypeElement currentElement = null;
        try {
            for (var object : objects) {
                if (!(object instanceof ProtobufObjectElement protobufObject)) {
                    continue;
                }
                currentElement = protobufObject.typeElement();
                var packageName = processingEnv.getElementUtils().getPackageOf(protobufObject.typeElement());
                var specVisitor = new ProtobufObjectSpecGenerator(processingEnv.getFiler());
                specVisitor.createClass(protobufObject, packageName);

                if (protobufObject.type() == ProtobufObjectElement.Type.MESSAGE || protobufObject.type() == ProtobufObjectElement.Type.GROUP) {
                    var typeGenerator = new ProtobufBuilderTypeGenerator(processingEnv.getFiler());
                    typeGenerator.createClass(packageName.getQualifiedName().toString(), protobufObject);
                    for (var builder : protobufObject.builders()) {
                        var methodGenerator = new ProtobufBuilderMethodGenerator(processingEnv.getFiler());
                        methodGenerator.createClass(packageName.getQualifiedName().toString(), protobufObject, builder);
                    }
                }
            }
        } catch (IOException throwable) {
            messages.printError("An error occurred while processing protobuf: " + Objects.requireNonNullElse(throwable.getMessage(), throwable.getClass().getName()), currentElement);
        }
    }

    /**
     * Processes a single type element, dispatching to the appropriate handler
     * based on its element kind.
     *
     * @param object the type element to process
     * @return the set of processed protobuf objects, possibly empty
     */
    private Set<ProtobufObjectElement> processElement(TypeElement object) {
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
     * Processes a class or record annotated with {@link ProtobufMessage}.
     *
     * @param typeElement the class or record type element
     * @return the set of processed protobuf objects
     */
    private Set<ProtobufObjectElement> processObject(TypeElement typeElement) {
        var messageAnnotation = typeElement.getAnnotation(ProtobufMessage.class);
        if (messageAnnotation != null) {
            var deserializer = getObjectDeserializer(typeElement)
                    .orElse(null);
            Set<? extends ProtobufReservedFieldElement> reserved = ProtobufReservedFieldElement.of(messageAnnotation);
            var messageElement = ProtobufObjectElement.ofMessage(typeElement, deserializer, reserved);
            var results = processObject(messageElement, messageElement.typeElement());
            linkMessage(typeElement.asType());
            if (!hasPropertiesConstructor(messageElement)) {
                messages.printError("Missing protobuf constructor: a protobuf message must provide a constructor that takes as parameters only its properties, following their declaration order, and, if present, its unknown fields wrapper", messageElement.typeElement());
            }
            return results;
        }

        // An error will be printed by the preliminary checks
        return Set.of();
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
                messages.printError("Duplicated protobuf builder delegate: a message or group should provide only one method annotated with @TypeDeserializer", method);
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
    private Set<ProtobufObjectElement> processObject(ProtobufObjectElement messageElement, TypeElement typeElement) {
        Set<ProtobufObjectElement> results = new HashSet<>();

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
     * handling builder, accessor, serializer, and deserializer annotations.
     *
     * @param messageElement the enclosing protobuf message element
     * @param executableElement the method to process
     */
    private void processObjectMethod(ProtobufObjectElement messageElement, ExecutableElement executableElement) {
        var builder = executableElement.getAnnotation(TypeBuilder.class);
        if (builder != null) {
            var parameters = executableElement.getParameters()
                    .stream()
                    .map(ProtobufBuilderParameterElement::new)
                    .toList();
            messageElement.addBuilder(builder.name(), parameters, executableElement);
            return;
        }

        var getter = executableElement.getAnnotation(ProtobufAccessor.class);
        if (getter != null) {
            handleMessageGetter(messageElement, executableElement, getter);
            return;
        }

        processObjectSerializer(executableElement);
        processObjectDeserializer(executableElement);
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
        converterGraph.link(from, to, method);
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

        var from = !executableElement.getParameters().isEmpty() ? executableElement.getParameters().getFirst().asType() : executableElement.getEnclosingElement().asType();
        var to = executableElement.getReturnType();
        var method = DaedalusMethodElement.of(executableElement, protobufTypes.isParametrized(executableElement), serializer.warning());
        converterGraph.link(from, to, method);
    }

    /**
     * Validates that a getter annotated with {@link ProtobufAccessor} has a matching property.
     *
     * @param messageElement the enclosing protobuf message element
     * @param executableElement the getter method
     * @param getter the accessor annotation
     */
    private void handleMessageGetter(ProtobufObjectElement messageElement, ExecutableElement executableElement, ProtobufAccessor getter) {
        if (hasMatchedProperty(messageElement, getter)) {
            return;
        }
        messages.printError("Invalid getter: there is no property with index \"" + getter.index() + "\" in \"" + messageElement.typeElement().getQualifiedName().toString() + "\"", executableElement);
    }

    /**
     * Returns whether the given accessor annotation matches a property in the message.
     *
     * @param messageElement the protobuf message element
     * @param getter the accessor annotation
     * @return {@code true} if a matching property exists
     */
    private boolean hasMatchedProperty(ProtobufObjectElement messageElement, ProtobufAccessor getter) {
        return messageElement.protobufProperties()
                .stream()
                .anyMatch(entry -> !(entry.type().descriptorElementType() instanceof ExecutableElement) && entry.index() == getter.index());
    }

    /**
     * Returns whether the given message has a constructor whose parameters match
     * its properties in declaration order, optionally including unknown fields.
     *
     * @param message the protobuf message element
     * @return {@code true} if a matching constructor exists
     */
    private boolean hasPropertiesConstructor(ProtobufObjectElement message) {
        var unknownFieldsType = message.unknownFieldsElement()
                .orElse(null);
        var properties = message.protobufProperties()
                .stream()
                .filter(property -> !property.synthetic())
                .toList();
        return message.typeElement()
                .getEnclosedElements()
                .stream()
                .filter(entry -> entry.getKind() == ElementKind.CONSTRUCTOR)
                .map(entry -> (ExecutableElement) entry)
                .anyMatch(constructor -> {
                    var constructorParameters = constructor.getParameters();
                    if (properties.size() + (unknownFieldsType != null ? 1 : 0) != constructorParameters.size()) {
                        return false;
                    }

                    var propertiesIterator = properties.iterator();
                    var constructorParametersIterator = constructorParameters.iterator();
                    var foundUnknownFieldsParam = false;
                    while (propertiesIterator.hasNext() && constructorParametersIterator.hasNext()) {
                        var property = propertiesIterator.next();
                        var constructorParameter = constructorParametersIterator.next();
                        if (unknownFieldsType != null && protobufTypes.isAssignable(constructorParameter.asType(), property.type().descriptorElementType())) {
                            if (foundUnknownFieldsParam) {
                                messages.printError("Duplicated protobuf unknown fields parameter: a protobuf constructor should provide only one parameter whose type can be assigned to the field annotated with @ProtobufUnknownFields", constructorParameter);
                            }

                            foundUnknownFieldsParam = true;
                        } else if (!protobufTypes.isAssignable(property.type().descriptorElementType(), constructorParameter.asType())) {
                            return false;
                        }
                    }

                    return unknownFieldsType == null || foundUnknownFieldsParam;
                });
    }

    /**
     * Processes a field in a class or record annotated with {@link ProtobufMessage},
     * handling property and unknown fields annotations.
     *
     * @param messageElement the enclosing protobuf message element
     * @param variableElement the field to process
     * @return the set of protobuf objects discovered during processing
     */
    private Set<ProtobufObjectElement> processObjectField(ProtobufObjectElement messageElement, VariableElement variableElement) {
        var propertyAnnotation = ProtobufFieldMetadata.of(variableElement);
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
    private void processMessageUnknownFields(ProtobufObjectElement messageElement, VariableElement variableElement, ProtobufMessage.UnknownFields unknownFieldsAnnotation) {
        if (messageElement.unknownFieldsElement().isPresent()) {
            messages.printError("Duplicated protobuf unknown fields: a message should provide only one method field annotated with @ProtobufUnknownFields", variableElement);
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
     * Processes the type and setter for an unknown fields field.
     *
     * @param variableElement the annotated field
     * @param unknownFieldsAnnotation the unknown fields annotation
     * @return the unknown fields element, if valid
     */
    private Optional<ProtobufUnknownFieldsElement> processUnknownFieldsField(VariableElement variableElement, ProtobufMessage.UnknownFields unknownFieldsAnnotation) {
        var unknownFieldsType = variableElement.asType();
        if (!(unknownFieldsType instanceof DeclaredType unknownFieldsDeclaredType)) {
            messages.printError("Type error: variables annotated with @ProtobufUnknownFields must have an object type", variableElement);
            return Optional.empty();
        }

        var mixins = protobufTypes.getMixins(unknownFieldsAnnotation);
        var setter = findUnknownFieldsSetterInType(unknownFieldsDeclaredType);
        if (setter != null) {
            return checkUnknownFieldsSetter(setter, false)
                    .map(setterElement -> createUnknownFieldsElement(variableElement, unknownFieldsDeclaredType, setterElement, unknownFieldsType, mixins));
        }

        var setterFromMixin = findUnknownFieldsSetterInMixins(variableElement, unknownFieldsType, mixins);
        if (setterFromMixin == null) {
            messages.printError("Type error: cannot find a @ProtobufUnknownFields.Setter for the provided type", variableElement);
            return Optional.empty();
        }

        return checkUnknownFieldsSetter(setterFromMixin, true)
                .map(setterElement -> createUnknownFieldsElement(variableElement, unknownFieldsDeclaredType, setterElement, unknownFieldsType, mixins));
    }

    /**
     * Creates a new unknown fields element with a resolved default value.
     *
     * @param variableElement the annotated field
     * @param variableType the declared type of the field
     * @param setterElement the setter method for the unknown fields
     * @param unknownFieldsType the type mirror of the unknown fields
     * @param mixins the mixin type elements
     * @return the unknown fields element
     */
    private ProtobufUnknownFieldsElement createUnknownFieldsElement(VariableElement variableElement, DeclaredType variableType, ExecutableElement setterElement, TypeMirror unknownFieldsType, List<TypeElement> mixins) {
        var defaultValue = getDefaultValue(variableElement, unknownFieldsType, mixins)
                .orElse("new %s()".formatted(variableType));
        return new ProtobufUnknownFieldsElement(variableType, defaultValue, setterElement);
    }

    /**
     * Finds a method annotated with {@link ProtobufMessage.UnknownFields.Setter}
     * in the given declared type.
     *
     * @param unknownFieldsDeclaredType the declared type to search
     * @return the setter method, or {@code null} if not found
     */
    private ExecutableElement findUnknownFieldsSetterInType(DeclaredType unknownFieldsDeclaredType) {
        return (ExecutableElement) unknownFieldsDeclaredType.asElement()
                .getEnclosedElements()
                .stream()
                .filter(enclosedElement -> enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getAnnotation(ProtobufMessage.UnknownFields.Setter.class) != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a method annotated with {@link ProtobufMessage.UnknownFields.Setter}
     * in the given mixin types that accepts the unknown fields type.
     *
     * @param element the annotated field element, used for error reporting
     * @param unknownFieldsType the type of the unknown fields
     * @param mixins the mixin type elements to search
     * @return the setter method, or {@code null} if not found
     */
    private ExecutableElement findUnknownFieldsSetterInMixins(VariableElement element, TypeMirror unknownFieldsType, List<TypeElement> mixins) {
        return mixins.stream()
                .map(TypeElement::getEnclosedElements)
                .flatMap(Collection::stream)
                .filter(enclosedElement -> enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getAnnotation(ProtobufMessage.UnknownFields.Setter.class) != null)
                .map(enclosedElement -> (ExecutableElement) enclosedElement)
                .filter(enclosedMethod -> !enclosedMethod.getParameters().isEmpty() && protobufTypes.isAssignable(enclosedMethod.getParameters().getFirst().asType(), unknownFieldsType))
                .reduce((first, second) -> {
                    messages.printError("Duplicated protobuf unknown fields setter: only one setter for %s is allowed in the mixins".formatted(unknownFieldsType), element);
                    return first;
                })
                .orElse(null);
    }

    /**
     * Validates the unknown fields setter method for correct visibility, static modifier,
     * parameter count, and parameter types.
     *
     * @param setter the setter method to validate
     * @param fromMixin whether the setter comes from a mixin
     * @return the setter method if valid, or empty if invalid
     */
    private Optional<ExecutableElement> checkUnknownFieldsSetter(ExecutableElement setter, boolean fromMixin) {
        if (!setter.getModifiers().contains(Modifier.PUBLIC)) {
            messages.printError("Type error: methods annotated with @ProtobufUnknownFields.Setter must have public visibility", setter);
            return Optional.empty();
        }

        if (fromMixin != setter.getModifiers().contains(Modifier.STATIC)) {
            messages.printError("Type error: methods annotated with @ProtobufUnknownFields.Setter %s".formatted(fromMixin ? "in a mixin must be static" : "must not be static"), setter);
            return Optional.empty();
        }

        if (setter.getParameters().size() != (fromMixin ? 3 : 2)) {
            messages.printError("Type error: methods annotated with @ProtobufUnknownFields.Setter %smust take only %s parameters".formatted(fromMixin ? "in a mixin" : "", fromMixin ? "three" : "two"), setter);
            return Optional.empty();
        }

        var error = false;
        var keyType = setter.getParameters()
                .get(fromMixin ? 1 : 0)
                .asType();
        if (!protobufTypes.isAssignable(keyType, Long.class) && !protobufTypes.isSameType(keyType, long.class)) {
            messages.printError("Type error: methods annotated with @ProtobufUnknownFields.Setter %smust take as the %s parameter a long".formatted(fromMixin ? "in a mixin" : "", fromMixin ? "second" : "first"), setter);
            error = true;
        }

        var valueType = setter.getParameters()
                .get(fromMixin ? 2 : 1)
                .asType();
        if (!protobufTypes.isSameType(valueType, Object.class)) {
            messages.printError("Type error: methods annotated with @ProtobufUnknownFields.Setter %smust take as the %s parameter an Object".formatted(fromMixin ? "in a mixin" : "", fromMixin ? "third" : "second"), setter);
            error = true;
        }

        return error ? Optional.empty() : Optional.of(setter);
    }

    /**
     * Processes a field with a protobuf field annotation, validating
     * its type, index, name, accessor, and converters.
     *
     * @param messageElement the enclosing protobuf message element
     * @param variableElement the annotated field
     * @param propertyAnnotation the normalized field annotation
     * @return the set of protobuf objects discovered during linking
     */
    private Set<ProtobufObjectElement> processMessageProperty(ProtobufObjectElement messageElement, VariableElement variableElement, ProtobufFieldMetadata propertyAnnotation) {
        if (propertyAnnotation.type() == ProtobufType.UNKNOWN) {
            messages.printError("Type error: properties must specify a valid protobuf type", variableElement);
            return Set.of();
        }

        if (propertyAnnotation.required() && !checks.isValidRequiredProperty(variableElement)) {
            return Set.of();
        }

        if (propertyAnnotation.packed() && !checks.isValidPackedProperty(variableElement, propertyAnnotation)) {
            return Set.of();
        }

        var accessor = getAccessor(variableElement, propertyAnnotation);
        if (accessor.isEmpty()) {
            messages.printError("Missing accessor: a non-private getter/accessor must be declared, or the property must have non-private visibility.", variableElement);
            return Set.of();
        }

        var accessorType = getAccessorType(accessor.get());
        var variableType = variableElement.asType();
        var type = getPropertyType(variableElement, variableType, accessorType, propertyAnnotation);
        if (type.isEmpty()) {
            return Set.of();
        }

        var propertyName = variableElement.getSimpleName().toString();
        if (!messageElement.isNameAllowed(propertyName)) {
            messages.printError("Restricted message property name: %s is not allowed as it's marked as reserved".formatted(propertyName), variableElement);
        }

        if (!messageElement.isIndexAllowed(propertyAnnotation.index())) {
            messages.printError("Restricted message property index: %s is not allowed as it's marked as reserved".formatted(propertyAnnotation.index()), variableElement);
        }

        if (propertyAnnotation.ignored()) {
            return Set.of();
        }

        var error = messageElement.addProperty(variableElement, accessor.get(), type.get(), propertyAnnotation);
        if (error.isPresent()) {
            messages.printError("Duplicated message property: %s and %s with index %s".formatted(variableElement.getSimpleName(), error.get().name(), propertyAnnotation.index()), variableElement);
            return Set.of();
        }

        var mixins = protobufTypes.getMirroredTypes(propertyAnnotation.mixins());
        linkMixins(mixins);
        return linkType(variableType);
    }

    /**
     * Links the given type into the converter graph, processing its serializers,
     * deserializers, and type arguments recursively.
     *
     * @param variableType the type to link
     * @return the set of protobuf objects discovered during linking
     */
    private Set<ProtobufObjectElement> linkType(TypeMirror variableType) {
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

        Set<ProtobufObjectElement> results = new HashSet<>();
        var name = typeElement.getQualifiedName().toString();
        if (linkedTypes.add(name)) {
            for (var entry : typeElement.getEnclosedElements()) {
                if (entry instanceof ExecutableElement element) {
                    processObjectSerializer(element);
                    processObjectDeserializer(element);
                }
            }
        }
        return results;
    }

    /**
     * Links the encode and decode converters for a protobuf enum type.
     *
     * @param type the enum type
     */
    private void linkEnum(TypeMirror type) {
        var specName = DaedalusMethodGenerator.getSpecByType(type);
        var serializer = DaedalusMethodElement.of(specName, Set.of(Modifier.PUBLIC, Modifier.STATIC), intType, ProtobufObjectSerializationGenerator.METHOD_NAME, type);
        converterGraph.link(type, intType, serializer);
        var deserializer = DaedalusMethodElement.of(specName, Set.of(Modifier.PUBLIC, Modifier.STATIC), type, ProtobufDeserializationGenerator.METHOD_NAME, intType);
        converterGraph.link(intType, type, deserializer);
    }

    /**
     * Links the encode and decode converters for a protobuf message type,
     * including recursive linking of its type arguments.
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
        var serializer = DaedalusMethodElement.of(specName, Set.of(Modifier.PUBLIC, Modifier.STATIC), serializedMessageType, ProtobufSerializationGenerator.METHOD_NAME, type, outputStreamType);
        converterGraph.link(type, serializedMessageType, serializer);
        var deserializer = DaedalusMethodElement.of(specName, Set.of(Modifier.PUBLIC, Modifier.STATIC), type, ProtobufDeserializationGenerator.METHOD_NAME, serializedMessageType);
        converterGraph.link(serializedMessageType, type, deserializer);
    }

    /**
     * Links all serializer and deserializer methods from the given mixin types
     * into the converter graphs.
     *
     * @param mixins the mixin type elements
     */
    private void linkMixins(List<TypeElement> mixins) {
        for (var mixin : mixins) {
            for (var element : mixin.getEnclosedElements()) {
                if (element instanceof ExecutableElement method) {
                    processObjectSerializer(method);
                    processObjectDeserializer(method);
                }
            }
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
    private Optional<String> getCollectionDefaultValue(Element invoker, TypeMirror collectionType, List<TypeElement> mixins) {
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
    private Optional<String> getDefaultValue(Element invoker, TypeMirror type, List<TypeElement> mixins) {
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
                messages.printError("Duplicated default value: " + getDefaultValueCaller(candidate) + " provides a default value for type " + bestMatchType + ", which was already defined by " + getDefaultValueType(bestMatch), invoker);
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
    private Optional<? extends ProtobufFieldType> getPropertyType(Element invoker, TypeMirror elementType, TypeMirror accessorType, ProtobufFieldMetadata property) {
        var mixins = protobufTypes.getMirroredTypes(property.mixins());
        if (protobufTypes.isAssignable(elementType, Collection.class)) {
            return getConcreteCollectionType(invoker, property, elementType, mixins);
        }

        if (protobufTypes.isAssignable(elementType, Map.class) && property instanceof ProtobufFieldMetadata.MapField mapField) {
            return getConcreteMapType(mapField, invoker, elementType, mixins);
        }

        if (property instanceof ProtobufFieldMetadata.MapField mapField) {
            return getConcreteMapType(mapField, invoker, elementType, mixins);
        }

        var defaultValue = getDefaultValue(invoker, elementType, mixins)
                .orElse("null");
        var implementation = new ProtobufSimpleFieldType(
                property.type(),
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
     * @param mixins the mixin type elements
     * @return the resolved collection property type, if valid
     */
    private Optional<? extends ProtobufFieldType> getConcreteCollectionType(Element invoker, ProtobufFieldMetadata property, TypeMirror elementType, List<TypeElement> mixins) {
        var collectionTypeParameter = protobufTypes.getTypeParameter(elementType, protobufTypes.getType(Collection.class), 0);
        if (collectionTypeParameter.isEmpty()) {
            messages.printError("Type inference error: cannot determine collection's type parameter", invoker);
            return Optional.empty();
        }

        var collectionDefaultValue = getCollectionDefaultValue(invoker, elementType, mixins);
        if (collectionDefaultValue.isEmpty()) {
            messages.printError("Type inference error: cannot determine collection's default value, provide one either in the definition or using a mixin", invoker);
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
                mixins
        );
        return Optional.of(type);
    }

    /**
     * Finds the accessor for a property field, either the field itself if it has
     * sufficient visibility, or a matching getter method.
     *
     * @param fieldElement the field element
     * @param propertyAnnotation the property annotation
     * @return the accessor element, if found
     */
    private Optional<? extends Element> getAccessor(VariableElement fieldElement, ProtobufFieldMetadata propertyAnnotation) {
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
                .filter(entry -> isProtobufGetter(entry, propertyAnnotation))
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
     * and matches the given property annotation index.
     *
     * @param entry the method element
     * @param propertyAnnotation the normalized field annotation
     * @return {@code true} if the method is a matching getter
     */
    private boolean isProtobufGetter(ExecutableElement entry, ProtobufFieldMetadata propertyAnnotation) {
        var annotation = entry.getAnnotation(ProtobufAccessor.class);
        return annotation != null && annotation.index() == propertyAnnotation.index();
    }

    /**
     * Resolves the concrete map type for a property, including its key type,
     * value type, and default value.
     *
     * @param invoker the element used for error reporting
     * @param elementType the type of the field element
     * @param mixins the mixin type elements
     * @return the resolved map property type, if valid
     */
    private Optional<ProtobufMapFieldType> getConcreteMapType(ProtobufFieldMetadata.MapField mapField, Element invoker, TypeMirror elementType, List<TypeElement> mixins) {
        var keyType = mapField.mapKeyType();
        var valueType = mapField.mapValueType();

        if (keyType == ProtobufType.UNKNOWN || keyType == ProtobufType.MAP) {
            messages.printError("Missing type error: specify the type of the map's key in @MapField with mapKeyType", invoker);
            return Optional.empty();
        }

        if (valueType == ProtobufType.UNKNOWN || valueType == ProtobufType.MAP) {
            messages.printError("Missing type error: specify the type of the map's valueType in @MapField with mapValueType", invoker);
            return Optional.empty();
        }

        if (keyType == ProtobufType.MESSAGE || keyType == ProtobufType.ENUM || keyType == ProtobufType.GROUP) {
            messages.printError("Type error: protobuf doesn't support messages, enums or groups as keys in a map", invoker);
            return Optional.empty();
        }

        if (valueType == ProtobufType.GROUP) {
            messages.printError("Type error: protobuf doesn't support groups as values in a map", invoker);
            return Optional.empty();
        }

        var keyTypeParameter = protobufTypes.getTypeParameter(elementType, protobufTypes.getType(Map.class), 0)
                .orElse(protobufTypes.getType(ProtobufFieldMetadata.deserializableType(keyType)));
        if (keyTypeParameter == null) {
            messages.printError("Type inference error: cannot determine map's key type", invoker);
            return Optional.empty();
        }

        var keyEntry = new ProtobufSimpleFieldType(
                keyType,
                keyTypeParameter,
                keyTypeParameter,
                null,
                mixins
        );
        createUnattributedSerializer(invoker, keyEntry);
        createUnattributedDeserializer(invoker, keyEntry);

        var valueTypeParameter = protobufTypes.getTypeParameter(elementType, protobufTypes.getType(Map.class), 1)
                .orElse(valueType != ProtobufType.MESSAGE && valueType != ProtobufType.ENUM ? protobufTypes.getType(ProtobufFieldMetadata.deserializableType(valueType)) : null);
        if (valueTypeParameter == null) {
            messages.printError("Type inference error: cannot determine map's valueType type", invoker);
            return Optional.empty();
        }

        var valueDefaultValue = getDefaultValue(invoker, valueTypeParameter, mixins)
                .orElse("null");
        var valueEntry = new ProtobufSimpleFieldType(
                valueType,
                valueTypeParameter,
                valueTypeParameter,
                valueDefaultValue,
                mixins
        );
        createUnattributedSerializer(invoker, valueEntry);
        createUnattributedDeserializer(invoker, valueEntry);

        var mapDefaultValue = getCollectionDefaultValue(invoker, elementType, mixins);
        if (mapDefaultValue.isEmpty()) {
            messages.printError("Type inference error: cannot determine map default valueType", invoker);
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
    private void createUnattributedSerializer(Element invoker, ProtobufFieldType implementation) {
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
    private void createUnattributedSerializer(Element invoker, TypeMirror from, ProtobufFieldType implementation) {
        var to = implementation.protobufType();
        var toWrapped = protobufTypes.getType(ProtobufFieldMetadata.deserializableType(to));
        if (to != ProtobufType.MESSAGE && to != ProtobufType.ENUM && to != ProtobufType.GROUP && protobufTypes.isAssignable(from, toWrapped)) {
            return;
        }

        var targetDescription = getProtobufTypeName(to, true);
        var unattributed = new DaedalusConverterElement.Unattributed(invoker, from, toWrapped, targetDescription, implementation.mixins(), DaedalusConverterElement.Unattributed.Type.SERIALIZER);
        implementation.addConverter(unattributed);
    }

    /**
     * Creates an unattributed deserializer converter element for the given property type,
     * if a conversion from the protobuf wire type to the descriptor element type is needed.
     *
     * @param invoker the element used for error reporting
     * @param implementation the property type
     */
    private void createUnattributedDeserializer(Element invoker, ProtobufFieldType implementation) {
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
    private void createUnattributedDeserializer(Element invoker, TypeMirror to, ProtobufFieldType implementation) {
        var from = implementation.protobufType();
        var fromType = protobufTypes.getType(ProtobufFieldMetadata.deserializableType(from));
        if (from != ProtobufType.MESSAGE && from != ProtobufType.ENUM && from != ProtobufType.GROUP && protobufTypes.isAssignable(to, fromType)) {
            return;
        }

        var targetDescription = getProtobufTypeName(from, false);
        var unattributed = new DaedalusConverterElement.Unattributed(invoker, fromType, to, targetDescription, implementation.mixins(), DaedalusConverterElement.Unattributed.Type.DESERIALIZER);
        implementation.addConverter(unattributed);
    }

    /**
     * Returns a human readable name for the given protobuf type, used in error messages
     * for missing converters.
     *
     * @param type the protobuf type
     * @param serializer whether this is for a serializer (affects the display of non-object types)
     * @return the human readable type name
     */
    private String getProtobufTypeName(ProtobufType type, boolean serializer) {
        return switch (type) {
            case MESSAGE -> "ProtobufMessage";
            case ENUM -> "ProtobufEnum";
            case GROUP -> "ProtobufGroup";
            case STRING -> "ProtobufString";
            default -> "%s(%s)".formatted(type.name(), ProtobufFieldMetadata.deserializableType(type));
        };
    }

    /**
     * Processes an enum annotated with {@link ProtobufEnum}, creating its element,
     * linking it into the converter graph, and processing its constants.
     *
     * @param enumElement the enum type element
     * @return the set of processed protobuf objects
     */
    private Set<ProtobufObjectElement> processEnum(TypeElement enumElement) {
        var messageElement = createEnumElement(enumElement);
        if (messageElement.isEmpty()) {
            return Set.of();
        }

        linkEnum(enumElement.asType());

        var constantsCount = processEnumConstants(messageElement.get());
        if (constantsCount == 0) {
            messages.printWarning("No constants found", enumElement);
        }

        return Set.of(messageElement.get());
    }

    /**
     * Counts and processes all enum constants in the given protobuf enum element.
     *
     * @param messageElement the protobuf enum element
     * @return the number of constants processed
     */
    private long processEnumConstants(ProtobufObjectElement messageElement) {
        var enumTree = trees.getTree(messageElement.typeElement());
        return enumTree.getMembers()
                .stream()
                .filter(member -> member instanceof VariableTree)
                .map(member -> (VariableTree) member)
                .peek(variableTree -> processEnumConstant(messageElement, messageElement.typeElement(), variableTree))
                .count();
    }

    /**
     * Creates a protobuf enum element from the given enum type, resolving
     * its metadata and reserved elements.
     *
     * @param enumElement the enum type element
     * @return the protobuf enum element, if valid
     */
    private Optional<ProtobufObjectElement> createEnumElement(TypeElement enumElement) {
        var enumAnnotation = enumElement.getAnnotation(ProtobufEnum.class);
        if (enumAnnotation == null) {
            return Optional.empty();
        }

        Set<? extends ProtobufReservedFieldElement> reserved = ProtobufReservedFieldElement.of(enumAnnotation);
        var metadata = getEnumMetadata(enumElement);
        if (metadata.isEmpty()) {
            return Optional.of(ProtobufObjectElement.ofEnum(enumElement, ProtobufEnumMetadata.javaEnum(), reserved));
        }

        if (metadata.get().isUnknown()) {
            return Optional.empty();
        }

        var result = ProtobufObjectElement.ofEnum(enumElement, metadata.get(), reserved);
        return Optional.of(result);
    }

    /**
     * Resolves the enum metadata by finding a constructor with a parameter annotated
     * with {@link ProtobufEnum.Constant} or a single int parameter.
     *
     * @param enumElement the enum type element
     * @return the enum metadata, if found
     */
    private Optional<ProtobufEnumMetadata> getEnumMetadata(TypeElement enumElement) {
        var fields = getEnumFields(enumElement);
        return getConstructors(enumElement)
                .stream()
                .map(constructor -> getEnumMetadata(constructor, fields))
                .flatMap(Optional::stream)
                .reduce((first, second) -> {
                    messages.printError("Duplicated protobuf constructor: an enum should provide only one constructor with a scalar parameter annotated with @ProtobufEnum.Constant", second.constructor());
                    return first;
                });
    }

    /**
     * Resolves the enum metadata for a single constructor.
     *
     * @param constructor the constructor element
     * @param fields the enum fields
     * @return the enum metadata, if found
     */
    private Optional<ProtobufEnumMetadata> getEnumMetadata(ExecutableElement constructor, ProtobufEnumFields fields) {
        var constructorTree = trees.getTree(constructor);
        return IntStream.range(0, constructor.getParameters().size())
                .filter(index -> isImplicitEnumConstructor(constructor) || hasProtobufIndexAnnotation(constructor, index))
                .mapToObj(index -> getEnumMetadata(constructor, constructor.getParameters().get(index), index, constructorTree, fields))
                .reduce((first, second) -> {
                    messages.printError("Duplicated protobuf enum index: an enum constructor should provide only one parameter annotated with @ProtobufEnum.Constant", second.parameter());
                    return first;
                });
    }

    /**
     * Returns whether the given constructor is an implicit enum constructor
     * with a single int parameter.
     *
     * @param constructor the constructor element
     * @return {@code true} if the constructor is implicit
     */
    private boolean isImplicitEnumConstructor(ExecutableElement constructor) {
        return constructor.getParameters().size() == 1
               && protobufTypes.isSameType(constructor.getParameters().getFirst().asType(), int.class);
    }

    /**
     * Returns whether the parameter at the given index in the constructor
     * is annotated with {@link ProtobufEnum.Constant}.
     *
     * @param constructor the constructor element
     * @param index the parameter index
     * @return {@code true} if the parameter is annotated
     */
    private boolean hasProtobufIndexAnnotation(ExecutableElement constructor, int index) {
        return constructor.getParameters()
                       .get(index)
                       .getAnnotation(ProtobufEnum.Constant.class) != null;
    }

    /**
     * Resolves the enum metadata for a specific constructor parameter, tracing
     * field assignments in the constructor body to identify the index field.
     *
     * @param constructor the constructor element
     * @param parameter the annotated parameter
     * @param index the parameter index
     * @param constructorTree the AST tree of the constructor
     * @param fields the enum fields
     * @return the resolved enum metadata
     */
    private ProtobufEnumMetadata getEnumMetadata(ExecutableElement constructor, VariableElement parameter, int index, MethodTree constructorTree, ProtobufEnumFields fields) {
        if (fields.enumIndexField() != null) {
            return new ProtobufEnumMetadata(constructor, fields.enumIndexField(), parameter, index);
        }

        return constructorTree.getBody()
                .getStatements()
                .stream()
                .filter(constructorEntry -> constructorEntry instanceof ExpressionStatementTree)
                .map(constructorEntry -> ((ExpressionStatementTree) constructorEntry).getExpression())
                .filter(constructorEntry -> constructorEntry instanceof AssignmentTree)
                .map(constructorEntry -> (AssignmentTree) constructorEntry)
                .filter(assignmentTree -> isEnumIndexParameterAssignment(assignmentTree, parameter))
                .map(this::getAssignmentExpressionName)
                .flatMap(Optional::stream)
                .map(fields.fields()::get)
                .filter(Objects::nonNull)
                .reduce((first, second) -> {
                    messages.printError("Duplicated assignment: the parameter annotated with @ProtobufEnum.Constant must be assigned to a single local field", second);
                    return first;
                })
                .map(fieldElement -> {
                    checkProtobufEnumIndexField(fieldElement);
                    return new ProtobufEnumMetadata(constructor, fieldElement, parameter, index);
                })
                .orElseGet(() -> {
                    messages.printError("Missing or too complex assignment: the parameter annotated with @ProtobufEnum.Constant should be assigned to a local field", constructor);
                    messages.printError("If the assignment is too complex for the compiler to evaluate, annotate the local field directly with @ProtobufEnum.Constant", constructor);
                    return ProtobufEnumMetadata.unknown();
                });
    }

    /**
     * Returns whether the given assignment tree assigns the specified parameter.
     *
     * @param assignmentTree the assignment tree
     * @param parameter the parameter element
     * @return {@code true} if the assignment uses the parameter
     */
    private boolean isEnumIndexParameterAssignment(AssignmentTree assignmentTree, VariableElement parameter) {
        return assignmentTree.getExpression() instanceof IdentifierTree identifierTree
               && identifierTree.getName().equals(parameter.getSimpleName());
    }

    /**
     * Returns the name of the field being assigned to in the given assignment tree.
     *
     * @param assignmentTree the assignment tree
     * @return the field name, if resolvable
     */
    private Optional<Name> getAssignmentExpressionName(AssignmentTree assignmentTree) {
        if (assignmentTree.getExpression() instanceof IdentifierTree fieldIdentifier) {
            return Optional.of(fieldIdentifier.getName());
        } else if (assignmentTree.getExpression() instanceof MemberSelectTree memberSelectTree) {
            return Optional.of(memberSelectTree.getIdentifier());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Collects the fields of the given enum type, checking for a field annotated
     * with {@link ProtobufEnum.Constant}.
     *
     * @param enumElement the enum type element
     * @return the collected enum fields
     */
    private ProtobufEnumFields getEnumFields(TypeElement enumElement) {
        Map<Name, VariableElement> fields = new HashMap<>();
        for (var entry : enumElement.getEnclosedElements()) {
            if (!(entry instanceof VariableElement variableElement)) {
                continue;
            }

            if (variableElement.getAnnotation(ProtobufEnum.Constant.class) != null) {
                checkProtobufEnumIndexField(variableElement);
                return new ProtobufEnumFields(variableElement, null);
            }

            fields.put(variableElement.getSimpleName(), variableElement);
        }

        return new ProtobufEnumFields(null, fields);
    }

    /**
     * Validates that the enum index field has at least package-private visibility.
     *
     * @param variableElement the field element
     */
    private void checkProtobufEnumIndexField(VariableElement variableElement) {
        if (!variableElement.getModifiers().contains(Modifier.PRIVATE)) {
            return;
        }

        messages.printError("Weak visibility: the field annotated with @ProtobufEnum.Constant must have at least package-private visibility", variableElement);
    }

    /**
     * A holder for the fields of a protobuf enum during metadata resolution.
     *
     * @param enumIndexField the field annotated with {@link ProtobufEnum.Constant}, if any
     * @param fields the map of field names to field elements
     */
    private record ProtobufEnumFields(VariableElement enumIndexField, Map<Name, VariableElement> fields) {

    }

    /**
     * Returns all constructor elements of the given enum type.
     *
     * @param enumElement the enum type element
     * @return the list of constructors
     */
    private List<ExecutableElement> getConstructors(TypeElement enumElement) {
        return enumElement.getEnclosedElements()
                .stream()
                .filter(entry -> entry instanceof ExecutableElement)
                .map(entry -> (ExecutableElement) entry)
                .filter(entry -> entry.getKind() == ElementKind.CONSTRUCTOR)
                .toList();
    }

    /**
     * Processes a single enum constant, extracting its index from the constructor
     * arguments and registering it with the protobuf enum element.
     *
     * @param messageElement the protobuf enum element
     * @param enumElement the enum type element, used for error reporting
     * @param enumConstantTree the AST tree of the enum constant
     */
    private void processEnumConstant(ProtobufObjectElement messageElement, TypeElement enumElement, VariableTree enumConstantTree) {
        if (!(enumConstantTree.getInitializer() instanceof NewClassTree newClassTree)) {
            return;
        }

        var newClassType = newClassTree.getIdentifier().toString();
        var simpleEnumName = enumElement.getSimpleName().toString();
        if (!newClassType.equals(simpleEnumName) && !newClassType.equals(messageElement.typeElement().getQualifiedName().toString())) {
            return;
        }

        var variableName = enumConstantTree.getName().toString();
        if (messageElement.enumMetadata().orElseThrow().isJavaEnum()) {
            var ordinal = messageElement.constants().size();
            if (!messageElement.isIndexAllowed(ordinal)) {
                messages.printError("Restricted message property index: %s is not allowed as it's marked as reserved".formatted(ordinal), enumElement);
            }

            var error = messageElement.addEnumConstant(ordinal, variableName);
            if (error.isEmpty()) {
                return;
            }

            messages.printError("Duplicated enum constant: %s and %s with index %s".formatted(variableName, error.get(), ordinal), enumElement);
        } else {
            if (newClassTree.getArguments().isEmpty()) {
                messages.printError("%s doesn't specify an index".formatted(variableName), enumElement);
                return;
            }

            var indexArgument = newClassTree.getArguments().get(messageElement.enumMetadata().orElseThrow().parameterIndex());
            if (!(indexArgument instanceof LiteralTree literalTree)) {
                messages.printError("%s's index must be a constant valueType".formatted(variableName), enumElement);
                return;
            }

            var value = ((Number) literalTree.getValue()).intValue();
            if (value < 0) {
                messages.printError("%s's index must be positive".formatted(variableName), enumElement);
            }

            if (!messageElement.isIndexAllowed(value)) {
                messages.printError("Restricted message property index: %s is not allowed as it's marked as reserved".formatted(value), enumElement);
            }

            var error = messageElement.addEnumConstant(value, variableName);
            if (error.isEmpty()) {
                return;
            }

            messages.printError("Duplicated enum constant: %s and %s with index %s".formatted(variableName, error.get(), value), enumElement);
        }
    }
}

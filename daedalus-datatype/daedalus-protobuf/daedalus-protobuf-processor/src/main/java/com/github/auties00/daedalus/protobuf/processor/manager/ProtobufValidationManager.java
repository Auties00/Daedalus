package com.github.auties00.daedalus.protobuf.processor.manager;

import com.github.auties00.daedalus.processor.manager.DaedalusValidationManager;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.protobuf.annotation.*;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.diagnostic.ProtobufProcessorErrors;
import com.github.auties00.daedalus.protobuf.processor.element.ParsedProtobufField;
import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A protobuf specific extension of the common validation checks that adds
 * protobuf annotation validation.
 *
 * <p>This class extends {@link DaedalusValidationManager}
 * with checks for protobuf messages, enums, properties, unknown fields,
 * accessors, and reserved ranges. The common checks for mixins, serializers,
 * deserializers, builders, and default values are inherited from the parent.
 */
public final class ProtobufValidationManager extends DaedalusValidationManager {
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
     * The protobuf specific type utility used for type predicates.
     */
    private final ProtobufTypeManager protobufTypes;

    /**
     * Constructs a new protobuf checks instance with the given type and message utilities.
     *
     * @param types the protobuf type utility
     * @param messages the diagnostics utility
     */
    public ProtobufValidationManager(ProtobufTypeManager types, DaedalusLogManager messages) {
        super(types, messages);
        this.protobufTypes = types;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFormatManagedType(TypeMirror type) {
        // A protobuf enum that is not a Java enum may convert its values to and from their index
        return protobufTypes.isMessage(type);
    }

    /**
     * Runs the protobuf specific validation checks against the given round environment.
     *
     * <p>This method invokes the checks for messages, enums, properties, accessors,
     * unknown fields, and reserved ranges. The common checks inherited from the parent
     * class are run once for every extension by the processor, so they are not run
     * here.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void runChecks(RoundEnvironment roundEnv) {
        checkMessages(roundEnv);
        checkMessageProperties(roundEnv);
        checkEnums(roundEnv);
        checkEnumProperties(roundEnv);
        checkAnyGetters(roundEnv);
        checkUnknownFields(roundEnv);
        checkReservedRanges(roundEnv);
        checkEnumConstantSerializerMixing(roundEnv);
        checkBuilderParameters(roundEnv);
    }

    /**
     * Validates all elements annotated with {@link ProtobufMessage} in the given round.
     *
     * <p>Only classes and records may be annotated with {@code @ProtobufMessage}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkMessages(RoundEnvironment roundEnv) {
        checkAnnotation(
                roundEnv,
                ProtobufMessage.class,
                ProtobufProcessorErrors::messageIllegalType,
                ElementKind.CLASS,
                ElementKind.RECORD
        );
    }

    /**
     * Validates all elements annotated with {@link ProtobufEnum} in the given round.
     *
     * <p>Enums, classes and records may be annotated with {@code @ProtobufEnum}, and so may
     * interfaces and abstract classes, as long as they are sealed, so that the set of their
     * values is known at compile time.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkEnums(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufEnum.class)) {
            var legal = switch (element.getKind()) {
                case ENUM, RECORD -> true;
                case CLASS -> !element.getModifiers().contains(Modifier.ABSTRACT) || element.getModifiers().contains(Modifier.SEALED);
                case INTERFACE -> element.getModifiers().contains(Modifier.SEALED);
                default -> false;
            };
            if (!legal) {
                messages.printError(ProtobufProcessorErrors.enumIllegalType(element.getSimpleName().toString()), element);
            }
        }
    }

    /**
     * Validates all elements with protobuf field annotations in the given round.
     *
     * <p>Each property must be enclosed by a type annotated with {@code @ProtobufMessage},
     * and all referenced mixins must be valid.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkMessageProperties(RoundEnvironment roundEnv) {
        for (var messageElement : roundEnv.getElementsAnnotatedWith(ProtobufMessage.class)) {
            for (var enclosed : messageElement.getEnclosedElements()) {
                ParsedProtobufField.of(enclosed).ifPresent(annotation -> processMessageProperty(enclosed, annotation));
                var oneOfAnnotation = enclosed.getAnnotation(ProtobufMessage.OneOfField.class);
                if (oneOfAnnotation != null) {
                    for (var oneOfCase : ParsedProtobufField.cases(oneOfAnnotation)) {
                        processMessageProperty(enclosed, oneOfCase.field());
                    }
                }
            }
        }
    }

    /**
     * Validates a single element with a protobuf field annotation.
     *
     * @param property the element to validate
     * @param annotation the normalized field annotation
     */
    private void processMessageProperty(Element property, ParsedProtobufField annotation) {
        var enclosingElement = types.getEnclosingTypeElement(property);
        if (enclosingElement.getAnnotation(ProtobufMessage.class) == null) {
            messages.printError(ProtobufProcessorErrors.fieldIllegalEnclosing(property, ParsedProtobufField.annotationNames(property).getFirst()), property);
            return;
        }

        var mixins = protobufTypes.getMirroredTypes(annotation.mixins());
        checkMixins(property, mixins);
    }

    /**
     * Validates all elements annotated with {@link ProtobufEnum.Constant} in the given round.
     *
     * <p>Each constant must be enclosed by a type annotated with {@code @ProtobufEnum}, which
     * may be an enum, a class, a record, a sealed interface or a sealed abstract class.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkEnumProperties(RoundEnvironment roundEnv) {
        checkEnclosing(
                roundEnv,
                ProtobufEnum.Constant.class,
                ProtobufProcessorErrors::enumConstantIllegalEnclosing,
                ProtobufEnum.class
        );
    }

    /**
     * Validates all elements annotated with {@link ProtobufAccessor} in the given round.
     *
     * <p>Each accessor must be enclosed by a class or record annotated with
     * {@code @ProtobufMessage}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkAnyGetters(RoundEnvironment roundEnv) {
        checkEnclosing(
                roundEnv,
                ProtobufAccessor.class,
                ProtobufProcessorErrors::accessorIllegalEnclosing,
                ProtobufMessage.class
        );
    }

    /**
     * Validates all elements annotated with {@link ProtobufMessage.UnknownFields} in the
     * given round.
     *
     * <p>Each unknown fields element must be enclosed by a type annotated with
     * {@code @ProtobufMessage}, and all referenced mixins must be valid.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkUnknownFields(RoundEnvironment roundEnv) {
        for (var property : roundEnv.getElementsAnnotatedWith(ProtobufMessage.UnknownFields.class)) {
            checkUnknownField(property);
        }
    }

    /**
     * Validates a single element annotated with {@link ProtobufMessage.UnknownFields}.
     *
     * @param property the element to validate
     */
    private void checkUnknownField(Element property) {
        var enclosingElement = types.getEnclosingTypeElement(property);
        if (enclosingElement.getAnnotation(ProtobufMessage.class) == null) {
            messages.printError(ProtobufProcessorErrors.unknownFieldsIllegalEnclosing(property), property);
            return;
        }

        var annotation = property.getAnnotation(ProtobufMessage.UnknownFields.class);
        var mixins = protobufTypes.getMixins(annotation);
        checkMixins(property, mixins);
    }

    /**
     * Validates all reserved ranges declared on {@link ProtobufMessage} and
     * {@link ProtobufEnum} annotated elements in the given round.
     *
     * <p>The range of a message must only contain valid field indexes, while the range of
     * an enum can contain any {@code int32} value. The min of a range must not exceed its
     * max.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkReservedRanges(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufMessage.class)) {
            var message = element.getAnnotation(ProtobufMessage.class);
            for (var range : message.reservedRanges()) {
                checkRange(element, range, MIN_FIELD_INDEX, MAX_FIELD_INDEX);
            }
        }
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufEnum.class)) {
            var enumeration = element.getAnnotation(ProtobufEnum.class);
            for (var range : enumeration.reservedRanges()) {
                checkRange(element, range, Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
        }
    }

    /**
     * Validates that every parameter on a {@code @TypeBuilder} method inside a
     * {@code @ProtobufMessage} type is annotated with either
     * {@code @ProtobufBuilderParameter.Field} or {@code @ProtobufBuilderParameter.Synthetic}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkBuilderParameters(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(TypeBuilder.class)) {
            if (!(element instanceof ExecutableElement method)) {
                continue;
            }

            var enclosingType = types.getEnclosingTypeElement(method);
            if (enclosingType.getAnnotation(ProtobufMessage.class) == null) {
                continue;
            }

            for (var parameter : method.getParameters()) {
                var hasFieldParam = parameter.getAnnotation(ProtobufBuilderParameter.Field.class) != null;
                var hasSyntheticParam = parameter.getAnnotation(ProtobufBuilderParameter.Synthetic.class) != null;
                if (!hasFieldParam && !hasSyntheticParam) {
                    messages.printError(ProtobufProcessorErrors.builderParameterMissingAnnotation(parameter), parameter);
                } else if (hasFieldParam && hasSyntheticParam) {
                    messages.printError(ProtobufProcessorErrors.builderParameterConflictingAnnotations(parameter), parameter);
                }
            }
        }
    }

    /**
     * Validates that {@code @ProtobufEnum.Constant} and {@code @TypeSerializer}/{@code @TypeDeserializer}
     * are not mixed in the same enum type.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkEnumConstantSerializerMixing(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufEnum.class)) {
            if (!(element instanceof TypeElement enumType)) {
                continue;
            }

            var hasConstant = false;
            var hasSerializerOrDeserializer = false;
            for (var enclosed : enumType.getEnclosedElements()) {
                if (enclosed.getAnnotation(ProtobufEnum.Constant.class) != null) {
                    hasConstant = true;
                }
                if (enclosed instanceof ExecutableElement) {
                    if (enclosed.getAnnotation(TypeSerializer.class) != null || enclosed.getAnnotation(TypeDeserializer.class) != null) {
                        hasSerializerOrDeserializer = true;
                    }
                }
            }

            if (hasConstant && hasSerializerOrDeserializer) {
                messages.printError(ProtobufProcessorErrors.enumMixedConstantsAndConverters(enumType), enumType);
            }
        }
    }

    /**
     * Validates a single reserved range annotation.
     *
     * @param element the annotated element
     * @param range the reserved range to validate
     * @param lower the lowest index the range may contain
     * @param upper the highest index the range may contain
     */
    private void checkRange(Element element, ProtobufReservedRange range, int lower, int upper) {
        if (range.min() > range.max()) {
            messages.printError(ProtobufProcessorErrors.reservedRangeInverted(range.min(), range.max()), element);
        } else if (range.min() < lower || range.max() > upper) {
            messages.printError(ProtobufProcessorErrors.reservedRangeOutOfBounds(range.min(), range.max(), lower, upper), element);
        }
    }
}

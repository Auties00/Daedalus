package com.github.auties00.daedalus.protobuf.processor.manager;

import com.github.auties00.daedalus.processor.manager.DaedalusValidationManager;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufAccessor;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.metadata.ProtobufFieldMetadata;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;

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
        return protobufTypes.isObject(type);
    }

    /**
     * Runs all validation checks against the given round environment.
     *
     * <p>This method invokes both the common checks inherited from the parent
     * class and all protobuf specific checks for messages, enums, properties,
     * accessors, unknown fields, and reserved ranges.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void runChecks(RoundEnvironment roundEnv) {
        runCommonChecks(roundEnv);
        checkMessages(roundEnv);
        checkMessageProperties(roundEnv);
        checkEnums(roundEnv);
        checkEnumProperties(roundEnv);
        checkAnyGetters(roundEnv);
        checkUnknownFields(roundEnv);
        checkReservedRanges(roundEnv);
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
                "Illegal annotation: only classes and records can be annotated with @ProtobufMessage",
                ElementKind.CLASS,
                ElementKind.RECORD
        );
    }

    /**
     * Validates all elements annotated with {@link ProtobufEnum} in the given round.
     *
     * <p>Only enum types may be annotated with {@code @ProtobufEnum}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkEnums(RoundEnvironment roundEnv) {
        checkAnnotation(
                roundEnv,
                ProtobufEnum.class,
                "Illegal annotation: only enums can be annotated with @ProtobufEnum",
                ElementKind.ENUM
        );
    }

    /**
     * Validates all elements with protobuf field annotations in the given round.
     *
     * <p>Each property must be enclosed by a type annotated with {@code @ProtobufMessage},
     * must not have an {@link ProtobufType#UNKNOWN} type, and all referenced mixins
     * must be valid.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkMessageProperties(RoundEnvironment roundEnv) {
        for (var messageElement : roundEnv.getElementsAnnotatedWith(ProtobufMessage.class)) {
            for (var enclosed : messageElement.getEnclosedElements()) {
                ProtobufFieldMetadata.of(enclosed).ifPresent(annotation -> processMessageProperty(enclosed, annotation));
            }
        }
    }

    /**
     * Validates a single element with a protobuf field annotation.
     *
     * @param property the element to validate
     * @param annotation the normalized field annotation
     */
    private void processMessageProperty(Element property, ProtobufFieldMetadata annotation) {
        var enclosingElement = getEnclosingTypeElement(property);
        if (enclosingElement.getAnnotation(ProtobufMessage.class) == null) {
            messages.printError("Illegal enclosing class: a field or method annotated with @ProtobufProperty should be enclosed by a class or record annotated with @ProtobufMessage", property);
            return;
        }

        if (annotation.type() == ProtobufType.UNKNOWN) {
            messages.printError("Illegal protobuf type: a field or method annotated with @ProtobufProperty cannot have an UNKNOWN type", property);
            return;
        }

        var mixins = protobufTypes.getMirroredTypes(annotation.mixins());
        checkMixins(property, mixins);
    }

    /**
     * Validates all elements annotated with {@link ProtobufEnum.Constant} in the given round.
     *
     * <p>Each constant must be enclosed by an enum annotated with {@code @ProtobufEnum}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkEnumProperties(RoundEnvironment roundEnv) {
        checkEnclosing(
                roundEnv,
                ProtobufEnum.Constant.class,
                "Illegal enclosing class: a field or parameter annotated with @ProtobufEnum.Constant should be enclosed by an enum annotated with @ProtobufEnum",
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
                "Illegal enclosing class: a method annotated with @ProtobufGetter should be enclosed by a class or record annotated with @ProtobufMessage",
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
        var enclosingElement = getEnclosingTypeElement(property);
        if (enclosingElement.getAnnotation(ProtobufMessage.class) == null) {
            messages.printError("Illegal enclosing class: a method or field annotated with @ProtobufUnknownFields should be enclosed by a class/record annotated with @ProtobufMessage", property);
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
     * <p>Each range must have non-negative min and max values, and the min
     * must not exceed the max.
     *
     * @param roundEnv the current annotation processing round environment
     */
    private void checkReservedRanges(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufMessage.class)) {
            var message = element.getAnnotation(ProtobufMessage.class);
            for (var range : message.reservedRanges()) {
                checkRange(element, range);
            }
        }
        for (var element : roundEnv.getElementsAnnotatedWith(ProtobufEnum.class)) {
            var enumeration = element.getAnnotation(ProtobufEnum.class);
            for (var range : enumeration.reservedRanges()) {
                checkRange(element, range);
            }
        }
    }

    /**
     * Validates a single reserved range annotation.
     *
     * @param element the annotated element
     * @param range the reserved range to validate
     */
    private void checkRange(Element element, ProtobufReservedRange range) {
        if (range.min() < 0) {
            messages.printError("Illegal annotation: min must be positive", element);
        }

        if (range.max() < 0) {
            messages.printError("Illegal annotation: max must be positive", element);
        }

        if (range.min() > range.max()) {
            messages.printError("Illegal annotation: max must be equal or bigger than min", element);
        }
    }

    /**
     * Returns whether the given required property is valid.
     *
     * <p>Required properties cannot be primitives because there is no way to
     * distinguish a missing value from a default value for primitive types.
     *
     * @param variableElement the property element to validate
     * @return {@code true} if the required property is valid
     */
    public boolean isValidRequiredProperty(Element variableElement) {
        if (variableElement.asType().getKind().isPrimitive()) {
            messages.printError("Required properties cannot be primitives", variableElement);
            return false;
        }

        return true;
    }

    /**
     * Returns whether the given packed property is valid.
     *
     * <p>Only properties whose type is assignable to {@link Collection} can be
     * packed. If the property is not packed, it is always considered valid.
     *
     * @param variableElement the property element to validate
     * @param propertyAnnotation the normalized field annotation to check
     * @return {@code true} if the packed property is valid
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValidPackedProperty(Element variableElement, ProtobufFieldMetadata propertyAnnotation) {
        if (!propertyAnnotation.packed() || types.isAssignable(variableElement.asType(), Collection.class)) {
            return true;
        }

        messages.printError("Only scalar properties can be packed", variableElement);
        return false;
    }
}

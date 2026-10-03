package com.github.auties00.daedalus.protobuf.processor.generator.builder;

import com.github.auties00.daedalus.processor.generator.builder.DaedalusCustomBuilderGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusBuilderElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;

/**
 * A protobuf-specific custom builder generator.
 *
 * <p>This generator extends the common custom builder generation with protobuf-specific
 * build method handling for unknown fields and deserializer delegation.
 */
public class ProtobufBuilderMethodGenerator extends DaedalusCustomBuilderGenerator {

    /**
     * Constructs a new protobuf custom builder generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     */
    public ProtobufBuilderMethodGenerator(Filer filer, DaedalusTypeManager types) {
        super(filer, types);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles protobuf-specific unknown fields and deserializer delegation.
     */
    @Override
    protected void createBuildStatement(
            MethodSpec.Builder buildMethodBuilder,
            DaedalusTypeElement typeElement,
            DaedalusBuilderElement builderElement,
            ArrayList<String> invocationArgs
    ) {
        var resultType = TypeName.get(typeElement.typeElement().asType());
        var invocationArgsJoined = String.join(", ", invocationArgs);
        var protobufMessage = (ProtobufMessageElement) typeElement;
        var unknownFieldsValue = protobufMessage.unknownFieldsElement().isEmpty()
                ? ""
                : ", " + ProtobufBuilderTypeGenerator.UNKNOWN_FIELDS_NAME;
        var delegate = builderElement.delegate();
        if (delegate.getKind() == ElementKind.CONSTRUCTOR) {
            buildMethodBuilder.addStatement("return new $T($L$L)", resultType, invocationArgsJoined, unknownFieldsValue);
        } else {
            var methodName = delegate.getSimpleName();
            buildMethodBuilder.addStatement("return $T.$L($L$L)", resultType, methodName, invocationArgsJoined, unknownFieldsValue);
        }
    }
}

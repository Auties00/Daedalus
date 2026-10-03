package com.github.auties00.daedalus.processor.generator.converter;

import com.github.auties00.daedalus.processor.diagnostic.DaedalusConverterErrors;
import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.util.DaedalusMixinInstanceUtils;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

/**
 * A generator that creates the holder class of every mixin whose instance is created by a
 * member annotated with {@link TypeMixin.Factory}.
 *
 * <p>The holder class is named as described by
 * {@link DaedalusMixinInstanceUtils#getHolderSimpleName(TypeElement)} and declared in the
 * package of the mixin, so that it can invoke a constructor or a static method that is not
 * public. It keeps the instance in a static {@link LazyConstant}, which invokes the member
 * the first time the instance is requested, and again on a later request if the member
 * throws. The instance converters of the mixin are invoked on
 * {@code {HolderName}.INSTANCE.get()}.
 *
 * <p>A member that is not valid, as reported by the validation of the mixin, is skipped, as
 * is any member of a mixin after its first one annotated with {@link TypeMixin.Instance} or
 * {@link TypeMixin.Factory}.
 */
public final class DaedalusMixinInstanceHolderGenerator extends DaedalusClassGenerator {
    private final DaedalusLogManager messages;

    /**
     * Constructs a new holder generator.
     *
     * @param filer the filer for creating source files
     * @param messages the compiler diagnostics utility
     */
    public DaedalusMixinInstanceHolderGenerator(Filer filer, DaedalusLogManager messages) {
        super(filer);
        this.messages = messages;
    }

    /**
     * Generates the holder class of every mixin of the given round whose instance is
     * created by a member annotated with {@link TypeMixin.Factory}.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void generate(RoundEnvironment roundEnv) {
        for (var member : roundEnv.getElementsAnnotatedWith(TypeMixin.Factory.class)) {
            if (member instanceof ExecutableElement factory
                    && factory.getEnclosingElement() instanceof TypeElement mixin
                    && isGeneratable(factory, mixin)) {
                generateHolder(factory, mixin);
            }
        }
    }

    /**
     * Returns whether a holder class can be generated for the given factory, which is the
     * case if it is the first member of its mixin that provides or creates its instance,
     * and if it is a constructor or a static method that takes no parameters and is not
     * private.
     *
     * @param factory the member annotated with {@link TypeMixin.Factory}
     * @param mixin the type that declares the member
     * @return {@code true} if the holder class can be generated
     */
    private boolean isGeneratable(ExecutableElement factory, TypeElement mixin) {
        var accessors = DaedalusMixinInstanceUtils.findInstanceAccessors(mixin);
        return DaedalusMixinInstanceUtils.isMixin(mixin)
                && !accessors.isEmpty()
                && accessors.getFirst().equals(factory)
                && factory.getParameters().isEmpty()
                && !factory.getModifiers().contains(Modifier.PRIVATE)
                && (factory.getKind() == ElementKind.CONSTRUCTOR
                ? !mixin.getModifiers().contains(Modifier.ABSTRACT)
                : factory.getModifiers().contains(Modifier.STATIC));
    }

    /**
     * Writes the holder class of the given mixin, which keeps the instance created by the
     * given factory.
     *
     * @param factory the constructor or the static method that creates the instance
     * @param mixin the mixin
     */
    private void generateHolder(ExecutableElement factory, TypeElement mixin) {
        var mixinName = ClassName.get(mixin);
        var holderName = DaedalusMixinInstanceUtils.getHolderSimpleName(mixin);
        var supplier = factory.getKind() == ElementKind.CONSTRUCTOR
                ? CodeBlock.of("$T::new", mixinName)
                : CodeBlock.of("$T::$L", mixinName, factory.getSimpleName());
        var instance = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(LazyConstant.class), mixinName), DaedalusMixinInstanceUtils.HOLDER_FIELD)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.of($L)", LazyConstant.class, supplier)
                .build();
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("throw new $T($S)", UnsupportedOperationException.class, holderName + " is a holder class and cannot be instantiated")
                .build();
        var holder = TypeSpec.classBuilder(holderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(instance)
                .addMethod(constructor)
                .addOriginatingElement(mixin)
                .build();
        var javaFile = JavaFile.builder(DaedalusMixinInstanceUtils.getPackageName(mixin), holder)
                .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException exception) {
            messages.printError(DaedalusConverterErrors.holderGenerationFailed(DaedalusMixinInstanceUtils.getHolderQualifiedName(mixin), mixin.getQualifiedName().toString(), exception), (Element) factory);
        }
    }
}

package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * Enum type definition.
 *
 * <p>New usages of this message as an alternative to EnumDescriptorProto are
 * strongly discouraged. This message does not reliability preserve all
 * information necessary to model the schema and preserve semantics. Instead
 * make use of FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.Enum}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Enum {

    /**
     * Enum type name.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * Enum value definitions.
     *
     * <p><code>repeated EnumValue enumvalue = 2;</code>
     */
    @ProtobufMessage.MessageField(index = 2)
    List<EnumValue> enumvalue;

    /**
     * Protocol buffer options.
     *
     * <p><code>repeated Option options = 3;</code>
     */
    @ProtobufMessage.MessageField(index = 3)
    List<Option> options;

    /**
     * The source context.
     *
     * <p><code>SourceContext source_context = 4;</code>
     */
    @ProtobufMessage.MessageField(index = 4)
    SourceContext sourceContext;

    /**
     * The source syntax.
     *
     * <p><code>Syntax syntax = 5;</code>
     */
    @ProtobufMessage.EnumField(index = 5)
    Syntax syntax;

    /**
     * The source edition string, only valid when syntax is SYNTAX_EDITIONS.
     *
     * <p><code>string edition = 6;</code>
     */
    @ProtobufMessage.StringField(index = 6)
    String edition;

    /**
     * Constructs a new {@code Enum} with the specified field values.
     *
     * @param name the value of the {@code name} field
     * @param enumvalue the values of the repeated {@code enumvalue} field
     * @param options the values of the repeated {@code options} field
     * @param sourceContext the value of the {@code source_context} field, or {@code null} if the
     *                      field is not set
     * @param syntax the value of the {@code syntax} field
     * @param edition the value of the {@code edition} field
     */
    Enum(
            String name,
            List<EnumValue> enumvalue,
            List<Option> options,
            SourceContext sourceContext,
            Syntax syntax,
            String edition
    ) {
        this.name = name;
        this.enumvalue = enumvalue;
        this.options = options;
        this.sourceContext = sourceContext;
        this.syntax = syntax;
        this.edition = edition;
    }

    /**
     * Returns the value of the {@code name} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code name} field
     */
    public String name() {
        return name;
    }

    /**
     * Returns an unmodifiable view of the values of the repeated
     * {@code enumvalue} field.
     *
     * @return an unmodifiable view of the values of the {@code enumvalue} field,
     *         in encounter order
     */
    public SequencedCollection<EnumValue> enumvalue() {
        return Collections.unmodifiableSequencedCollection(enumvalue);
    }

    /**
     * Returns an unmodifiable view of the values of the repeated
     * {@code options} field.
     *
     * @return an unmodifiable view of the values of the {@code options} field,
     *         in encounter order
     */
    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }

    /**
     * Returns the value of the {@code source_context} field, if present.
     *
     * @return an {@code Optional} containing the value of the {@code source_context}
     *         field, or an empty {@code Optional} if the field is not set
     */
    public Optional<SourceContext> sourceContext() {
        return Optional.ofNullable(sourceContext);
    }

    /**
     * Returns the value of the {@code syntax} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code syntax} field
     */
    public Syntax syntax() {
        return syntax;
    }

    /**
     * Returns the value of the {@code edition} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code edition} field
     */
    public String edition() {
        return edition;
    }
}

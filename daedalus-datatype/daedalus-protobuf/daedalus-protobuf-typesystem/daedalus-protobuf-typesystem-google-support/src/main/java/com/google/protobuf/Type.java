package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * A protocol buffer message type.
 *
 * <p>New usages of this message as an alternative to DescriptorProto are strongly
 * discouraged. This message does not reliability preserve all information
 * necessary to model the schema and preserve semantics. Instead make use of
 * FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.Type}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Type {

    /**
     * The fully qualified message name.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * The list of fields.
     *
     * <p><code>repeated Field fields = 2;</code>
     */
    @ProtobufMessage.MessageField(index = 2)
    List<Field> fields;

    /**
     * The list of types appearing in {@code oneof} definitions in this type.
     *
     * <p><code>repeated string oneofs = 3;</code>
     */
    @ProtobufMessage.StringField(index = 3)
    List<String> oneofs;

    /**
     * The protocol buffer options.
     *
     * <p><code>repeated Option options = 4;</code>
     */
    @ProtobufMessage.MessageField(index = 4)
    List<Option> options;

    /**
     * The source context.
     *
     * <p><code>SourceContext source_context = 5;</code>
     */
    @ProtobufMessage.MessageField(index = 5)
    SourceContext sourceContext;

    /**
     * The source syntax.
     *
     * <p><code>Syntax syntax = 6;</code>
     */
    @ProtobufMessage.EnumField(index = 6)
    Syntax syntax;

    /**
     * The source edition string, only valid when syntax is SYNTAX_EDITIONS.
     *
     * <p><code>string edition = 7;</code>
     */
    @ProtobufMessage.StringField(index = 7)
    String edition;

    /**
     * Constructs a new {@code Type} with the specified field values.
     *
     * @param name the value of the {@code name} field
     * @param fields the values of the repeated {@code fields} field
     * @param oneofs the values of the repeated {@code oneofs} field
     * @param options the values of the repeated {@code options} field
     * @param sourceContext the value of the {@code source_context} field, or {@code null} if the
     *                      field is not set
     * @param syntax the value of the {@code syntax} field
     * @param edition the value of the {@code edition} field
     */
    Type(
            String name,
            List<Field> fields,
            List<String> oneofs,
            List<Option> options,
            SourceContext sourceContext,
            Syntax syntax,
            String edition
    ) {
        this.name = name;
        this.fields = fields;
        this.oneofs = oneofs;
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
     * {@code fields} field.
     *
     * @return an unmodifiable view of the values of the {@code fields} field,
     *         in encounter order
     */
    public SequencedCollection<Field> fields() {
        return Collections.unmodifiableSequencedCollection(fields);
    }

    /**
     * Returns an unmodifiable view of the values of the repeated
     * {@code oneofs} field.
     *
     * @return an unmodifiable view of the values of the {@code oneofs} field,
     *         in encounter order
     */
    public SequencedCollection<String> oneofs() {
        return Collections.unmodifiableSequencedCollection(oneofs);
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

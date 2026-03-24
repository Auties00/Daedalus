package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * A protocol buffer message type.
 *
 * <p>New usages of this message as an alternative to DescriptorProto are strongly
 * discouraged. This message does not reliability preserve all information
 * necessary to model the schema and preserve semantics. Instead make use of
 * FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code google.protobuf.Type}
 */
@ProtobufMessage
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

    public String name() {
        return name;
    }

    public SequencedCollection<Field> fields() {
        return Collections.unmodifiableSequencedCollection(fields);
    }

    public SequencedCollection<String> oneofs() {
        return Collections.unmodifiableSequencedCollection(oneofs);
    }

    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }

    public SourceContext sourceContext() {
        return sourceContext;
    }

    public Syntax syntax() {
        return syntax;
    }

    public String edition() {
        return edition;
    }
}

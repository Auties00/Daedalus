package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
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
@ProtobufMessage
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

    public String name() {
        return name;
    }

    public SequencedCollection<EnumValue> enumvalue() {
        return Collections.unmodifiableSequencedCollection(enumvalue);
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

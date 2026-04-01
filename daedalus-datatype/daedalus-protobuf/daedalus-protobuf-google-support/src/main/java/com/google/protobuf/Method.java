package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Method represents a method of an API interface.
 *
 * <p>New usages of this message as an alternative to MethodDescriptorProto are
 * strongly discouraged. This message does not reliability preserve all
 * information necessary to model the schema and preserve semantics. Instead
 * make use of FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.Method}
 */
@ProtobufMessage
public final class Method {

    /**
     * The simple name of this method.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * A URL of the input message type.
     *
     * <p><code>string request_type_url = 2;</code>
     */
    @ProtobufMessage.StringField(index = 2)
    String requestTypeUrl;

    /**
     * If true, the request is streamed.
     *
     * <p><code>bool request_streaming = 3;</code>
     */
    @ProtobufMessage.BoolField(index = 3)
    boolean requestStreaming;

    /**
     * The URL of the output message type.
     *
     * <p><code>string response_type_url = 4;</code>
     */
    @ProtobufMessage.StringField(index = 4)
    String responseTypeUrl;

    /**
     * If true, the response is streamed.
     *
     * <p><code>bool response_streaming = 5;</code>
     */
    @ProtobufMessage.BoolField(index = 5)
    boolean responseStreaming;

    /**
     * Any metadata attached to the method.
     *
     * <p><code>repeated Option options = 6;</code>
     */
    @ProtobufMessage.MessageField(index = 6)
    List<Option> options;

    /**
     * The source syntax of this method.
     *
     * <p>This field should be ignored, instead the syntax should be inherited from
     * Api. This is similar to Field and EnumValue.
     *
     * <p><code>Syntax syntax = 7;</code>
     */
    @Deprecated
    @ProtobufMessage.EnumField(index = 7)
    Syntax syntax;

    /**
     * The source edition string, only valid when syntax is SYNTAX_EDITIONS.
     *
     * <p>This field should be ignored, instead the edition should be inherited from
     * Api. This is similar to Field and EnumValue.
     *
     * <p><code>string edition = 8;</code>
     */
    @Deprecated
    @ProtobufMessage.StringField(index = 8)
    String edition;

    Method(
            String name,
            String requestTypeUrl,
            boolean requestStreaming,
            String responseTypeUrl,
            boolean responseStreaming,
            List<Option> options,
            Syntax syntax,
            String edition
    ) {
        this.name = name;
        this.requestTypeUrl = requestTypeUrl;
        this.requestStreaming = requestStreaming;
        this.responseTypeUrl = responseTypeUrl;
        this.responseStreaming = responseStreaming;
        this.options = options;
        this.syntax = syntax;
        this.edition = edition;
    }

    public String name() {
        return name;
    }

    public String requestTypeUrl() {
        return requestTypeUrl;
    }

    public boolean requestStreaming() {
        return requestStreaming;
    }

    public String responseTypeUrl() {
        return responseTypeUrl;
    }

    public boolean responseStreaming() {
        return responseStreaming;
    }

    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }

    @Deprecated
    public Syntax syntax() {
        return syntax;
    }

    @Deprecated
    public String edition() {
        return edition;
    }
}

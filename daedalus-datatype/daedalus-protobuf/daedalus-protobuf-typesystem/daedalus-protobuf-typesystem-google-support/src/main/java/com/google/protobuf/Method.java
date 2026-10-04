package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
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
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
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

    /**
     * Constructs a new {@code Method} with the specified field values.
     *
     * @param name the value of the {@code name} field
     * @param requestTypeUrl the value of the {@code request_type_url} field
     * @param requestStreaming the value of the {@code request_streaming} field
     * @param responseTypeUrl the value of the {@code response_type_url} field
     * @param responseStreaming the value of the {@code response_streaming} field
     * @param options the values of the repeated {@code options} field
     * @param syntax the value of the {@code syntax} field
     * @param edition the value of the {@code edition} field
     */
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
     * Returns the value of the {@code request_type_url} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code request_type_url} field
     */
    public String requestTypeUrl() {
        return requestTypeUrl;
    }

    /**
     * Returns the value of the {@code request_streaming} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code request_streaming} field
     */
    public boolean requestStreaming() {
        return requestStreaming;
    }

    /**
     * Returns the value of the {@code response_type_url} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code response_type_url} field
     */
    public String responseTypeUrl() {
        return responseTypeUrl;
    }

    /**
     * Returns the value of the {@code response_streaming} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code response_streaming} field
     */
    public boolean responseStreaming() {
        return responseStreaming;
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
     * Returns the value of the {@code syntax} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code syntax} field
     * @deprecated The {@code .proto} declaration marks this field as deprecated.
     */
    @Deprecated
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
     * @deprecated The {@code .proto} declaration marks this field as deprecated.
     */
    @Deprecated
    public String edition() {
        return edition;
    }
}

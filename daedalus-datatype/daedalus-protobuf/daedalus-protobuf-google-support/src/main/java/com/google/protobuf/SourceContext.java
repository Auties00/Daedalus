package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * {@code SourceContext} represents information about the source of a
 * protobuf element, like the file in which it is defined.
 *
 * <p>Protobuf type {@code com.google.protobuf.SourceContext}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class SourceContext {

    /**
     * The path-qualified name of the .proto file that contained the associated
     * protobuf element.  For example: {@code "google/protobuf/source_context.proto"}.
     *
     * <p><code>string file_name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String fileName;

    /**
     * Constructs a new {@code SourceContext} with the specified field values.
     *
     * @param fileName the value of the {@code file_name} field
     */
    SourceContext(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the value of the {@code file_name} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code file_name} field
     */
    public String fileName() {
        return fileName;
    }
}

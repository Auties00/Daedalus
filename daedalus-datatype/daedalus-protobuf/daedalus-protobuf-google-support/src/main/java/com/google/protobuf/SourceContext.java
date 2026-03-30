package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * {@code SourceContext} represents information about the source of a
 * protobuf element, like the file in which it is defined.
 *
 * <p>Protobuf type {@code google.protobuf.SourceContext}
 */
@ProtobufMessage
public final class SourceContext {

    /**
     * The path-qualified name of the .proto file that contained the associated
     * protobuf element.  For example: {@code "google/protobuf/source_context.proto"}.
     *
     * <p><code>string file_name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String fileName;

    SourceContext(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}

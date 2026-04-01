package com.github.auties00.daedalus.protobuf.compiler.tree;

import java.util.Collections;
import java.util.List;

/**
 * Represents the name component of a Protocol Buffer option as a list of segments.
 * <p>
 * Option names follow the grammar:
 * </p>
 * <pre>{@code
 * optionName = segment { "." segment }
 * segment    = simpleName | extensionName
 * simpleName = ident
 * extensionName = "(" [ "." ] fullIdent ")"
 * }</pre>
 * <h2>Examples:</h2>
 * <pre>{@code
 * option java_package = "com.example";                          // [java_package]
 * option (my.extension) = "value";                              // [(my.extension)]
 * option (file_option).nested.field = 42;                       // [(file_option), nested, field]
 * option features.field_presence = EXPLICIT;                    // [features, field_presence]
 * option features.(pb.java).legacy_closed_enum = false;         // [features, (pb.java), legacy_closed_enum]
 * }</pre>
 *
 * @param segments the ordered list of segments that make up the option name
 */
public record ProtobufOptionName(List<ProtobufOptionNameSegment> segments) {
    /**
     * Returns an unmodifiable view of the segments.
     *
     * @return unmodifiable list of segments
     */
    @Override
    public List<ProtobufOptionNameSegment> segments() {
        return Collections.unmodifiableList(segments);
    }

    @Override
    public String toString() {
        var result = new StringBuilder();
        for (var i = 0; i < segments.size(); i++) {
            if (i > 0) {
                result.append('.');
            }
            result.append(segments.get(i));
        }
        return result.toString();
    }
}
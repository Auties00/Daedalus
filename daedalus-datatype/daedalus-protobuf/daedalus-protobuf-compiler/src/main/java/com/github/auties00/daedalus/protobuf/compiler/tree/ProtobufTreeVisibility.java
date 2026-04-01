package com.github.auties00.daedalus.protobuf.compiler.tree;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of symbol visibility modifiers introduced in Protocol Buffers Edition 2024.
 * <p>
 * Visibility modifiers control whether a message or enum type is exported from its defining
 * file. They prefix {@code message} and {@code enum} declarations:
 * </p>
 * <pre>{@code
 * export message PublicMessage { ... }
 * local message PrivateMessage { ... }
 * }</pre>
 * <p>
 * The default visibility is determined by the file level
 * {@code default_symbol_visibility} feature. In Edition 2024, the default is
 * {@code EXPORT_TOP_LEVEL}, meaning top level symbols are exported while nested
 * symbols are local.
 * </p>
 */
public enum ProtobufTreeVisibility {
    /**
     * No explicit visibility modifier, uses the file level default.
     */
    NONE(""),

    /**
     * Exported visibility, the type is visible to importers of the file.
     */
    EXPORT("export"),

    /**
     * Local visibility, the type is only visible within the defining file.
     */
    LOCAL("local");

    private final String token;

    ProtobufTreeVisibility(String token) {
        this.token = token;
    }

    /**
     * Returns the keyword token for this visibility modifier.
     *
     * @return the token string, or an empty string for {@link #NONE}
     */
    public String token() {
        return token;
    }

    private static final Map<String, ProtobufTreeVisibility> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a visibility modifier by its token string.
     *
     * @param name the token string
     * @return optional containing the visibility, or empty if not found
     */
    public static Optional<ProtobufTreeVisibility> of(String name) {
        return Optional.ofNullable(VALUES.get(name));
    }
}
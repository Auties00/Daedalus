package com.github.auties00.daedalus.typesystem.jdk.support.nio.charset;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Charset}.
 *
 * <p>{@code Charset} is encoded as the {@code String} that contains its {@linkplain Charset#name() canonical name}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CharsetMixin {
    private CharsetMixin() {
        throw new UnsupportedOperationException("CharsetMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the canonical name of the given charset.
     *
     * @param value the charset, or {@code null}
     * @return the canonical name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Charset value) {
        return value == null ? null : value.name();
    }

    /**
     * Returns the charset with the given name or alias.
     *
     * <p>The canonical name of a standard charset, which is what the serializer writes, is
     * resolved to the corresponding constant of {@link StandardCharsets}. Any other name is
     * resolved with {@link Charset#forName(String)}, as the lookup of a charset provider
     * relies on reflection that no other API avoids.
     *
     * @param value the name or alias, or {@code null}
     * @return the charset, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid or supported charset name
     */
    @TypeDeserializer
    public static Charset ofCharset(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "UTF-8" -> StandardCharsets.UTF_8;
            case "US-ASCII" -> StandardCharsets.US_ASCII;
            case "ISO-8859-1" -> StandardCharsets.ISO_8859_1;
            case "UTF-16" -> StandardCharsets.UTF_16;
            case "UTF-16BE" -> StandardCharsets.UTF_16BE;
            case "UTF-16LE" -> StandardCharsets.UTF_16LE;
            default -> {
                try {
                    yield Charset.forName(value);
                } catch (IllegalArgumentException exception) {
                    throw TypeDeserializationException.malformedValue(Charset.class, value, exception);
                }
            }
        };
    }
}

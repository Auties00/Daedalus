package com.github.auties00.daedalus.typesystem.jdk.support.nio.file;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Path}.
 *
 * <p>{@code Path} is encoded as the {@code String} that contains its string form. It is deserialized with the
 * {@linkplain java.nio.file.FileSystems#getDefault() default file system}, so its string
 * form uses the separator of the platform that serialized it.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PathMixin {
    private PathMixin() {
        throw new UnsupportedOperationException("PathMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given path.
     *
     * @param value the path, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Path value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the path of the default file system with the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the path, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid path
     */
    @TypeDeserializer
    public static Path ofPath(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Path.of(value);
        } catch (InvalidPathException exception) {
            throw TypeDeserializationException.malformedValue(Path.class, value, exception);
        }
    }
}

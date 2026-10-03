package com.github.auties00.daedalus.typesystem.jdk.support.io;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.io.File;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link File}.
 *
 * <p>{@code File} is encoded as the {@code String} that contains its path.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class FileMixin {
    private FileMixin() {
        throw new UnsupportedOperationException("FileMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given file.
     *
     * @param value the file, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(File value) {
        return value == null ? null : value.getPath();
    }

    /**
     * Returns the file with the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the file, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static File ofFile(String value) {
        return value == null ? null : new File(value);
    }
}

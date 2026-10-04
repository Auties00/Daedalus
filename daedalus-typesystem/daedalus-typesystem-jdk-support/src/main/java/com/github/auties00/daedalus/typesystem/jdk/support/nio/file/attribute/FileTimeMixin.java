package com.github.auties00.daedalus.typesystem.jdk.support.nio.file.attribute;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;
import com.github.auties00.daedalus.typesystem.jdk.support.time.InstantMixin;

import java.nio.file.attribute.FileTime;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link FileTime}.
 *
 * <p>A file time is encoded as the {@code String} that contains its ISO 8601 form, such
 * as {@code 2024-01-31T10:15:30Z}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} since the epoch.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @FileTimeMixinParameters}, which supplies the unit.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class FileTimeMixin {
    private FileTimeMixin() {
        throw new UnsupportedOperationException("FileTimeMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given file time.
     *
     * @param value the file time, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(FileTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the file time represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the file time, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid instant
     */
    @TypeDeserializer
    public static FileTime ofFileTime(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : FileTime.from(Instant.parse(value));
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(FileTime.class, value, exception);
        }
    }

    /**
     * Returns the time elapsed from the epoch to the given file time, as a count of the
     * given unit.
     *
     * @param value the file time, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    @TypeSerializer
    public static Long toLong(FileTime value, TimeUnit unit) throws TypeSerializationException {
        return value == null ? null : InstantMixin.toLong(value.toInstant(), unit);
    }

    /**
     * Returns the file time that follows the epoch by the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the file time, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static FileTime ofFileTime(Long value, TimeUnit unit) {
        return value == null ? null : FileTime.from(value, unit);
    }
}

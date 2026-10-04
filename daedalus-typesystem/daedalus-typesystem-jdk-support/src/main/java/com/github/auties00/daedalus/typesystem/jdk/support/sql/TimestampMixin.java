package com.github.auties00.daedalus.typesystem.jdk.support.sql;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;
import com.github.auties00.daedalus.typesystem.jdk.support.time.InstantMixin;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Timestamp}.
 *
 * <p>A timestamp represents an instant with a precision of a nanosecond, and is encoded
 * as the {@code String} that contains the ISO 8601 form of that instant, such as
 * {@code 2024-01-31T10:15:30.123456789Z}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} since the epoch.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @TimestampMixinParameters}, which supplies the unit.
 *
 * <p>This mixin only requires the {@code java.sql} module statically, so it can only be
 * used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class TimestampMixin {
    private TimestampMixin() {
        throw new UnsupportedOperationException("TimestampMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the instant represented by the given timestamp.
     *
     * @param value the timestamp, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Timestamp value) {
        return value == null ? null : value.toInstant().toString();
    }

    /**
     * Returns the timestamp that represents the instant with the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the timestamp, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid instant, or is
     *         out of the range of a timestamp
     */
    @TypeDeserializer
    public static Timestamp ofTimestamp(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Timestamp.from(Instant.parse(value));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Timestamp.class, value, exception);
        }
    }

    /**
     * Returns the time elapsed from the epoch to the given timestamp, as a count of the
     * given unit.
     *
     * @param value the timestamp, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    @TypeSerializer
    public static Long toLong(Timestamp value, TimeUnit unit) throws TypeSerializationException {
        return value == null ? null : InstantMixin.toLong(value.toInstant(), unit);
    }

    /**
     * Returns the timestamp that follows the epoch by the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the timestamp, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the timestamp is out of the supported range
     */
    @TypeDeserializer
    public static Timestamp ofTimestamp(Long value, TimeUnit unit) throws TypeDeserializationException {
        try {
            return value == null ? null : Timestamp.from(InstantMixin.ofInstant(value, unit));
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Timestamp.class, value, exception);
        }
    }
}

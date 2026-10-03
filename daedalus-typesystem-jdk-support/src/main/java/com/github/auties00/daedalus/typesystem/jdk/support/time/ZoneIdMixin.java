package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for the time zone
 * identifiers of the {@code java.time} package: {@link ZoneId} and {@link ZoneOffset}.
 *
 * <p>A time zone is encoded as the {@code String} that contains its identifier, such as
 * {@code Europe/Paris} or {@code +02:00}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ZoneIdMixin {
    private ZoneIdMixin() {
        throw new UnsupportedOperationException("ZoneIdMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the identifier of the given time zone.
     *
     * @param value the time zone, or {@code null}
     * @return the identifier, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ZoneId value) {
        return value == null ? null : value.getId();
    }

    /**
     * Returns the time zone with the given identifier.
     *
     * @param value the identifier, or {@code null}
     * @return the time zone, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid or known identifier
     */
    @TypeDeserializer
    public static ZoneId ofZoneId(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ZoneId.of(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(ZoneId.class, value, exception);
        }
    }

    /**
     * Returns the identifier of the given offset, such as {@code +02:00}.
     *
     * @param value the offset, or {@code null}
     * @return the identifier, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ZoneOffset value) {
        return value == null ? null : value.getId();
    }

    /**
     * Returns the offset with the given identifier.
     *
     * @param value the identifier, or {@code null}
     * @return the offset, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid offset
     */
    @TypeDeserializer
    public static ZoneOffset ofZoneOffset(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ZoneOffset.of(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(ZoneOffset.class, value, exception);
        }
    }
}

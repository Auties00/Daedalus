package com.github.auties00.daedalus.typesystem.jdk.support.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link TimeZone}.
 *
 * <p>{@code TimeZone} is encoded as the {@code String} that contains its identifier, such as
 * {@code Europe/Paris}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class TimeZoneMixin {
    private TimeZoneMixin() {
        throw new UnsupportedOperationException("TimeZoneMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the identifier of the given legacy time zone.
     *
     * @param value the time zone, or {@code null}
     * @return the identifier, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(TimeZone value) {
        return value == null ? null : value.getID();
    }

    /**
     * Returns the legacy time zone with the given identifier.
     *
     * <p>The identifier is validated with {@link ZoneId#of(String, java.util.Map)}, using
     * {@link ZoneId#SHORT_IDS} to accept the three letter identifiers supported by
     * {@code TimeZone}, as {@link TimeZone#getTimeZone(String)} silently returns the
     * {@code GMT} time zone for an unknown identifier.
     *
     * @param value the identifier, or {@code null}
     * @return the time zone, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid or known identifier
     */
    @TypeDeserializer
    public static TimeZone ofTimeZone(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : TimeZone.getTimeZone(ZoneId.of(value, ZoneId.SHORT_IDS));
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(TimeZone.class, value, exception);
        }
    }
}

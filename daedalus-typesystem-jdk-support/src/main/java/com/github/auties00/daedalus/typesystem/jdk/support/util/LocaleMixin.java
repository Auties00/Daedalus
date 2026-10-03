package com.github.auties00.daedalus.typesystem.jdk.support.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.util.IllformedLocaleException;
import java.util.Locale;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Locale}.
 *
 * <p>{@code Locale} is encoded as the {@code String} that contains its IETF BCP 47 language tag, such as
 * {@code en-US}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LocaleMixin {
    private LocaleMixin() {
        throw new UnsupportedOperationException("LocaleMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the IETF BCP 47 language tag of the given locale.
     *
     * @param value the locale, or {@code null}
     * @return the language tag, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Locale value) {
        return value == null ? null : value.toLanguageTag();
    }

    /**
     * Returns the locale with the given IETF BCP 47 language tag.
     *
     * <p>Unlike {@link Locale#forLanguageTag(String)}, which returns the undetermined
     * locale for an ill formed tag, this method rejects it.
     *
     * @param value the language tag, or {@code null}
     * @return the locale, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a well formed language tag
     */
    @TypeDeserializer
    public static Locale ofLocale(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new Locale.Builder().setLanguageTag(value).build();
        } catch (IllformedLocaleException exception) {
            throw TypeDeserializationException.malformedValue(Locale.class, value, exception);
        }
    }
}

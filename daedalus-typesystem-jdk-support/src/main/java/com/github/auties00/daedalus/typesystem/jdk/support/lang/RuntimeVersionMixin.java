package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Runtime.Version}.
 *
 * <p>{@code Runtime.Version} is encoded as the {@code String} that contains its string form, such as
 * {@code 21.0.2+13}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class RuntimeVersionMixin {
    private RuntimeVersionMixin() {
        throw new UnsupportedOperationException("RuntimeVersionMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given runtime version.
     *
     * @param value the runtime version, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Runtime.Version value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the runtime version represented by the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the runtime version, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid runtime version
     */
    @TypeDeserializer
    public static Runtime.Version ofRuntimeVersion(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Runtime.Version.parse(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Runtime.Version.class, value, exception);
        }
    }
}

package com.github.auties00.daedalus.typesystem.jdk.support.javax.naming;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link CompositeName}.
 *
 * <p>{@code CompositeName} is encoded as the {@code String} that contains its string form.
 *
 * <p>This mixin only requires the {@code java.naming} module statically, so it can only
 * be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CompositeNameMixin {
    private CompositeNameMixin() {
        throw new UnsupportedOperationException("CompositeNameMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given composite name.
     *
     * @param value the composite name, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(CompositeName value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the composite name with the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the composite name, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid composite name
     */
    @TypeDeserializer
    public static CompositeName ofCompositeName(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new CompositeName(value);
        } catch (InvalidNameException exception) {
            throw TypeDeserializationException.malformedValue(CompositeName.class, value, exception);
        }
    }
}

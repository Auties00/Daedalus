package com.github.auties00.daedalus.typesystem.jdk.support.javax.xml.namespace;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.xml.namespace.QName;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link QName}.
 *
 * <p>{@code QName} is encoded as the {@code String} that contains the string returned by {@link QName#toString()},
 * which contains its namespace URI in curly braces followed by its local part.
 *
 * <p>This mixin only requires the {@code java.xml} module statically, so it can only be
 * used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class QNameMixin {
    private QNameMixin() {
        throw new UnsupportedOperationException("QNameMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given qualified name.
     *
     * @param value the qualified name, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(QName value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the qualified name represented by the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the qualified name, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid qualified name
     */
    @TypeDeserializer
    public static QName ofQName(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : QName.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(QName.class, value, exception);
        }
    }
}

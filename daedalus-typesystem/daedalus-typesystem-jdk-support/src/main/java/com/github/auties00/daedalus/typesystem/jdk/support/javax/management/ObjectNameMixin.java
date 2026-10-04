package com.github.auties00.daedalus.typesystem.jdk.support.javax.management;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ObjectName}.
 *
 * <p>{@code ObjectName} is encoded as the {@code String} that contains its
 * {@linkplain ObjectName#getCanonicalName() canonical form}.
 *
 * <p>This mixin only requires the {@code java.management} module statically, so it can
 * only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ObjectNameMixin {
    private ObjectNameMixin() {
        throw new UnsupportedOperationException("ObjectNameMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the canonical form of the given object name.
     *
     * @param value the object name, or {@code null}
     * @return the canonical form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ObjectName value) {
        return value == null ? null : value.getCanonicalName();
    }

    /**
     * Returns the object name with the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the object name, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid object name
     */
    @TypeDeserializer
    public static ObjectName ofObjectName(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ObjectName.getInstance(value);
        } catch (MalformedObjectNameException exception) {
            throw TypeDeserializationException.malformedValue(ObjectName.class, value, exception);
        }
    }
}

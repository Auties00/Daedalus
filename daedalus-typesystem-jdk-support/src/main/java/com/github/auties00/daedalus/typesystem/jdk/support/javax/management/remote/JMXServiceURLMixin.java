package com.github.auties00.daedalus.typesystem.jdk.support.javax.management.remote;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.management.remote.JMXServiceURL;

import java.net.MalformedURLException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link JMXServiceURL}.
 *
 * <p>{@code JMXServiceURL} is encoded as the {@code String} that contains its URL.
 *
 * <p>This mixin only requires the {@code java.management} module statically, so it can
 * only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class JMXServiceURLMixin {
    private JMXServiceURLMixin() {
        throw new UnsupportedOperationException("JMXServiceURLMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the URL of the given service URL.
     *
     * @param value the service URL, or {@code null}
     * @return the URL, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(JMXServiceURL value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the service URL with the given URL.
     *
     * @param value the URL, or {@code null}
     * @return the service URL, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid service URL
     */
    @TypeDeserializer
    public static JMXServiceURL ofJMXServiceURL(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new JMXServiceURL(value);
        } catch (MalformedURLException exception) {
            throw TypeDeserializationException.malformedValue(JMXServiceURL.class, value, exception);
        }
    }
}

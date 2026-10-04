package com.github.auties00.daedalus.typesystem.jdk.support.net;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.net.UnixDomainSocketAddress;
import java.nio.file.InvalidPathException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link UnixDomainSocketAddress}.
 *
 * <p>{@code UnixDomainSocketAddress} is encoded as the {@code String} that contains the string form of its path.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class UnixDomainSocketAddressMixin {
    private UnixDomainSocketAddressMixin() {
        throw new UnsupportedOperationException("UnixDomainSocketAddressMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the path of the given Unix domain socket address.
     *
     * @param value the Unix domain socket address, or {@code null}
     * @return the path, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(UnixDomainSocketAddress value) {
        return value == null ? null : value.getPath().toString();
    }

    /**
     * Returns the Unix domain socket address with the given path.
     *
     * @param value the path, or {@code null}
     * @return the Unix domain socket address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid path
     */
    @TypeDeserializer
    public static UnixDomainSocketAddress ofUnixDomainSocketAddress(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : UnixDomainSocketAddress.of(value);
        } catch (InvalidPathException exception) {
            throw TypeDeserializationException.malformedValue(UnixDomainSocketAddress.class, value, exception);
        }
    }
}

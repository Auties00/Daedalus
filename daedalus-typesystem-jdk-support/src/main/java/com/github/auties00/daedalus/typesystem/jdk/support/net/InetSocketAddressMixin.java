package com.github.auties00.daedalus.typesystem.jdk.support.net;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for
 * {@link InetSocketAddress}.
 *
 * <p>A socket address is encoded as a {@code String} of the form {@code host:port},
 * where an IPv6 literal is enclosed in square brackets. The host is the host name of the
 * address if it has one, or the literal form of its IP address otherwise.
 *
 * <p>Deserialization never resolves a host name, so it is deterministic and never
 * performs a network operation: a socket address whose host is not a literal is
 * deserialized as an {@linkplain InetSocketAddress#createUnresolved(String, int) unresolved}
 * address.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class InetSocketAddressMixin {
    private InetSocketAddressMixin() {
        throw new UnsupportedOperationException("InetSocketAddressMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the {@code host:port} form of the given socket address.
     *
     * <p>The host is the host name of the address if it has one, or the literal form of
     * its IP address otherwise, enclosed in square brackets if it is an IPv6 literal.
     *
     * @param value the socket address, or {@code null}
     * @return the {@code host:port} form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(InetSocketAddress value) {
        if (value == null) {
            return null;
        }

        var host = value.getHostString();
        return (host.indexOf(':') >= 0 ? "[" + host + "]" : host) + ":" + value.getPort();
    }

    /**
     * Returns the socket address represented by the given {@code host:port} form.
     *
     * <p>If the host is an IP address literal, the returned address is resolved to it.
     * Otherwise, the returned address is unresolved, as a host name is never resolved.
     *
     * @param value the {@code host:port} form, or {@code null}
     * @return the socket address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid socket address
     */
    @TypeDeserializer
    public static InetSocketAddress ofInetSocketAddress(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        try {
            var separator = value.lastIndexOf(':');
            if (separator <= 0) {
                throw TypeDeserializationException.malformedValue(InetSocketAddress.class, value, null);
            }

            var host = value.substring(0, separator);
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }

            var port = Integer.parseInt(value, separator + 1, value.length(), 10);
            var literal = InetAddress.ofLiteral(host);
            return new InetSocketAddress(literal, port);
        } catch (IllegalArgumentException literalException) {
            return ofUnresolvedSocketAddress(value);
        }
    }

    /**
     * Returns the unresolved socket address represented by the given {@code host:port}
     * form, whose host is not an IP address literal.
     *
     * @param value the {@code host:port} form
     * @return the unresolved socket address
     * @throws TypeDeserializationException if {@code value} is not a valid socket address
     */
    private static InetSocketAddress ofUnresolvedSocketAddress(String value) throws TypeDeserializationException {
        try {
            var separator = value.lastIndexOf(':');
            var host = value.substring(0, separator);
            var port = Integer.parseInt(value, separator + 1, value.length(), 10);
            return InetSocketAddress.createUnresolved(host, port);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(InetSocketAddress.class, value, exception);
        }
    }
}

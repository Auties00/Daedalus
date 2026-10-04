package com.github.auties00.daedalus.typesystem.jdk.support.net;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for the IP addresses
 * of the {@code java.net} package: {@link InetAddress}, {@link Inet4Address} and
 * {@link Inet6Address}.
 *
 * <p>An IP address is encoded as the {@code byte[]} that contains its raw address in
 * network byte order. The literal form can be used instead by listing
 * {@link InetAddressTextMixin} in the mixins of a field.
 *
 * <p>Deserialization never resolves a host name, so it is deterministic and never
 * performs a network operation.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class InetAddressMixin {
    private InetAddressMixin() {
        throw new UnsupportedOperationException("InetAddressMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the raw address of the given IP address, in network byte order.
     *
     * @param value the IP address, or {@code null}
     * @return the raw address, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(InetAddress value) {
        return value == null ? null : value.getAddress();
    }

    /**
     * Returns the IP address with the given raw address, in network byte order.
     *
     * @param value the raw address, or {@code null}
     * @return the IP address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is neither 4 nor 16 bytes long
     */
    @TypeDeserializer
    public static InetAddress ofInetAddress(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : InetAddress.getByAddress(value);
        } catch (UnknownHostException exception) {
            throw TypeDeserializationException.malformedValue(InetAddress.class, value, exception);
        }
    }

    /**
     * Returns the IPv4 address with the given raw address, in network byte order.
     *
     * @param value the raw address, or {@code null}
     * @return the IPv4 address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not 4 bytes long
     */
    @TypeDeserializer
    public static Inet4Address ofInet4Address(byte[] value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        try {
            if (value.length == 4 && InetAddress.getByAddress(value) instanceof Inet4Address address) {
                return address;
            }
        } catch (UnknownHostException exception) {
            throw TypeDeserializationException.malformedValue(Inet4Address.class, value, exception);
        }

        throw TypeDeserializationException.malformedValue(Inet4Address.class, value, null);
    }

    /**
     * Returns the IPv6 address with the given raw address, in network byte order.
     *
     * <p>An IPv4 mapped address is deserialized as an IPv6 address, although
     * {@link InetAddress#getByAddress(byte[])} returns an IPv4 address for it.
     *
     * @param value the raw address, or {@code null}
     * @return the IPv6 address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not 16 bytes long
     */
    @TypeDeserializer
    public static Inet6Address ofInet6Address(byte[] value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        try {
            if (value.length == 16) {
                return Inet6Address.getByAddress(null, value, -1);
            }
        } catch (UnknownHostException exception) {
            throw TypeDeserializationException.malformedValue(Inet6Address.class, value, exception);
        }

        throw TypeDeserializationException.malformedValue(Inet6Address.class, value, null);
    }
}

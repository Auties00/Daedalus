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
 * <p>An IP address is encoded as the {@code String} that contains its literal form.
 *
 * <p>Deserialization never resolves a host name, so it is deterministic and never
 * performs a network operation: an IP address must be encoded as a literal.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The raw address of an IP address is already provided globally by
 * {@link InetAddressMixin}, and a global mixin that provided both encodings would let a
 * conversion enter through one encoding and leave through the other, such as a
 * {@code byte[]} field silently converted to a {@code String} through an
 * {@link InetAddress}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class InetAddressTextMixin {
    private InetAddressTextMixin() {
        throw new UnsupportedOperationException("InetAddressTextMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the literal form of the given IP address.
     *
     * @param value the IP address, or {@code null}
     * @return the literal form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(InetAddress value) {
        return value == null ? null : value.getHostAddress();
    }

    /**
     * Returns the IP address with the given literal form.
     *
     * @param value the literal form, or {@code null}
     * @return the IP address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not an IP address literal
     */
    @TypeDeserializer
    public static InetAddress ofInetAddress(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : InetAddress.ofLiteral(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(InetAddress.class, value, exception);
        }
    }

    /**
     * Returns the IPv4 address with the given literal form.
     *
     * @param value the literal form, or {@code null}
     * @return the IPv4 address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not an IPv4 address literal
     */
    @TypeDeserializer
    public static Inet4Address ofInet4Address(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Inet4Address.ofLiteral(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Inet4Address.class, value, exception);
        }
    }

    /**
     * Returns the IPv6 address with the given literal form.
     *
     * <p>An IPv4 mapped literal, such as {@code ::ffff:192.0.2.1}, is deserialized as an
     * IPv6 address, although {@link InetAddress#ofLiteral(String)} returns an IPv4
     * address for it.
     *
     * @param value the literal form, or {@code null}
     * @return the IPv6 address, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not an IPv6 address literal
     */
    @TypeDeserializer
    public static Inet6Address ofInet6Address(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        try {
            return switch (InetAddress.ofLiteral(value)) {
                case Inet6Address address -> address;
                case Inet4Address address when value.indexOf(':') >= 0 -> toInet6Address(address);
                default -> throw TypeDeserializationException.malformedValue(Inet6Address.class, value, null);
            };
        } catch (IllegalArgumentException | UnknownHostException exception) {
            throw TypeDeserializationException.malformedValue(Inet6Address.class, value, exception);
        }
    }

    /**
     * Returns the IPv4 mapped IPv6 address of the given IPv4 address.
     *
     * @param address the IPv4 address
     * @return the IPv4 mapped IPv6 address
     * @throws UnknownHostException never, as the raw address is always 16 bytes long
     */
    private static Inet6Address toInet6Address(Inet4Address address) throws UnknownHostException {
        var mapped = new byte[16];
        mapped[10] = (byte) 0xFF;
        mapped[11] = (byte) 0xFF;
        System.arraycopy(address.getAddress(), 0, mapped, 12, 4);
        return Inet6Address.getByAddress(null, mapped, -1);
    }
}

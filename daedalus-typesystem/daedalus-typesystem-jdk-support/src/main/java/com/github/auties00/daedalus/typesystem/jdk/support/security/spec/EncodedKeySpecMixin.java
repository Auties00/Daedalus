package com.github.auties00.daedalus.typesystem.jdk.support.security.spec;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for the encoded key
 * specifications of the {@code java.security.spec} package: {@link X509EncodedKeySpec}
 * and {@link PKCS8EncodedKeySpec}.
 *
 * <p>A key specification is encoded as the {@code byte[]} that contains its DER encoding.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class EncodedKeySpecMixin {
    private EncodedKeySpecMixin() {
        throw new UnsupportedOperationException("EncodedKeySpecMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the X.509 DER encoding contained by the given key specification.
     *
     * @param value the key specification, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(X509EncodedKeySpec value) {
        return value == null ? null : value.getEncoded();
    }

    /**
     * Returns the key specification that contains the given X.509 DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @return the key specification, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static X509EncodedKeySpec ofX509EncodedKeySpec(byte[] value) {
        return value == null ? null : new X509EncodedKeySpec(value);
    }

    /**
     * Returns the PKCS#8 DER encoding contained by the given key specification.
     *
     * @param value the key specification, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(PKCS8EncodedKeySpec value) {
        return value == null ? null : value.getEncoded();
    }

    /**
     * Returns the key specification that contains the given PKCS#8 DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @return the key specification, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static PKCS8EncodedKeySpec ofPKCS8EncodedKeySpec(byte[] value) {
        return value == null ? null : new PKCS8EncodedKeySpec(value);
    }
}

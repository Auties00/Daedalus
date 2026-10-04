package com.github.auties00.daedalus.typesystem.jdk.support.security.cert;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.security.PEMDecoder;
import java.security.PEMEncoder;
import java.security.cert.X509Certificate;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for
 * {@link X509Certificate}.
 *
 * <p>An X.509 certificate is encoded as the {@code String} that contains its PEM form,
 * which uses the {@link PEMEncoder} and {@link PEMDecoder} preview API of the JDK.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The DER encoding of a certificate is already provided globally by
 * {@link CertificateMixin}, and a global mixin that provided both encodings would let a
 * conversion enter through one encoding and leave through the other, such as a
 * {@code byte[]} field silently converted to a {@code String} through an
 * {@link X509Certificate}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class CertificatePemMixin {
    private CertificatePemMixin() {
        throw new UnsupportedOperationException("CertificatePemMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the PEM form of the given X.509 certificate.
     *
     * @param value the certificate, or {@code null}
     * @return the PEM form, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the certificate cannot be encoded
     */
    @TypeSerializer
    public static String toText(X509Certificate value) throws TypeSerializationException {
        try {
            return value == null ? null : PEMEncoder.of().encodeToString(value);
        } catch (IllegalArgumentException exception) {
            throw TypeSerializationException.unencodableValue(X509Certificate.class, exception);
        }
    }

    /**
     * Returns the X.509 certificate with the given PEM form.
     *
     * @param value the PEM form, or {@code null}
     * @return the certificate, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not the PEM form of an X.509
     *         certificate
     */
    @TypeDeserializer
    public static X509Certificate ofX509Certificate(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : PEMDecoder.of().decode(value, X509Certificate.class);
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw TypeDeserializationException.malformedValue(X509Certificate.class, value, exception);
        }
    }
}

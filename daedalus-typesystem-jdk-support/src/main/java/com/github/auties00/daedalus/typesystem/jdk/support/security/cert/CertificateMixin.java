package com.github.auties00.daedalus.typesystem.jdk.support.security.cert;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Certificate}
 * and {@link X509Certificate}.
 *
 * <p>A certificate is encoded as the {@code byte[]} that contains its DER encoding. The
 * PEM form of an X.509 certificate can be used instead by listing
 * {@link CertificatePemMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CertificateMixin {
    /**
     * The type of the certificates decoded by this mixin.
     */
    private static final String CERTIFICATE_TYPE = "X.509";

    private CertificateMixin() {
        throw new UnsupportedOperationException("CertificateMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the DER encoding of the given certificate.
     *
     * @param value the certificate, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the certificate cannot be encoded
     */
    @TypeSerializer
    public static byte[] toBytes(Certificate value) throws TypeSerializationException {
        try {
            return value == null ? null : value.getEncoded();
        } catch (CertificateEncodingException exception) {
            throw TypeSerializationException.unencodableValue(Certificate.class, exception);
        }
    }

    /**
     * Returns the X.509 certificate with the given DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @return the certificate, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid encoding of an
     *         X.509 certificate
     */
    @TypeDeserializer
    public static X509Certificate ofX509Certificate(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : (X509Certificate) CertificateFactory.getInstance(CERTIFICATE_TYPE).generateCertificate(new ByteArrayInputStream(value));
        } catch (CertificateException exception) {
            throw TypeDeserializationException.malformedValue(X509Certificate.class, value, exception);
        }
    }
}

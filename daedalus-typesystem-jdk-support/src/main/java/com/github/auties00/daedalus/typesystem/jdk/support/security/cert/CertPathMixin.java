package com.github.auties00.daedalus.typesystem.jdk.support.security.cert;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link CertPath}.
 *
 * <p>A certification path of X.509 certificates is encoded as the {@code byte[]} that
 * contains its {@code PkiPath} encoding.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CertPathMixin {
    /**
     * The type of the certificates decoded by this mixin.
     */
    private static final String CERTIFICATE_TYPE = "X.509";

    /**
     * The encoding of the certification paths serialized by this mixin.
     */
    private static final String CERTIFICATE_PATH_ENCODING = "PkiPath";

    private CertPathMixin() {
        throw new UnsupportedOperationException("CertPathMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the {@code PkiPath} encoding of the given certification path.
     *
     * @param value the certification path, or {@code null}
     * @return the {@code PkiPath} encoding, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the certification path cannot be encoded
     */
    @TypeSerializer
    public static byte[] toBytes(CertPath value) throws TypeSerializationException {
        try {
            return value == null ? null : value.getEncoded(CERTIFICATE_PATH_ENCODING);
        } catch (CertificateEncodingException exception) {
            throw TypeSerializationException.unencodableValue(CertPath.class, exception);
        }
    }

    /**
     * Returns the certification path of X.509 certificates with the given {@code PkiPath}
     * encoding.
     *
     * @param value the {@code PkiPath} encoding, or {@code null}
     * @return the certification path, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid {@code PkiPath}
     *         encoding
     */
    @TypeDeserializer
    public static CertPath ofCertPath(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : CertificateFactory.getInstance(CERTIFICATE_TYPE).generateCertPath(new ByteArrayInputStream(value), CERTIFICATE_PATH_ENCODING);
        } catch (CertificateException exception) {
            throw TypeDeserializationException.malformedValue(CertPath.class, value, exception);
        }
    }
}

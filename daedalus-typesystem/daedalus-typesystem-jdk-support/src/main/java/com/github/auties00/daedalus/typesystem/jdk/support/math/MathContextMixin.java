package com.github.auties00.daedalus.typesystem.jdk.support.math;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link MathContext}.
 *
 * <p>{@code MathContext} is encoded as the {@code String} that contains its string form, such as
 * {@code precision=7 roundingMode=HALF_EVEN}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class MathContextMixin {
    /**
     * The prefix of the precision in the string form of a math context.
     */
    private static final String PRECISION_PREFIX = "precision=";

    /**
     * The prefix of the rounding mode in the string form of a math context, which
     * follows the precision after a space.
     */
    private static final String ROUNDING_MODE_PREFIX = "roundingMode=";

    private MathContextMixin() {
        throw new UnsupportedOperationException("MathContextMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given math context, such as
     * {@code precision=7 roundingMode=HALF_EVEN}.
     *
     * @param value the math context, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(MathContext value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the math context represented by the given string form.
     *
     * <p>The string form is parsed by this method instead of
     * {@link MathContext#MathContext(String)}, which looks up the rounding mode with
     * {@link RoundingMode#valueOf(String)}, as that relies on reflection: the rounding
     * mode is found by iterating {@link RoundingMode#values()} instead.
     *
     * @param value the string form, or {@code null}
     * @return the math context, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid math context
     */
    @TypeDeserializer
    public static MathContext ofMathContext(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        var separator = value.indexOf(' ');
        if (!value.startsWith(PRECISION_PREFIX) || separator < 0 || !value.startsWith(ROUNDING_MODE_PREFIX, separator + 1)) {
            throw TypeDeserializationException.malformedValue(MathContext.class, value, null);
        }

        try {
            var precision = Integer.parseInt(value, PRECISION_PREFIX.length(), separator, 10);
            var roundingModeName = value.substring(separator + 1 + ROUNDING_MODE_PREFIX.length());
            for (var roundingMode : RoundingMode.values()) {
                if (roundingMode.name().equals(roundingModeName)) {
                    return new MathContext(precision, roundingMode);
                }
            }
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(MathContext.class, value, exception);
        }

        throw TypeDeserializationException.malformedValue(MathContext.class, value, null);
    }
}

package com.github.auties00.daedalus.typesystem.jdk.support.util.regex;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Pattern}.
 *
 * <p>A pattern is encoded as the {@code String} that contains its regular expression.
 * The flags passed to {@link Pattern#compile(String, int)} are not part of it, so a pattern
 * compiled with flags cannot be serialized: embedded flag expressions, such as
 * {@code (?i)}, must be used instead.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PatternMixin {
    private PatternMixin() {
        throw new UnsupportedOperationException("PatternMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the regular expression of the given pattern.
     *
     * <p>{@link Pattern#flags()} also reports the flags enabled by the embedded flag
     * expressions of the regular expression, so the flags of the pattern are preserved
     * exactly when compiling its regular expression alone reproduces them.
     *
     * @param value the pattern, or {@code null}
     * @return the regular expression, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the pattern was compiled with flags that are
     *         not embedded in its regular expression, which would be lost
     */
    @TypeSerializer
    public static String toText(Pattern value) throws TypeSerializationException {
        if (value == null) {
            return null;
        }

        var regex = value.pattern();
        if (value.flags() != 0 && Pattern.compile(regex).flags() != value.flags()) {
            throw TypeSerializationException.patternFlags(value);
        }

        return regex;
    }

    /**
     * Returns the pattern compiled from the given regular expression.
     *
     * @param value the regular expression, or {@code null}
     * @return the pattern, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid regular expression
     */
    @TypeDeserializer
    public static Pattern ofPattern(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Pattern.compile(value);
        } catch (PatternSyntaxException exception) {
            throw TypeDeserializationException.malformedValue(Pattern.class, value, exception);
        }
    }
}

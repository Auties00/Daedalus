package it.auties.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to non-static methods,
 * in a type annotated with {@link ProtobufMessage},
 * to represent a custom accessor for an existing field with the same index.
 * <h2>Usage Example:</h2>
 * <h3>In a {@link ProtobufMessage}:</h3>
 * <pre>{@code
 * @ProtobufMessage
 * public final class BoxedMessage {
 *     @ProtobufMessage.StringField(index = 1)
 *     private final String value;
 *
 *     public BoxedMessage(String value) {
 *         this.value = value;
 *     }
 *
 *     @ProtobufAccessor(index = 1)
 *     public String unbox() {
 *         return value;
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufAccessor {
    /**
     * Returns the index of the associated field.
     *
     * @return the numeric index of the associated field
     */
    long index();
}

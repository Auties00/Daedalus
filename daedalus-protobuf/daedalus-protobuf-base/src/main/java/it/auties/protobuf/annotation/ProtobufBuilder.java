package it.auties.protobuf.annotation;

import it.auties.protobuf.builtin.*;
import it.auties.protobuf.model.ProtobufType;

import javax.lang.model.element.Modifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a fluent builder class for protobuf messages with customizable construction logic.
 *
 * <p>This annotation can be applied to static methods or constructors in classes/records
 * annotated with {@link ProtobufMessage} to auto-generate a builder
 * class that provides a fluent API for constructing instances of the protobuf type.
 *
 * <p>The parameters of the annotated method/constructor must be annotated with {@link PropertyParameter} or {@link SyntheticParameter}.
 *
 * <h2>Usage Example:</h2>
 * <p>Given the following protobuf message:
 * <pre>{@code
 * @ProtobufMessage
 * public record Person(
 *     @ProtobufMessage.StringField(index = 1)
 *     String name,
 *     @ProtobufMessage.Int32Field(index = 2)
 *     int age
 * ) {
 *
 * }
 * }</pre>
 *
 * <p>The following builder class is generated:
 * <pre>{@code
 * public class PersonBuilder {
 *     private String name;
 *     private int age;
 *
 *     public PersonBuilder() {
 *         this.name = null;
 *         this.age = 0;
 *     }
 *
 *     public PersonBuilder name(String name) {
 *         this.name = name;
 *         return this;
 *     }
 *
 *     public PersonBuilder age(int age) {
 *         this.age = age;
 *         return this;
 *     }
 *
 *     public Person build() {
 *         return new Person(name, age);
 *     }
 * }
 * }</pre>
 *
 * <p>The builder can then be used as follows:
 * <pre>{@code
 * var person = new PersonBuilder()
 *     .name("Alice")
 *     .age(30)
 *     .build();
 * }</pre>
 *
 * @see PropertyParameter
 * @see SyntheticParameter
 * @see ProtobufMessage
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufBuilder {
    /**
     * Specifies the visibility and mutability modifiers for the generated builder class.
     *
     * @return an array of modifiers to apply to the generated builder class
     */
    Modifier[] modifiers() default {
            Modifier.PUBLIC,
            Modifier.FINAL
    };

    /**
     * Specifies the name of the generated builder class:
     * <ul>
     *   <li><b>Empty name (default):</b> Overrides the existing default builder</li>
     *   <li><b>Non-empty name:</b> Generates an additional builder with the provided name</li>
     * </ul>
     *
     * @return the name of the generated builder class
     */
    String name() default "";

    /**
     * Maps a builder method parameter directly to an existing protobuf field by its field index.
     *
     * <p>Use this annotation when a parameter corresponds exactly to a single protobuf field.
     *
     * <h2>Example:</h2>
     * <p>Given the following protobuf message:
     * <pre>{@code
     * @ProtobufMessage
     * public record Person(
     *     @ProtobufMessage.StringField(index = 1)
     *     String name,
     *     @ProtobufMessage.Int32Field(index = 2)
     *     int age
     * ) {
     *     @ProtobufBuilder(name = "PersonPropertyBuilder")
     *     static Person of(
     *         @PropertyParameter(index = 1) String name,
     *         @PropertyParameter(index = 2) int age
     *     ) {
     *         return new Person(name, age);
     *     }
     * }
     * }</pre>
     *
     * <p>The following builder class is generated:
     * <pre>{@code
     * public class PersonPropertyBuilder {
     *     private String name;
     *     private int age;
     *
     *     public PersonPropertyBuilder() {
     *     }
     *
     *     public PersonPropertyBuilder name(String name) {
     *         this.name = name;
     *         return this;
     *     }
     *
     *     public PersonPropertyBuilder age(int age) {
     *         this.age = age;
     *         return this;
     *     }
     *
     *     public Person build() {
     *         return Person.of(name, age);
     *     }
     * }
     * }</pre>
     *
     * @see SyntheticParameter
     */
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface PropertyParameter {
        /**
         * The index of the protobuf field this parameter maps to.
         *
         * <p>This must match the {@code index} value of a field annotation
         * on a field or record component in the same class.
         *
         * @return the protobuf field index
         */
        long index();
    }

    /**
     * Marks a builder method parameter that doesn't directly map to a single existing protobuf field.
     *
     * <p>Use this annotation for parameters that require custom logic to populate one or more fields,
     * such as union types, computed values, or parameters that transform into multiple fields.
     *
     * <p><b>Common Use Cases:</b>
     * <ul>
     *   <li><b>Union types:</b> A single parameter that maps to one of several mutually exclusive fields</li>
     *   <li><b>Computed fields:</b> A parameter that derives multiple field values through calculation</li>
     *   <li><b>Complex types:</b> Custom types that need transformation before being set as fields</li>
     * </ul>
     *
     * <h2>Union types:</h2>
     * <p>A single parameter that maps to one of several mutually exclusive fields:
     * <pre>{@code
     * @ProtobufMessage
     * public record MediaMessage(
     *     @ProtobufMessage.MessageField(index = 1)
     *     ImageMedia image,
     *     @ProtobufMessage.MessageField(index = 2)
     *     VideoMedia video,
     *     @ProtobufMessage.MessageField(index = 3)
     *     AudioMedia audio
     * ) {
     *     sealed interface Media permits ImageMedia, VideoMedia, AudioMedia {}
     *
     *     @ProtobufBuilder(name = "MediaMessageUnionBuilder")
     *     static MediaMessage of(@SyntheticParameter(type = ProtobufType.MESSAGE) Media media) {
     *         return switch (media) {
     *             case ImageMedia img -> new MediaMessage(img, null, null);
     *             case VideoMedia vid -> new MediaMessage(null, vid, null);
     *             case AudioMedia aud -> new MediaMessage(null, null, aud);
     *         };
     *     }
     * }
     * }</pre>
     *
     * <p>The following builder class is generated:
     * <pre>{@code
     * public class MediaMessageUnionBuilder {
     *     private Media media;
     *
     *     public MediaMessageUnionBuilder() {
     *     }
     *
     *     public MediaMessageUnionBuilder media(Media media) {
     *         this.media = media;
     *         return this;
     *     }
     *
     *     public MediaMessage build() {
     *         return MediaMessage.of(media);
     *     }
     * }
     * }</pre>
     *
     * <h2>Computed fields:</h2>
     * <p>A parameter that derives multiple field values through calculation:
     * <pre>{@code
     * @ProtobufMessage
     * public record Rectangle(
     *     @ProtobufMessage.Int32Field(index = 1)
     *     int x,
     *     @ProtobufMessage.Int32Field(index = 2)
     *     int y,
     *     @ProtobufMessage.Int32Field(index = 3)
     *     int width,
     *     @ProtobufMessage.Int32Field(index = 4)
     *     int height
     * ) {
     *     record Bounds(int x, int y, int width, int height) {}
     *
     *     @ProtobufBuilder(name = "RectangleFromBoundsBuilder")
     *     static Rectangle fromBounds(@SyntheticParameter(type = ProtobufType.UNKNOWN) Bounds bounds) {
     *         return new Rectangle(bounds.x(), bounds.y(), bounds.width(), bounds.height());
     *     }
     * }
     * }</pre>
     *
     * <p>The following builder class is generated:
     * <pre>{@code
     * public class RectangleFromBoundsBuilder {
     *     private Bounds bounds;
     *
     *     public RectangleFromBoundsBuilder() {
     *     }
     *
     *     public RectangleFromBoundsBuilder bounds(Bounds bounds) {
     *         this.bounds = bounds;
     *         return this;
     *     }
     *
     *     public Rectangle build() {
     *         return Rectangle.fromBounds(bounds);
     *     }
     * }
     * }</pre>
     *
     * <h2>Complex types:</h2>
     * <p>Custom types that need transformation before being set as fields:
     * <pre>{@code
     * @ProtobufMessage
     * public record Timestamp(
     *     @ProtobufMessage.Int64Field(index = 1)
     *     long seconds,
     *     @ProtobufMessage.Int32Field(index = 2)
     *     int nanos
     * ) {
     *     @ProtobufBuilder(name = "TimestampFromInstantBuilder")
     *     static Timestamp fromInstant(@SyntheticParameter(type = ProtobufType.UNKNOWN) Instant instant) {
     *         return new Timestamp(instant.getEpochSecond(), instant.getNano());
     *     }
     * }
     * }</pre>
     *
     * <p>The following builder class is generated:
     * <pre>{@code
     * public class TimestampFromInstantBuilder {
     *     private Instant instant;
     *
     *     public TimestampFromInstantBuilder() {
     *     }
     *
     *     public TimestampFromInstantBuilder instant(Instant instant) {
     *         this.instant = instant;
     *         return this;
     *     }
     *
     *     public Timestamp build() {
     *         return Timestamp.fromInstant(instant);
     *     }
     * }
     * }</pre>
     *
     * @see PropertyParameter
     */
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SyntheticParameter {
        /**
         * The protobuf type of this synthetic parameter.
         * If the type of the parameter is not a protobuf, use {@link ProtobufType#UNKNOWN}.
         *
         * @return the ProtobufType that categorizes this parameter
         */
        ProtobufType type();

        /**
         * Mixin classes that provide serialization/deserialization support for this parameter's type.
         * Only used if {@link #type()} is a valid Protobuf type.
         *
         * @return an array of mixin classes for type conversion
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                MapMixin.class,
                OptionalMixin.class,
                StringMixin.class,
                URIMixin.class,
                URLMixin.class,
                UUIDMixin.class
        };
    }
}

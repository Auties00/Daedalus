package com.github.auties00.daedalus.protobuf.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A container for the annotations that describe the parameters of a {@code @TypeBuilder} method
 * declared inside a protobuf message.
 *
 * <p>Every parameter of such a method must be annotated with exactly one of the children of this
 * type: {@link Field} for a parameter that maps to a single existing protobuf field, or
 * {@link Synthetic} for a parameter that doesn't.
 *
 * <p>This type is not meant to be used directly as an annotation.
 */
public final class ProtobufBuilderParameter {
    private ProtobufBuilderParameter() {
        throw new UnsupportedOperationException("ProtobufBuilderParameter is a container and cannot be instantiated");
    }

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
     *     @TypeBuilder(name = "PersonPropertyBuilder")
     *     static Person of(
     *         @ProtobufBuilderParameter.Field(index = 1) String name,
     *         @ProtobufBuilderParameter.Field(index = 2) int age
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
     * @see Synthetic
     */
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Field {
        /**
         * The index of the protobuf field this parameter maps to.
         *
         * <p>This must match the {@code index} value of a field annotation
         * on a field or record component in the same class.
         *
         * @return the protobuf field index
         */
        int index();
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
     *     @TypeBuilder(name = "MediaMessageUnionBuilder")
     *     static MediaMessage of(@ProtobufBuilderParameter.Synthetic Media media) {
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
     *     @TypeBuilder(name = "RectangleFromBoundsBuilder")
     *     static Rectangle fromBounds(@ProtobufBuilderParameter.Synthetic Bounds bounds) {
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
     *     @TypeBuilder(name = "TimestampFromInstantBuilder")
     *     static Timestamp fromInstant(@ProtobufBuilderParameter.Synthetic Instant instant) {
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
     * @see Field
     */
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Synthetic {
        /**
         * Mixin classes that provide serialization/deserialization support for this parameter's type.
         * Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here.
         *
         * @return an array of mixin classes for type conversion
         */
        Class<?>[] mixins() default {};
    }
}

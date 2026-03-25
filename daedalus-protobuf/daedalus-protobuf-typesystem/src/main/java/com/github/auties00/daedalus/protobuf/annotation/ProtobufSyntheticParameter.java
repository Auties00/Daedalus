package com.github.auties00.daedalus.protobuf.annotation;

import com.github.auties00.daedalus.protobuf.adapter.StringMixin;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.typesystem.adapter.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 *     static MediaMessage of(@ProtobufSyntheticParameter(type = ProtobufType.MESSAGE) Media media) {
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
 *     static Rectangle fromBounds(@ProtobufSyntheticParameter(type = ProtobufType.UNKNOWN) Bounds bounds) {
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
 *     static Timestamp fromInstant(@ProtobufSyntheticParameter(type = ProtobufType.UNKNOWN) Instant instant) {
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
 * @see ProtobufFieldParameter
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufSyntheticParameter {
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

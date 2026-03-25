package com.github.auties00.daedalus.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 *         @ProtobufFieldParameter(index = 1) String name,
 *         @ProtobufFieldParameter(index = 2) int age
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
 * @see ProtobufSyntheticParameter
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufFieldParameter {
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

package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;

import java.util.Optional;

/**
 * Holder of the Daedalus models declared by {@code google/protobuf/java_mutable_features.proto}.
 *
 * <p>These models represent the Java mutable feature set carried by the
 * {@code (pb.java_mutable)} extension of {@link DescriptorProtos.FeatureSet}.
 *
 * <p>The models mirror the {@code proto2} definitions of the official protobuf
 * distribution field by field, including field numbers, wire types, field
 * presence and repeated field encodings. This class only scopes the nested
 * types and cannot be instantiated.
 */
public final class JavaMutableFeaturesProto {
    /**
     * Prevents the instantiation of this holder class.
     */
    private JavaMutableFeaturesProto() {}

    /**
     * <p>Protobuf type {@code pb.JavaMutableFeatures}
     */
    @ProtobufMessage
    public static final class JavaMutableFeatures {

        /**
         * <p>Protobuf type {@code pb.JavaMutableFeatures.NestInFileClassFeature}
         */
        @ProtobufMessage(reservedRanges = {@ProtobufReservedRange(min = 1, max = 536870911)})
        public static final class NestInFileClassFeature {

            /**
             * <p>Protobuf enum {@code pb.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass}
             */
            @ProtobufEnum
            public enum NestInFileClass {
                /**
                 * Invalid default, which should never be used.
                 */
                @ProtobufEnum.Constant(index = 0)
                NEST_IN_FILE_CLASS_UNKNOWN,

                /**
                 * Do not nest the generated class in the file class.
                 */
                @ProtobufEnum.Constant(index = 1)
                NO,

                /**
                 * Nest the generated class in the file class.
                 */
                @ProtobufEnum.Constant(index = 2)
                YES,

                /**
                 * Fall back to the {@code java_multiple_files} and
                 * {@code java_multiple_files_mutable_package} options. Users won't be able to
                 * set this option.
                 */
                @ProtobufEnum.Constant(index = 3)
                LEGACY;
            }
        }

        /**
         * Whether to nest the generated class in the generated file class for
         * Java Proto2 Mutable API. This is only available at the file level.
         *
         * <p><code>NestInFileClassFeature.NestInFileClass nest_in_file_class = 6;</code>
         */
        @ProtobufMessage.EnumField(index = 6)
        JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass;

        /**
         * Constructs a new {@code JavaMutableFeatures} with the specified field values.
         *
         * @param nestInFileClass the value of the {@code nest_in_file_class} field, or {@code null} if the
         *                        field is not set
         */
        JavaMutableFeatures(
                JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass
        ) {
            this.nestInFileClass = nestInFileClass;
        }

        /**
         * Returns the value of the {@code nest_in_file_class} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code nest_in_file_class}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass> nestInFileClass() {
            return Optional.ofNullable(nestInFileClass);
        }
    }
}

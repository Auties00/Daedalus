package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;

public final class JavaMutableFeaturesProto {
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

        JavaMutableFeatures(
                JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass
        ) {
            this.nestInFileClass = nestInFileClass;
        }

        public JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass() {
            return nestInFileClass;
        }
    }
}

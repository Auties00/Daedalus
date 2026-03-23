package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufReservedRange;

public final class JavaFeaturesProto {
    private JavaFeaturesProto() {}

    /**
     * <p>Protobuf type {@code pb.JavaFeatures}
     */
    @ProtobufMessage(reservedIndexes = {6})
    public static final class JavaFeatures {

        /**
         * The UTF8 validation strategy to use.
         *
         * <p>Protobuf enum {@code pb.JavaFeatures.Utf8Validation}
         */
        @ProtobufEnum
        public enum Utf8Validation {
            /**
             * Invalid default, which should never be used.
             */
            @ProtobufEnum.Constant(index = 0)
            UTF8_VALIDATION_UNKNOWN,

            /**
             * Respect the UTF8 validation behavior specified by the global
             * utf8_validation feature.
             */
            @ProtobufEnum.Constant(index = 1)
            DEFAULT,

            /**
             * Verifies UTF8 validity overriding the global utf8_validation
             * feature. This represents the legacy java_string_check_utf8 option.
             */
            @ProtobufEnum.Constant(index = 2)
            VERIFY;
        }

        /**
         * <p>Protobuf type {@code pb.JavaFeatures.NestInFileClassFeature}
         */
        @ProtobufMessage(reservedRanges = {@ProtobufReservedRange(min = 1, max = 536870911)})
        public static final class NestInFileClassFeature {

            /**
             * <p>Protobuf enum {@code pb.JavaFeatures.NestInFileClassFeature.NestInFileClass}
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
                 * Fall back to the {@code java_multiple_files} option. Users won't be able to
                 * set this option.
                 */
                @ProtobufEnum.Constant(index = 3)
                LEGACY;
            }
        }

        /**
         * Whether or not to treat an enum field as closed.  This option is only
         * applicable to enum fields, and will be removed in the future.  It is
         * consistent with the legacy behavior of using proto3 enum types for proto2
         * fields.
         * protobuf.dev/programming-guides/enum/#java for "
         *
         * <p><code>bool legacy_closed_enum = 1;</code>
         */
        @ProtobufMessage.BoolField(index = 1)
        boolean legacyClosedEnum;

        /**
         * <p><code>Utf8Validation utf8_validation = 2;</code>
         */
        @ProtobufMessage.EnumField(index = 2)
        JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation;

        /**
         * Allows creation of large Java enums, extending beyond the standard
         * constant limits imposed by the Java language.
         *
         * <p><code>bool large_enum = 3;</code>
         */
        @ProtobufMessage.BoolField(index = 3)
        boolean largeEnum;

        /**
         * Whether to use the old default outer class name scheme, or the new feature
         * which adds a "Proto" suffix to the outer class name.
         *
         * <p>Users will not be able to set this option, because we removed it in the
         * same edition that it was introduced. But we use it to determine which
         * naming scheme to use for outer class name defaults.
         *
         * <p><code>bool use_old_outer_classname_default = 4;</code>
         */
        @ProtobufMessage.BoolField(index = 4)
        boolean useOldOuterClassnameDefault;

        /**
         * Whether to nest the generated class in the generated file class. This is
         * only applicable to *top-level* messages, enums, and services.
         *
         * <p><code>NestInFileClassFeature.NestInFileClass nest_in_file_class = 5;</code>
         */
        @ProtobufMessage.EnumField(index = 5)
        JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass;

        JavaFeatures(
                boolean legacyClosedEnum,
                JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation,
                boolean largeEnum,
                boolean useOldOuterClassnameDefault,
                JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass
        ) {
            this.legacyClosedEnum = legacyClosedEnum;
            this.utf8Validation = utf8Validation;
            this.largeEnum = largeEnum;
            this.useOldOuterClassnameDefault = useOldOuterClassnameDefault;
            this.nestInFileClass = nestInFileClass;
        }

        public boolean legacyClosedEnum() {
            return legacyClosedEnum;
        }

        public JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation() {
            return utf8Validation;
        }

        public boolean largeEnum() {
            return largeEnum;
        }

        public boolean useOldOuterClassnameDefault() {
            return useOldOuterClassnameDefault;
        }

        public JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass nestInFileClass() {
            return nestInFileClass;
        }
    }
}

package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;

public final class GoFeaturesProto {
    private GoFeaturesProto() {}

    /**
     * <p>Protobuf type {@code pb.GoFeatures}
     */
    @ProtobufMessage
    public static final class GoFeatures {

        /**
         * <p>Protobuf enum {@code pb.GoFeatures.APILevel}
         */
        @ProtobufEnum
        public enum APILevel {
            /**
             * Results in selecting the OPEN API, but is a separate value to
             * distinguish between an explicitly set api level or a missing one.
             */
            @ProtobufEnum.Constant(index = 0)
            API_LEVEL_UNSPECIFIED,

            @ProtobufEnum.Constant(index = 1)
            API_OPEN,

            @ProtobufEnum.Constant(index = 2)
            API_HYBRID,

            @ProtobufEnum.Constant(index = 3)
            API_OPAQUE;
        }

        /**
         * <p>Protobuf enum {@code pb.GoFeatures.StripEnumPrefix}
         */
        @ProtobufEnum
        public enum StripEnumPrefix {
            @ProtobufEnum.Constant(index = 0)
            STRIP_ENUM_PREFIX_UNSPECIFIED,

            @ProtobufEnum.Constant(index = 1)
            STRIP_ENUM_PREFIX_KEEP,

            @ProtobufEnum.Constant(index = 2)
            STRIP_ENUM_PREFIX_GENERATE_BOTH,

            @ProtobufEnum.Constant(index = 3)
            STRIP_ENUM_PREFIX_STRIP;
        }

        /**
         * <p>Protobuf type {@code pb.GoFeatures.OptimizeModeFeature}
         */
        @ProtobufMessage(reservedRanges = {@ProtobufReservedRange(min = 1, max = 536870911)})
        public static final class OptimizeModeFeature {

            /**
             * The name of this enum matches OptimizeMode in descriptor.proto.
             *
             * <p>Protobuf enum {@code pb.GoFeatures.OptimizeModeFeature.OptimizeMode}
             */
            @ProtobufEnum
            public enum OptimizeMode {
                /**
                 * Falls back to the default (optimize for code size), but is a
                 * separate value so an explicit setting can be distinguished from
                 * a missing one.
                 */
                @ProtobufEnum.Constant(index = 0)
                OPTIMIZE_MODE_UNSPECIFIED,

                @ProtobufEnum.Constant(index = 1)
                SPEED,

                @ProtobufEnum.Constant(index = 2)
                CODE_SIZE;
            }
        }

        /**
         * Whether or not to generate the deprecated UnmarshalJSON method for
         * enums. Can only be true for protos using the Open Struct api.
         *
         * <p><code>bool legacy_unmarshal_json_enum = 1;</code>
         */
        @ProtobufMessage.BoolField(index = 1)
        boolean legacyUnmarshalJsonEnum;

        /**
         * One of OPEN, HYBRID or OPAQUE.
         *
         * <p><code>APILevel api_level = 2;</code>
         */
        @ProtobufMessage.EnumField(index = 2)
        GoFeaturesProto.GoFeatures.APILevel apiLevel;

        /**
         * <p><code>StripEnumPrefix strip_enum_prefix = 3;</code>
         */
        @ProtobufMessage.EnumField(index = 3)
        GoFeaturesProto.GoFeatures.StripEnumPrefix stripEnumPrefix;

        /**
         * <p><code>OptimizeModeFeature.OptimizeMode optimize_mode = 4;</code>
         */
        @ProtobufMessage.EnumField(index = 4)
        GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode optimizeMode;

        GoFeatures(
                boolean legacyUnmarshalJsonEnum,
                GoFeaturesProto.GoFeatures.APILevel apiLevel,
                GoFeaturesProto.GoFeatures.StripEnumPrefix stripEnumPrefix,
                GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode optimizeMode
        ) {
            this.legacyUnmarshalJsonEnum = legacyUnmarshalJsonEnum;
            this.apiLevel = apiLevel;
            this.stripEnumPrefix = stripEnumPrefix;
            this.optimizeMode = optimizeMode;
        }

        public boolean legacyUnmarshalJsonEnum() {
            return legacyUnmarshalJsonEnum;
        }

        public GoFeaturesProto.GoFeatures.APILevel apiLevel() {
            return apiLevel;
        }

        public GoFeaturesProto.GoFeatures.StripEnumPrefix stripEnumPrefix() {
            return stripEnumPrefix;
        }

        public GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode optimizeMode() {
            return optimizeMode;
        }
    }
}

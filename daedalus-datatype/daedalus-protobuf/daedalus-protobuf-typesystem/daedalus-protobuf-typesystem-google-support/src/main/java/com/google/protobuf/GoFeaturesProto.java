package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import java.util.Optional;

/**
 * Holder of the Daedalus models declared by {@code google/protobuf/go_features.proto}.
 *
 * <p>These models represent the Go specific feature set carried by the
 * {@code (pb.go)} extension of {@link DescriptorProtos.FeatureSet}.
 *
 * <p>The models mirror the {@code proto2} definitions of the official protobuf
 * distribution field by field, including field numbers, wire types, field
 * presence and repeated field encodings. This class only scopes the nested
 * types and cannot be instantiated.
 */
public final class GoFeaturesProto {
    /**
     * Prevents the instantiation of this holder class.
     */
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
        @ProtobufMessage
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
        Boolean legacyUnmarshalJsonEnum;

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

        /**
         * Constructs a new {@code GoFeatures} with the specified field values.
         *
         * @param legacyUnmarshalJsonEnum the value of the {@code legacy_unmarshal_json_enum} field, or {@code null} if the
         *                                field is not set
         * @param apiLevel the value of the {@code api_level} field, or {@code null} if the
         *                 field is not set
         * @param stripEnumPrefix the value of the {@code strip_enum_prefix} field, or {@code null} if the
         *                        field is not set
         * @param optimizeMode the value of the {@code optimize_mode} field, or {@code null} if the
         *                     field is not set
         */
        GoFeatures(
                Boolean legacyUnmarshalJsonEnum,
                GoFeaturesProto.GoFeatures.APILevel apiLevel,
                GoFeaturesProto.GoFeatures.StripEnumPrefix stripEnumPrefix,
                GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode optimizeMode
        ) {
            this.legacyUnmarshalJsonEnum = legacyUnmarshalJsonEnum;
            this.apiLevel = apiLevel;
            this.stripEnumPrefix = stripEnumPrefix;
            this.optimizeMode = optimizeMode;
        }

        /**
         * Returns the value of the {@code legacy_unmarshal_json_enum} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code legacy_unmarshal_json_enum}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<Boolean> legacyUnmarshalJsonEnum() {
            return Optional.ofNullable(legacyUnmarshalJsonEnum);
        }

        /**
         * Returns the value of the {@code api_level} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code api_level}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<GoFeaturesProto.GoFeatures.APILevel> apiLevel() {
            return Optional.ofNullable(apiLevel);
        }

        /**
         * Returns the value of the {@code strip_enum_prefix} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code strip_enum_prefix}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<GoFeaturesProto.GoFeatures.StripEnumPrefix> stripEnumPrefix() {
            return Optional.ofNullable(stripEnumPrefix);
        }

        /**
         * Returns the value of the {@code optimize_mode} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code optimize_mode}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode> optimizeMode() {
            return Optional.ofNullable(optimizeMode);
        }
    }
}

package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import java.util.Optional;

/**
 * Holder of the Daedalus models declared by {@code google/protobuf/cpp_features.proto}.
 *
 * <p>These models represent the C++ specific feature set carried by the
 * {@code (pb.cpp)} extension of {@link DescriptorProtos.FeatureSet}.
 *
 * <p>The models mirror the {@code proto2} definitions of the official protobuf
 * distribution field by field, including field numbers, wire types, field
 * presence and repeated field encodings. This class only scopes the nested
 * types and cannot be instantiated.
 */
public final class CppFeaturesProto {
    /**
     * Prevents the instantiation of this holder class.
     */
    private CppFeaturesProto() {}

    /**
     * <p>Protobuf type {@code pb.CppFeatures}
     */
    @ProtobufMessage
    public static final class CppFeatures {

        /**
         * <p>Protobuf enum {@code pb.CppFeatures.StringType}
         */
        @ProtobufEnum
        public enum StringType {
            @ProtobufEnum.Constant(index = 0)
            STRING_TYPE_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            VIEW,

            @ProtobufEnum.Constant(index = 2)
            CORD,

            @ProtobufEnum.Constant(index = 3)
            STRING;
        }

        /**
         * Whether or not to treat an enum field as closed. This option is only
         * applicable to enum fields, and will be removed in the future. It is
         * consistent with the legacy behavior of using proto3 enum types for
         * proto2 fields.
         *
         * <p><code>bool legacy_closed_enum = 1;</code>
         */
        @ProtobufMessage.BoolField(index = 1)
        Boolean legacyClosedEnum;

        /**
         * <p><code>StringType string_type = 2;</code>
         */
        @ProtobufMessage.EnumField(index = 2)
        CppFeaturesProto.CppFeatures.StringType stringType;

        /**
         * <p><code>bool enum_name_uses_string_view = 3;</code>
         */
        @ProtobufMessage.BoolField(index = 3)
        Boolean enumNameUsesStringView;

        /**
         * Constructs a new {@code CppFeatures} with the specified field values.
         *
         * @param legacyClosedEnum the value of the {@code legacy_closed_enum} field, or {@code null} if the
         *                         field is not set
         * @param stringType the value of the {@code string_type} field, or {@code null} if the
         *                   field is not set
         * @param enumNameUsesStringView the value of the {@code enum_name_uses_string_view} field, or {@code null} if the
         *                               field is not set
         */
        CppFeatures(
                Boolean legacyClosedEnum,
                CppFeaturesProto.CppFeatures.StringType stringType,
                Boolean enumNameUsesStringView
        ) {
            this.legacyClosedEnum = legacyClosedEnum;
            this.stringType = stringType;
            this.enumNameUsesStringView = enumNameUsesStringView;
        }

        /**
         * Returns the value of the {@code legacy_closed_enum} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code legacy_closed_enum}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<Boolean> legacyClosedEnum() {
            return Optional.ofNullable(legacyClosedEnum);
        }

        /**
         * Returns the value of the {@code string_type} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code string_type}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<CppFeaturesProto.CppFeatures.StringType> stringType() {
            return Optional.ofNullable(stringType);
        }

        /**
         * Returns the value of the {@code enum_name_uses_string_view} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code enum_name_uses_string_view}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<Boolean> enumNameUsesStringView() {
            return Optional.ofNullable(enumNameUsesStringView);
        }
    }
}

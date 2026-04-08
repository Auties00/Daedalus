package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

public final class CppFeaturesProto {
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
        boolean legacyClosedEnum;

        /**
         * <p><code>StringType string_type = 2;</code>
         */
        @ProtobufMessage.EnumField(index = 2)
        CppFeaturesProto.CppFeatures.StringType stringType;

        /**
         * <p><code>bool enum_name_uses_string_view = 3;</code>
         */
        @ProtobufMessage.BoolField(index = 3)
        boolean enumNameUsesStringView;

        CppFeatures(
                boolean legacyClosedEnum,
                CppFeaturesProto.CppFeatures.StringType stringType,
                boolean enumNameUsesStringView
        ) {
            this.legacyClosedEnum = legacyClosedEnum;
            this.stringType = stringType;
            this.enumNameUsesStringView = enumNameUsesStringView;
        }

        public boolean legacyClosedEnum() {
            return legacyClosedEnum;
        }

        public CppFeaturesProto.CppFeatures.StringType stringType() {
            return stringType;
        }

        public boolean enumNameUsesStringView() {
            return enumNameUsesStringView;
        }
    }
}

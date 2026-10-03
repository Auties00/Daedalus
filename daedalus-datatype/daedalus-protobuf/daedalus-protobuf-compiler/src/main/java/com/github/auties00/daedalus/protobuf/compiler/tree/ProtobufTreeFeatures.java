package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufValueTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufEnumConstantExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufFloatingPointExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufFloatingPoint;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufEnumTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;
import com.github.auties00.daedalus.protobuf.model.*;
import com.google.protobuf.CppFeaturesProto;
import com.google.protobuf.CppFeaturesProtoCppFeaturesBuilder;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosFeatureSetBuilder;
import com.google.protobuf.GoFeaturesProto;
import com.google.protobuf.GoFeaturesProtoGoFeaturesBuilder;
import com.google.protobuf.JavaFeaturesProto;
import com.google.protobuf.JavaFeaturesProtoJavaFeaturesBuilder;
import com.google.protobuf.JavaMutableFeaturesProto;
import com.google.protobuf.JavaMutableFeaturesProtoJavaMutableFeaturesBuilder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility for resolving edition features
 * </p>
 */
final class ProtobufTreeFeatures {
    private ProtobufTreeFeatures() {
        throw new UnsupportedOperationException("ProtobufTreeFeatures is a utility class and cannot be instantiated");
    }

    /**
     * Resolves the field presence for the given tree node by walking up the AST.
     *
     * @param node the starting node
     *
     * @return the resolved field presence
     */
    static ProtobufFieldPresence fieldPresence(ProtobufTree node) {
        return resolveFeature(node, "field_presence", ProtobufFieldPresence::of, version -> switch (version) {
            case PROTOBUF_3 -> ProtobufFieldPresence.IMPLICIT;
            case PROTOBUF_2, EDITION_2023, EDITION_2024 -> ProtobufFieldPresence.EXPLICIT;
        });
    }

    /**
     * Resolves the enum type for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved enum type
     */
    static ProtobufEnumType enumType(ProtobufTree node) {
        return resolveFeature(node, "enum_type", ProtobufEnumType::of, version -> switch (version) {
            case PROTOBUF_2 -> ProtobufEnumType.CLOSED;
            case PROTOBUF_3, EDITION_2023, EDITION_2024 -> ProtobufEnumType.OPEN;
        });
    }

    /**
     * Resolves the repeated field encoding for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved repeated field encoding
     */
    static ProtobufRepeatedFieldEncoding repeatedFieldEncoding(ProtobufTree node) {
        return resolveFeature(node, "repeated_field_encoding", ProtobufRepeatedFieldEncoding::of, version -> switch (version) {
            case PROTOBUF_2 -> ProtobufRepeatedFieldEncoding.EXPANDED;
            case PROTOBUF_3, EDITION_2023, EDITION_2024 -> ProtobufRepeatedFieldEncoding.PACKED;
        });
    }

    /**
     * Resolves the UTF8 validation mode for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved UTF8 validation
     */
    static ProtobufUtf8Validation utf8Validation(ProtobufTree node) {
        return resolveFeature(node, "utf8_validation", ProtobufUtf8Validation::of, version -> switch (version) {
            case PROTOBUF_2 -> ProtobufUtf8Validation.NONE;
            case PROTOBUF_3, EDITION_2023, EDITION_2024 -> ProtobufUtf8Validation.VERIFY;
        });
    }

    /**
     * Resolves the message encoding for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved message encoding
     */
    static ProtobufMessageEncoding messageEncoding(ProtobufTree node) {
        return resolveFeature(node, "message_encoding", ProtobufMessageEncoding::of, _ -> ProtobufMessageEncoding.LENGTH_PREFIXED);
    }

    /**
     * Resolves the JSON compatibility for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved JSON compatibility
     */
    static ProtobufJsonCompatibility jsonCompatibility(ProtobufTree node) {
        return resolveFeature(node, "json_format", ProtobufJsonCompatibility::of, version -> switch (version) {
            case PROTOBUF_2 -> ProtobufJsonCompatibility.DISABLED;
            case PROTOBUF_3, EDITION_2023, EDITION_2024 -> ProtobufJsonCompatibility.ENABLED;
        });
    }

    /**
     * Resolves the naming style for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved naming style
     */
    static ProtobufNamingStyle namingStyle(ProtobufTree node) {
        return resolveFeature(node, "enforce_naming_style", ProtobufNamingStyle::of, version -> switch (version) {
            case EDITION_2024 -> ProtobufNamingStyle.STYLE2024;
            case PROTOBUF_2, PROTOBUF_3, EDITION_2023 -> ProtobufNamingStyle.STYLE_LEGACY;
        });
    }


    private static <T> T resolveFeature(ProtobufTree node, String featureName, Function<String, Optional<T>> parser, Function<ProtobufVersion, T> defaultProvider) {
        var current = node;
        while(current != null) {
            var value = getFeatureValue(current, featureName);
            if(value.isPresent()) {
                var result = parser.apply(value.get());
                if(result.isPresent()) {
                    return result.get();
                }
            }

            if(current instanceof ProtobufDocumentTree document) {
                return defaultProvider.apply(document.version());
            }

            current = current.parent();
        }

        throw new IllegalStateException("Tree node is not attached to a document");
    }

    private static Optional<String> getFeatureValue(ProtobufTree node, String featureName) {
        return switch (node) {
            case ProtobufTree.WithOptions withOptions -> withOptions.options()
                    .stream()
                    .filter(option -> isFeatureOption(option.name(), featureName))
                    .findFirst()
                    .map(option -> option.value() instanceof ProtobufEnumConstantExpression(var name) ? name : null);
            case ProtobufTree.WithBody<?> withBody -> withBody.getDirectChildrenByType(ProtobufOptionStatement.class)
                    .filter(option -> isFeatureOption(option.name(), featureName))
                    .findFirst()
                    .map(statement -> statement.value() instanceof ProtobufEnumConstantExpression(var name) ? name : null);
            default -> Optional.empty();
        };
    }

    private static boolean isFeatureOption(ProtobufOptionName name, String featureName) {
        var segments = name.segments();
        if(segments.size() != 2) {
            return false;
        }

        var first = segments.get(0);
        var second = segments.get(1);
        return first.hasName("features") && !first.isExtension()
               && second.hasName(featureName) && !second.isExtension();
    }

    /**
     * Returns the builder of a {@link DescriptorProtos.FeatureSet} containing only the features that are
     * explicitly declared on the given node, without inheriting from parent scopes or
     * filling in edition defaults.
     *
     * @param node the AST node whose explicit features should be transcribed
     * @return the builder of a {@code FeatureSet} containing the declared features, or
     *         {@code null} if no recognized {@code features.*} option is present on the node
     */
    static DescriptorProtosFeatureSetBuilder featureSet(ProtobufTree node) {
        var standard = new HashMap<String, ProtobufExpression>();
        var cpp = new HashMap<String, ProtobufExpression>();
        var java = new HashMap<String, ProtobufExpression>();
        var go = new HashMap<String, ProtobufExpression>();
        var javaMutable = new HashMap<String, ProtobufExpression>();
        switch (node) {
            case ProtobufTree.WithOptions withOptions -> {
                for (var option : withOptions.options()) {
                    var optionName = option.name();
                    var optionValue = option.value();
                    if (optionName == null || optionValue == null) {
                        continue;
                    }
                    classifyFeatureOption(optionName.segments(), optionValue, standard, cpp, java, go, javaMutable);
                }
            }
            case ProtobufTree.WithBody<?> withBody -> {
                var optionStatements = withBody.getDirectChildrenByType(ProtobufOptionStatement.class).toList();
                for (var option : optionStatements) {
                    if (!option.hasName() || !option.hasValue()) {
                        continue;
                    }
                    classifyFeatureOption(option.name().segments(), option.value(), standard, cpp, java, go, javaMutable);
                }
            }
            default -> {}
        }

        if (standard.isEmpty() && cpp.isEmpty() && java.isEmpty() && go.isEmpty() && javaMutable.isEmpty()) {
            return null;
        }

        var builder = new DescriptorProtosFeatureSetBuilder();
        if (standard.get("field_presence") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.fieldPresence(DescriptorProtos.FeatureSet.FieldPresence.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown field_presence literal
            }
        }
        if (standard.get("enum_type") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.enumType(DescriptorProtos.FeatureSet.EnumType.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown enum_type literal
            }
        }
        if (standard.get("repeated_field_encoding") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.repeatedFieldEncoding(DescriptorProtos.FeatureSet.RepeatedFieldEncoding.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown repeated_field_encoding literal
            }
        }
        if (standard.get("utf8_validation") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.utf8Validation(DescriptorProtos.FeatureSet.Utf8Validation.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown utf8_validation literal
            }
        }
        if (standard.get("message_encoding") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.messageEncoding(DescriptorProtos.FeatureSet.MessageEncoding.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown message_encoding literal
            }
        }
        if (standard.get("json_format") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.jsonFormat(DescriptorProtos.FeatureSet.JsonFormat.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown json_format literal
            }
        }
        if (standard.get("enforce_naming_style") instanceof ProtobufEnumConstantExpression(var name)) {
            try {
                builder.enforceNamingStyle(DescriptorProtos.FeatureSet.EnforceNamingStyle.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown enforce_naming_style literal
            }
        }
        var cppFeatures = explicitCppFeatures(cpp);
        if (cppFeatures != null) {
            builder.cppFeatures(cppFeatures);
        }
        var javaFeatures = explicitJavaFeatures(java);
        if (javaFeatures != null) {
            builder.javaFeatures(javaFeatures);
        }
        var goFeatures = explicitGoFeatures(go);
        if (goFeatures != null) {
            builder.goFeatures(goFeatures);
        }
        var javaMutableFeatures = explicitJavaMutableFeatures(javaMutable);
        if (javaMutableFeatures != null) {
            builder.javaMutableFeatures(javaMutableFeatures);
        }
        return builder;
    }

    private static void classifyFeatureOption(
            List<ProtobufOptionNameSegment> segments,
            ProtobufExpression value,
            Map<String, ProtobufExpression> standard,
            Map<String, ProtobufExpression> cpp,
            Map<String, ProtobufExpression> java,
            Map<String, ProtobufExpression> go,
            Map<String, ProtobufExpression> javaMutable
    ) {
        switch (segments.size()) {
            case 2 -> {
                var first = segments.get(0);
                var second = segments.get(1);
                if (first.hasName("features") && !first.isExtension() && !second.isExtension()) {
                    standard.put(second.name(), value);
                }
            }
            case 3 -> {
                var first = segments.get(0);
                var second = segments.get(1);
                var third = segments.get(2);
                if (!first.hasName("features") || first.isExtension()
                        || !second.isExtension() || third.isExtension()) {
                    return;
                }
                switch (second.name()) {
                    case "pb.cpp" -> cpp.put(third.name(), value);
                    case "pb.java" -> java.put(third.name(), value);
                    case "pb.go" -> go.put(third.name(), value);
                    case "pb.java_mutable" -> javaMutable.put(third.name(), value);
                    default -> {
                        // Unknown extension namespace; falls through to uninterpreted_option
                    }
                }
            }
            default -> {
                // Single segment options are not features; multi segment paths
                // beyond three segments are not currently modeled.
            }
        }
    }

    private static CppFeaturesProtoCppFeaturesBuilder explicitCppFeatures(Map<String, ProtobufExpression> options) {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new CppFeaturesProtoCppFeaturesBuilder();
        if (options.get("legacy_closed_enum") instanceof ProtobufBoolExpression(var flag)) {
            builder.legacyClosedEnum(flag);
        }
        if (options.get("string_type") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.stringType(CppFeaturesProto.CppFeatures.StringType.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown cpp string_type literal
            }
        }
        if (options.get("enum_name_uses_string_view") instanceof ProtobufBoolExpression(var flag)) {
            builder.enumNameUsesStringView(flag);
        }
        return builder;
    }

    private static JavaFeaturesProtoJavaFeaturesBuilder explicitJavaFeatures(Map<String, ProtobufExpression> options) {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new JavaFeaturesProtoJavaFeaturesBuilder();
        if (options.get("legacy_closed_enum") instanceof ProtobufBoolExpression(var flag)) {
            builder.legacyClosedEnum(flag);
        }
        if (options.get("utf8_validation") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.utf8Validation(JavaFeaturesProto.JavaFeatures.Utf8Validation.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown java utf8_validation literal
            }
        }
        if (options.get("large_enum") instanceof ProtobufBoolExpression(var flag)) {
            builder.largeEnum(flag);
        }
        if (options.get("use_old_outer_classname_default") instanceof ProtobufBoolExpression(var flag)) {
            builder.useOldOuterClassnameDefault(flag);
        }
        if (options.get("nest_in_file_class") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.nestInFileClass(JavaFeaturesProto.JavaFeatures.NestInFileClassFeature.NestInFileClass.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown nest_in_file_class literal
            }
        }
        return builder;
    }

    private static GoFeaturesProtoGoFeaturesBuilder explicitGoFeatures(Map<String, ProtobufExpression> options) {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new GoFeaturesProtoGoFeaturesBuilder();
        if (options.get("legacy_unmarshal_json_enum") instanceof ProtobufBoolExpression(var flag)) {
            builder.legacyUnmarshalJsonEnum(flag);
        }
        if (options.get("api_level") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.apiLevel(GoFeaturesProto.GoFeatures.APILevel.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown go api_level literal
            }
        }
        if (options.get("strip_enum_prefix") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.stripEnumPrefix(GoFeaturesProto.GoFeatures.StripEnumPrefix.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown go strip_enum_prefix literal
            }
        }
        if (options.get("optimize_mode") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.optimizeMode(GoFeaturesProto.GoFeatures.OptimizeModeFeature.OptimizeMode.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown go optimize_mode literal
            }
        }
        return builder;
    }

    private static JavaMutableFeaturesProtoJavaMutableFeaturesBuilder explicitJavaMutableFeatures(Map<String, ProtobufExpression> options) {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new JavaMutableFeaturesProtoJavaMutableFeaturesBuilder();
        if (options.get("nest_in_file_class") instanceof ProtobufEnumConstantExpression(var constantName)) {
            try {
                builder.nestInFileClass(JavaMutableFeaturesProto.JavaMutableFeatures.NestInFileClassFeature.NestInFileClass.valueOf(constantName));
            } catch (IllegalArgumentException ignored) {
                // Unknown java_mutable nest_in_file_class literal
            }
        }
        return builder;
    }

    /**
     * Walks the body-style {@link ProtobufOptionStatement} children of the
     * given AST container and collects every single segment custom-extension
     * option whose definition was resolved by the analyzer into a fresh
     * {@link ProtobufUnknownFields}.
     *
     * <p>This is the bridge from the AST option layer to the unknown fields
     * sink that the codegen iterates during serialization. Multi segment
     * options (including {@code features.X} and nested-message paths) are
     * deliberately skipped: features are routed to the typed
     * {@code features} field by {@link #featureSet}, and nested-message
     * extensions require recursive sub-message construction that the AST
     * does not yet model.
     *
     * @param node the enclosing AST container (a {@code ProtobufDocumentTree},
     *             {@code ProtobufMessageStatement}, etc.)
     * @return a freshly allocated store containing the resolved custom
     *         extensions; never {@code null}, but may be empty
     */
    static ProtobufUnknownFields collectUnknownExtensions(ProtobufTree.WithBody<?> node) {
        var store = new ProtobufUnknownFields();
        node.getDirectChildrenByType(ProtobufOptionStatement.class).forEach(option -> {
            if (!option.hasName() || !option.hasValue()) {
                return;
            }
            var segments = option.name().segments();
            if (segments.size() != 1) {
                return;
            }
            if (!segments.getFirst().isExtension()) {
                return;
            }
            if (option.definition() instanceof ProtobufFieldStatement fieldDefinition) {
                appendUnknownOption(store, fieldDefinition, option.value());
            }
        });
        return store;
    }

    static ProtobufUnknownFields collectUnknownExtensions(ProtobufTree.WithOptions node) {
        var store = new ProtobufUnknownFields();
        for (var option : node.options()) {
            var segments = option.name().segments();
            if (segments.size() != 1) {
                continue;
            }
            if (!segments.getFirst().isExtension()) {
                continue;
            }
            if (option.definition() instanceof ProtobufFieldStatement fieldDefinition) {
                appendUnknownOption(store, fieldDefinition, option.value());
            }
        }
        return store;
    }

    /**
     * Encodes the value of an unhandled custom-extension option as a
     * {@link ProtobufUnknownValue.Binary} entry on the given store.
     *
     * <p>This is the bridge from the AST option layer to the wire-level
     * unknown-fields store on a {@code *Options} message. The codegen on the
     * serialization side iterates the store and re-emits each entry as a
     * regular wire field with the original field index, which matches what
     * {@code protoc} produces for resolved custom extensions.
     *
     * <p>The conversion is driven by the option's resolved
     * {@link ProtobufFieldStatement} definition, which carries both the
     * extension field number and the wire type. Only scalar protobuf types
     * are supported today; {@code MESSAGE}, {@code GROUP}, and {@code MAP}
     * extensions are silently skipped because they require recursive
     * sub-message serialization that the AST does not yet model. Enum
     * extensions are also skipped pending a constant-resolution helper.
     *
     * @param store      the unknown-fields sink to populate
     * @param definition the resolved extension field declaration
     * @param value      the AST value supplied by the user
     */
    static void appendUnknownOption(
            ProtobufUnknownFields store,
            ProtobufFieldStatement definition,
            ProtobufExpression value
    ) {
        if (definition == null || !definition.hasIndex() || !definition.hasType()) {
            return;
        }
        var fieldNumber = (int) definition.index().value();
        var encoded = encodeOptionValue(definition.type(), value);
        if (encoded != null) {
            store.add(fieldNumber, encoded);
        }
    }

    /**
     * Converts an AST {@link ProtobufExpression} into the matching
     * {@link ProtobufUnknownValue.Binary} subtype for the given protobuf type
     * reference. Returns {@code null} when the value cannot be encoded.
     */
    private static ProtobufUnknownValue.Binary encodeOptionValue(ProtobufTypeReference typeReference, ProtobufExpression value) {
        if (!(typeReference instanceof ProtobufValueTypeReference valueType)) {
            return null;
        }

        return switch (valueType.protobufType()) {
            case BOOL -> value instanceof ProtobufBoolExpression(var flag)
                    ? new ProtobufUnknownValue.Binary.VarInt(flag ? 1 : 0)
                    : null;
            case INT32, INT64, UINT32, UINT64 -> value instanceof ProtobufIntegerExpression(ProtobufInteger(var data))
                    ? new ProtobufUnknownValue.Binary.VarInt(data)
                    : null;
            case SINT32 -> value instanceof ProtobufIntegerExpression(ProtobufInteger(var data))
                    ? new ProtobufUnknownValue.Binary.VarInt((data << 1) ^ (data >> 31))
                    : null;
            case SINT64 -> value instanceof ProtobufIntegerExpression(ProtobufInteger(var data))
                    ? new ProtobufUnknownValue.Binary.VarInt((data << 1) ^ (data >> 63))
                    : null;
            case FIXED32, SFIXED32 -> value instanceof ProtobufIntegerExpression(ProtobufInteger(var data))
                    ? new ProtobufUnknownValue.Binary.Fixed32(Math.toIntExact(data))
                    : null;
            case FIXED64, SFIXED64 -> value instanceof ProtobufIntegerExpression(ProtobufInteger(var data))
                    ? new ProtobufUnknownValue.Binary.Fixed64(data)
                    : null;
            case FLOAT -> switch (value) {
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.Finite(var data)) -> {
                    var truncated = (float) data;
                    if((double) truncated != data && Double.isFinite(data)) {
                        throw new ArithmeticException("float overflow"); // https://github.com/openjdk/jdk/pull/8548
                    } else {
                        yield new ProtobufUnknownValue.Binary.Fixed32(Float.floatToRawIntBits(truncated));
                    }
                }
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.Infinity(var signum)) -> switch (signum) {
                    case POSITIVE -> ProtobufUnknownValue.Binary.Fixed32.POSITIVE_INFINITY;
                    case NEGATIVE -> ProtobufUnknownValue.Binary.Fixed32.NEGATIVE_INFINITY;
                };
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.NaN()) -> ProtobufUnknownValue.Binary.Fixed32.NaN;
                default -> null;
            };
            case DOUBLE -> switch (value) {
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.Finite(var data)) -> new ProtobufUnknownValue.Binary.Fixed64(Double.doubleToRawLongBits(data));
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.Infinity(var signum)) -> switch (signum) {
                    case POSITIVE -> ProtobufUnknownValue.Binary.Fixed64.POSITIVE_INFINITY;
                    case NEGATIVE -> ProtobufUnknownValue.Binary.Fixed64.NEGATIVE_INFINITY;
                };
                case ProtobufFloatingPointExpression(ProtobufFloatingPoint.NaN()) -> ProtobufUnknownValue.Binary.Fixed64.NaN;
                default -> null;
            };
            case STRING -> value instanceof ProtobufLiteralExpression(var data)
                    ? new ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(data.getBytes(StandardCharsets.UTF_8))
                    : null;
            case BYTES -> value instanceof ProtobufLiteralExpression(var data)
                    ? new ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(data.getBytes(StandardCharsets.ISO_8859_1))
                    : null;
            case ENUM -> {
                if (!(typeReference instanceof ProtobufEnumTypeReference(var declaration))) {
                    yield null;
                }
                if (!(value instanceof ProtobufEnumConstantExpression(var constantName))) {
                    yield null;
                }
                var constant = declaration.getDirectChildByNameAndType(constantName, ProtobufEnumConstantStatement.class);
                if (constant.isEmpty() || !constant.get().hasIndex()) {
                    yield null;
                }
                yield new ProtobufUnknownValue.Binary.VarInt(constant.get().index().value());
            }
            case MESSAGE -> null;
        };
    }
}
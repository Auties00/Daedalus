package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufEnumConstantExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufExpression;
import com.github.auties00.daedalus.protobuf.model.*;

import java.util.Optional;
import java.util.function.Function;

/**
 * Utility for resolving edition feature values from any position in the AST.
 * <p>
 * Features are resolved by walking up the tree from a given node, checking for explicit
 * {@code features.<name>} options at each level. If no override is found, the document
 * level default (based on the protobuf version) is used.
 * </p>
 * <p>
 * For nodes with compact options ({@link ProtobufTree.WithOptions}), the feature is looked
 * up in the options list. For nodes with bodies ({@link ProtobufTree.WithBody}), it is looked
 * up in child {@link ProtobufOptionStatement} entries.
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
        return resolve(node, "field_presence", ProtobufFieldPresence::of, version -> switch (version) {
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
        return resolve(node, "enum_type", ProtobufEnumType::of, version -> switch (version) {
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
        return resolve(node, "repeated_field_encoding", ProtobufRepeatedFieldEncoding::of, version -> switch (version) {
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
        return resolve(node, "utf8_validation", ProtobufUtf8Validation::of, version -> switch (version) {
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
        return resolve(node, "message_encoding", ProtobufMessageEncoding::of, _ -> ProtobufMessageEncoding.LENGTH_PREFIXED);
    }

    /**
     * Resolves the JSON compatibility for the given tree node by walking up the AST.
     *
     * @param node the starting node
     * @return the resolved JSON compatibility
     */
    static ProtobufJsonCompatibility jsonCompatibility(ProtobufTree node) {
        return resolve(node, "json_format", ProtobufJsonCompatibility::of, version -> switch (version) {
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
        return resolve(node, "enforce_naming_style", ProtobufNamingStyle::of, version -> switch (version) {
            case EDITION_2024 -> ProtobufNamingStyle.STYLE2024;
            case PROTOBUF_2, PROTOBUF_3, EDITION_2023 -> ProtobufNamingStyle.STYLE_LEGACY;
        });
    }

    private static <T> T resolve(ProtobufTree node, String featureName, Function<String, Optional<T>> parser, Function<ProtobufVersion, T> defaultProvider) {
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
            case ProtobufTree.WithOptions withOptions -> getFeatureFromCompactOptions(withOptions, featureName);
            case ProtobufTree.WithBody<?> withBody -> getFeatureFromChildOptions(withBody, featureName);
            default -> Optional.empty();
        };
    }

    private static Optional<String> getFeatureFromCompactOptions(ProtobufTree.WithOptions node, String featureName) {
        return node.options()
                .stream()
                .filter(option -> isFeatureOption(option.name(), featureName))
                .findFirst()
                .map(option -> getEnumConstantName(option.value()));
    }

    private static Optional<String> getFeatureFromChildOptions(ProtobufTree.WithBody<?> node, String featureName) {
        return node.getDirectChildrenByType(ProtobufOptionStatement.class)
                .filter(option -> isFeatureOption(option.name(), featureName))
                .findFirst()
                .map(statement -> getEnumConstantName(statement.value()));
    }

    private static String getEnumConstantName(ProtobufExpression value) {
        return value instanceof ProtobufEnumConstantExpression(String name) ? name : null;
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
}
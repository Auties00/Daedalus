
package it.auties.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the supported Protocol Buffer (protobuf) specification versions and editions.
 * <p>
 * This enum provides constants for the major protobuf versions ({@code proto2}, {@code proto3})
 * and protobuf editions ({@code 2023}, {@code 2024}), along with utilities
 * for version management and lookup operations.
 * </p>
 *
 * @see <a href="https://protobuf.dev/programming-guides/proto2/">Protocol Buffers Version 2 Guide</a>
 * @see <a href="https://protobuf.dev/programming-guides/proto3/">Protocol Buffers Version 3 Guide</a>
 * @see <a href="https://protobuf.dev/editions/overview/">Protocol Buffers Editions Overview</a>
 */
public enum ProtobufVersion {
    /**
     * Protocol Buffers version 2 (proto2).
     */
    PROTOBUF_2("proto2"),

    /**
     * Protocol Buffers version 3 (proto3).
     */
    PROTOBUF_3("proto3"),

    /**
     * Protocol Buffers Edition 2023.
     * <p>
     * The inaugural edition that unified proto2 and proto3 semantics under a single
     * system of configurable feature flags.
     */
    EDITION_2023("2023"),

    /**
     * Protocol Buffers Edition 2024.
     * <p>
     * Builds on Edition 2023 with new language syntax ({@code import option},
     * {@code export}/{@code local} keywords) and new features
     * ({@code default_symbol_visibility}, {@code enforce_naming_style}).
     */
    EDITION_2024("2024");

    /**
     * The default protobuf version used when no version is explicitly specified.
     * Currently defaults to {@link #PROTOBUF_2}.
     */
    private static final ProtobufVersion DEFAULT_VERSION = PROTOBUF_2;

    /**
     * A lookup map for efficient version resolution by version code string.
     * Maps lowercase version codes to their corresponding enum constants.
     */
    private static final Map<String, ProtobufVersion> BY_VERSION_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.versionCode.toLowerCase(), Function.identity()));

    /**
     * The string representation of this protobuf version as used in .proto files.
     */
    private final String versionCode;

    /**
     * Constructs a ProtobufVersion with the specified version code.
     *
     * @param versionCode the string representation of the version (e.g., "proto2", "proto3", "2023", "2024")
     */
    ProtobufVersion(String versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * Resolves a ProtobufVersion from its string representation.
     * <p>
     * The lookup is case-insensitive, so both "proto2" and "PROTO2" will
     * return {@link #PROTOBUF_2}.
     * </p>
     *
     * @param name the version string to look up (e.g., "proto2", "proto3", "2023", "2024")
     * @return an Optional containing the matching ProtobufVersion, or empty if no match is found
     * @see #versionCode()
     */
    public static Optional<ProtobufVersion> of(String name) {
        return Optional.ofNullable(BY_VERSION_CODE.get(name));
    }

    /**
     * Returns the default protobuf version.
     * <p>
     * This is used when no explicit version is specified in protobuf processing.
     * Currently returns {@link #PROTOBUF_2} for backward compatibility.
     * </p>
     *
     * @return the default ProtobufVersion
     */
    public static ProtobufVersion defaultVersion() {
        return DEFAULT_VERSION;
    }

    /**
     * Returns whether this version is a protobuf edition (2023 or later)
     * rather than a classic syntax version (proto2/proto3).
     *
     * @return {@code true} if this is an edition, {@code false} if this is a classic syntax version
     */
    public boolean isEdition() {
        return this == EDITION_2023 || this == EDITION_2024;
    }

    /**
     * Returns the version code string for this protobuf version.
     * <p>
     * This is the string representation as it appears in .proto file syntax or edition
     * declarations (e.g., {@code syntax = "proto2";} or {@code edition = "2023";}).
     * </p>
     *
     * @return the version code string
     */
    public String versionCode() {
        return this.versionCode;
    }

    /**
     * Returns the version code string representation of this protobuf version.
     * <p>
     * This method delegates to {@link #versionCode()} for consistency.
     * </p>
     *
     * @return the version code string
     */
    @Override
    public String toString() {
        return versionCode();
    }
}
package com.github.auties00.daedalus.protobuf.cli.model;

/**
 * Specifies how a protobuf enum is represented as a Java type.
 */
public enum EnumForm {
    /**
     * A standard Java enum.
     */
    JAVA_ENUM,

    /**
     * A concrete final class with static final constant fields.
     */
    CLASS,

    /**
     * A Java record with static final constant fields.
     */
    RECORD
}

package com.github.auties00.daedalus.typesystem.model;

/**
 * A modifier applicable to a method declaration, such as '{@code public}'
 * or '{@code static}'.
 *
 * <p>Not all modifiers are applicable to all kinds of method declarations.
 * For example, {@code abstract} cannot be combined with {@code final},
 * {@code static}, {@code synchronized}, {@code native}, or {@code strictfp}.
 * When two or more modifiers appear in the source code of a method then it
 * is customary, though not required, that they appear in the same order as
 * the constants listed in the detail section below.
 */
public enum MethodModifier {
    /**
     * The modifier {@code public}.
     */
    PUBLIC,

    /**
     * The modifier {@code protected}.
     */
    PROTECTED,

    /**
     * The modifier {@code private}.
     */
    PRIVATE,

    /**
     * The modifier {@code abstract}.
     */
    ABSTRACT,

    /**
     * The modifier {@code default}.
     */
    DEFAULT,

    /**
     * The modifier {@code static}.
     */
    STATIC,

    /**
     * The modifier {@code final}.
     */
    FINAL,

    /**
     * The modifier {@code synchronized}.
     */
    SYNCHRONIZED,

    /**
     * The modifier {@code native}.
     */
    NATIVE;

    /**
     * Returns this modifier's name
     *
     * @return the modifier's name
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
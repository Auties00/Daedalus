package com.github.auties00.daedalus.typesystem.model;

/**
 * A modifier applicable to a class or interface declaration, such as
 * '{@code public}' or '{@code final}'.
 *
 * <p>Not all modifiers are applicable to all kinds of class declarations.
 * For example, {@code abstract} cannot be combined with {@code final},
 * and {@code sealed} cannot be combined with {@code non-sealed} or
 * {@code final}. When two or more modifiers appear in the source code of
 * a class then it is customary, though not required, that they appear in
 * the same order as the constants listed in the detail section below.
 */
public enum ClassModifier {
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
     * The modifier {@code static}.
     */
    STATIC,

    /**
     * The modifier {@code sealed}.
     */
    SEALED,

    /**
     * The modifier {@code non-sealed}.
     */
    NON_SEALED {
        @Override
        public String toString() {
            return "non-sealed";
        }
    },

    /**
     * The modifier {@code final}.
     */
    FINAL,

    /**
     * The modifier {@code strictfp}.
     */
    STRICTFP;

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
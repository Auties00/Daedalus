package com.github.auties00.daedalus.cli.tui;

/**
 * Implemented by the TUI model of an extension to tell the application that the user went
 * back from the first screen of the extension, so that the application shows the screen that
 * opened the extension instead of ending.
 *
 * <p>An extension that wants to end the application returns {@code Command.quit()} as usual.
 */
public interface BackNavigation {
    /**
     * Returns whether the user went back from the first screen of the extension.
     *
     * @return {@code true} if the application has to show the screen that opened the extension
     */
    boolean isBackRequested();
}

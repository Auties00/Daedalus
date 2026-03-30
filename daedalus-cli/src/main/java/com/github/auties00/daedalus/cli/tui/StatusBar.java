package com.github.auties00.daedalus.cli.tui;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a bottom status bar showing available key bindings.
 *
 * <p>Each entry consists of a key combination and a description of
 * the action it triggers, separated by a vertical bar delimiter.
 */
public final class StatusBar {
    private final List<String> entries;

    /**
     * Constructs an empty status bar.
     */
    public StatusBar() {
        this.entries = new ArrayList<>();
    }

    /**
     * Adds a key binding entry to the status bar.
     *
     * @param key the key combination (e.g., {@code "up/down"}, {@code "enter"})
     * @param action the action description (e.g., {@code "navigate"}, {@code "confirm"})
     * @return this status bar for chaining
     */
    public StatusBar add(String key, String action) {
        entries.add(Theme.keyHint(key, action));
        return this;
    }

    /**
     * Renders the status bar as a single-line string.
     *
     * @return the rendered status bar
     */
    public String render() {
        var separator = Theme.STATUS_SEPARATOR.render(" | ");
        return Theme.STATUS_BAR.render(String.join(separator, entries));
    }
}

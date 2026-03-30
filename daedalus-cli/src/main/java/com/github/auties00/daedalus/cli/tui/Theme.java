package com.github.auties00.daedalus.cli.tui;

import com.williamcallahan.tui4j.compat.lipgloss.Borders;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

/**
 * Defines the visual theme for the Daedalus TUI application.
 *
 * <p>Provides a consistent color palette and pre-configured Lip Gloss styles
 * used across all TUI screens. The palette draws inspiration from the legend
 * of Daedalus: warm bronze and gold tones evoking ancient Greek craftsmanship,
 * Mediterranean stone, Aegean waters, and wax-and-feather wings.
 */
public final class Theme {

    private Theme() {
    }

        /**
     * The primary accent color (antique bronze gold).
     */
    public static final Color PRIMARY = Color.color("#D4A44C");

    /**
     * A lighter variant of the primary accent (champagne gold).
     */
    public static final Color PRIMARY_LIGHT = Color.color("#E8C878");

    /**
     * The success color (laurel green).
     */
    public static final Color SUCCESS = Color.color("#8BA858");

    /**
     * The danger/error color (terracotta red).
     */
    public static final Color DANGER = Color.color("#C75B3A");

    /**
     * The warning color (warm amber).
     */
    public static final Color WARNING = Color.color("#E8A830");

    /**
     * The informational color (Aegean blue).
     */
    public static final Color INFO = Color.color("#5B99A8");

    /**
     * The muted/secondary text color (warm stone gray).
     */
    public static final Color MUTED = Color.color("#8B8178");

    /**
     * The primary text color (parchment white).
     */
    public static final Color TEXT = Color.color("#E8DFD0");

    /**
     * The dimmed text color (aged patina).
     */
    public static final Color DIM = Color.color("#5A5248");

    /**
     * The highlight/selected background color (dark walnut).
     */
    public static final Color HIGHLIGHT_BG = Color.color("#2E2418");

        /**
     * The style for the ASCII art logo on the welcome screen.
     */
    public static final Style LOGO = Style.newStyle()
            .bold(true)
            .foreground(PRIMARY);

    /**
     * The style for main titles.
     */
    public static final Style TITLE = Style.newStyle()
            .bold(true)
            .foreground(PRIMARY);

    /**
     * The style for subtitles and section headers.
     */
    public static final Style SUBTITLE = Style.newStyle()
            .bold(true)
            .foreground(PRIMARY_LIGHT);

    /**
     * The style for body text.
     */
    public static final Style BODY = Style.newStyle()
            .foreground(TEXT);

    /**
     * The style for muted/secondary text.
     */
    public static final Style MUTED_TEXT = Style.newStyle()
            .foreground(MUTED);

    /**
     * The style for dimmed text.
     */
    public static final Style DIM_TEXT = Style.newStyle()
            .foreground(DIM);

    /**
     * The style for success messages.
     */
    public static final Style SUCCESS_TEXT = Style.newStyle()
            .foreground(SUCCESS);

    /**
     * The style for error messages.
     */
    public static final Style DANGER_TEXT = Style.newStyle()
            .bold(true)
            .foreground(DANGER);

    /**
     * The style for warning messages.
     */
    public static final Style WARNING_TEXT = Style.newStyle()
            .foreground(WARNING);

    /**
     * The style for informational messages.
     */
    public static final Style INFO_TEXT = Style.newStyle()
            .foreground(INFO);

        /**
     * The style for the currently selected item cursor.
     */
    public static final Style SELECTED_CURSOR = Style.newStyle()
            .foreground(PRIMARY)
            .bold(true);

    /**
     * The style for the selected item text.
     */
    public static final Style SELECTED_ITEM = Style.newStyle()
            .foreground(TEXT)
            .bold(true);

    /**
     * The style for unselected item text.
     */
    public static final Style UNSELECTED_ITEM = Style.newStyle()
            .foreground(MUTED);

        /**
     * The style for the main application frame with a rounded border.
     */
    public static final Style FRAME = Style.newStyle()
            .border(Borders.roundedBorder(), true, true, true, true)
            .borderForeground(PRIMARY)
            .padding(1, 2);

    /**
     * The style for secondary panels (e.g., code preview).
     */
    public static final Style PREVIEW_PANEL = Style.newStyle()
            .border(Borders.normalBorder(), true, true, true, true)
            .borderForeground(DIM)
            .padding(0, 1)
            .width(72);

        /**
     * The style for annotation keywords in code previews.
     */
    public static final Style CODE_ANNOTATION = Style.newStyle()
            .foreground(WARNING);

    /**
     * The style for Java keywords in code previews.
     */
    public static final Style CODE_KEYWORD = Style.newStyle()
            .foreground(PRIMARY_LIGHT);

    /**
     * The style for type names in code previews.
     */
    public static final Style CODE_TYPE = Style.newStyle()
            .foreground(INFO);

    /**
     * The style for string literals in code previews.
     */
    public static final Style CODE_STRING = Style.newStyle()
            .foreground(SUCCESS);

    /**
     * The style for plain code text.
     */
    public static final Style CODE_PLAIN = Style.newStyle()
            .foreground(TEXT);

        /**
     * The style for the bottom status/key-binding bar.
     */
    public static final Style STATUS_BAR = Style.newStyle()
            .foreground(MUTED)
            .padding(0, 1);

    /**
     * The style for key hints in the status bar (e.g., "up/down").
     */
    public static final Style STATUS_KEY = Style.newStyle()
            .foreground(TEXT)
            .bold(true);

    /**
     * The style for key action descriptions in the status bar.
     */
    public static final Style STATUS_ACTION = Style.newStyle()
            .foreground(MUTED);

    /**
     * The style for the separator between status bar items.
     */
    public static final Style STATUS_SEPARATOR = Style.newStyle()
            .foreground(DIM);

        /**
     * The style for a filled section of a progress bar.
     */
    public static final Style PROGRESS_FILLED = Style.newStyle()
            .foreground(PRIMARY);

    /**
     * The style for an empty section of a progress bar.
     */
    public static final Style PROGRESS_EMPTY = Style.newStyle()
            .foreground(DIM);

        /**
     * The style for added lines in a diff view.
     */
    public static final Style DIFF_ADDED = Style.newStyle()
            .foreground(SUCCESS);

    /**
     * The style for removed lines in a diff view.
     */
    public static final Style DIFF_REMOVED = Style.newStyle()
            .foreground(DANGER);

    /**
     * The style for context (unchanged) lines in a diff view.
     */
    public static final Style DIFF_CONTEXT = Style.newStyle()
            .foreground(MUTED);

    /**
     * The style for diff hunk headers.
     */
    public static final Style DIFF_HEADER = Style.newStyle()
            .foreground(INFO)
            .bold(true);

        /**
     * Formats a key binding hint for the status bar.
     *
     * @param key the key combination (e.g., {@code "up/down"})
     * @param action the action description (e.g., {@code "navigate"})
     * @return the formatted key binding string
     */
    public static String keyHint(String key, String action) {
        return STATUS_KEY.render(key) + " " + STATUS_ACTION.render(action);
    }

    /**
     * Formats a step indicator (e.g., {@code "Step 3/9"}).
     *
     * @param current the current step number (1-based)
     * @param total the total number of steps
     * @return the formatted step indicator string
     */
    public static String stepIndicator(int current, int total) {
        return MUTED_TEXT.render("Step " + current + "/" + total);
    }
}

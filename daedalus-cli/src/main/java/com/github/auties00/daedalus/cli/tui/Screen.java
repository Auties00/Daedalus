package com.github.auties00.daedalus.cli.tui;

/**
 * Lays out a screen of an extension the same way as every other screen of the TUI.
 *
 * <p>A screen is made of a header, a body, and a status bar with the available key bindings,
 * all indented by the same amount. The header is the Daedalus lettering followed by a line that
 * names the extension and the action, the same way the home screens present themselves. A
 * terminal that is too short for the lettering gets a one line header instead, so the body
 * keeps the space it needs. Building every screen through this class keeps the typography of the
 * headings, the label and value rows, the status lines and the panels identical.
 */
public final class Screen {
    /**
     * The smallest terminal height at which the header shows the Daedalus lettering.
     */
    public static final int LOGO_MIN_HEIGHT = 36;

    private static final String INDENT = "  ";
    private static final int FULL_HEADER_ROWS = 9;
    private static final int COMPACT_HEADER_ROWS = 3;

    private final String extension;
    private final String action;
    private final boolean full;
    private final StringBuilder body;
    private String indicator;
    private String keys;

    private Screen(String extension, String action, int height) {
        this.extension = extension;
        this.action = action;
        this.full = height >= LOGO_MIN_HEIGHT;
        this.body = new StringBuilder();
        this.indicator = "";
        this.keys = "";
    }

    /**
     * Starts a screen for an action of an extension.
     *
     * @param extension the extension name (e.g., {@code "Protobuf"})
     * @param action the action name (e.g., {@code "Generate"})
     * @param height the height of the terminal, which decides how much room the header takes
     * @return the new screen
     */
    public static Screen of(String extension, String action, int height) {
        return new Screen(extension, action, height);
    }

    /**
     * Returns the number of rows that the header of a screen takes, so that a screen can
     * size the parts of its body that grow with the terminal.
     *
     * @param height the height of the terminal
     * @return the rows used by the header, including the empty line below it
     */
    public static int headerRows(int height) {
        return height >= LOGO_MIN_HEIGHT ? FULL_HEADER_ROWS : COMPACT_HEADER_ROWS;
    }

    /**
     * Renders a bordered panel with a titled rule on top, which is how previews and lists
     * of items are framed on every screen.
     *
     * @param title the title shown in the rule
     * @param content the content of the panel, which may span several lines
     * @return the rendered panel
     */
    public static String panel(String title, String content) {
        var rule = Theme.DIM_TEXT.render("─── " + title + " ")
                + Theme.DIM_TEXT.render("─".repeat(Math.max(3, 40 - title.length())));
        return Theme.PREVIEW_PANEL.render(rule + "\n\n" + content);
    }

    /**
     * Sets the text shown next to the name of the action, such as the current step or phase.
     *
     * @param text the indicator text
     * @return this screen
     */
    public Screen indicator(String text) {
        this.indicator = text;
        return this;
    }

    /**
     * Adds a section heading, followed by an empty line.
     *
     * @param text the heading
     * @return this screen
     */
    public Screen heading(String text) {
        return block(Theme.SUBTITLE.render(text)).blank();
    }

    /**
     * Adds a status line, which is a symbol followed by a sentence, and an empty line.
     *
     * @param symbol the already styled symbol, such as a checkmark or a spinner frame
     * @param text the sentence that describes the status
     * @return this screen
     */
    public Screen result(String symbol, String text) {
        return block(symbol + " " + Theme.BODY.render(text)).blank();
    }

    /**
     * Adds a row made of a muted label and a value.
     *
     * @param label the label
     * @param value the value
     * @return this screen
     */
    public Screen entry(String label, String value) {
        return block(Theme.MUTED_TEXT.render(label + ": ") + Theme.BODY.render(value));
    }

    /**
     * Adds an already styled text, which may span several lines, to the body.
     *
     * @param text the text to add
     * @return this screen
     */
    public Screen block(String text) {
        for (var line : text.split("\n", -1)) {
            body.append(INDENT).append(line).append('\n');
        }
        return this;
    }

    /**
     * Adds an empty line to the body.
     *
     * @return this screen
     */
    public Screen blank() {
        body.append('\n');
        return this;
    }

    /**
     * Sets the status bar shown at the bottom of the screen.
     *
     * @param statusBar the available key bindings
     * @return this screen
     */
    public Screen keys(StatusBar statusBar) {
        this.keys = statusBar.render();
        return this;
    }

    /**
     * Renders the screen.
     *
     * @return the rendered screen
     */
    public String render() {
        var sb = new StringBuilder();
        if (full) {
            for (var line : Theme.logo().split("\n")) {
                sb.append(INDENT).append(line).append('\n');
            }
            sb.append('\n');
            sb.append(INDENT).append(Theme.MUTED_TEXT.render(extension + " › " + action));
            if (!indicator.isEmpty()) {
                sb.append(INDENT).append(Theme.DIM_TEXT.render(indicator));
            }
            sb.append("\n\n");
        } else {
            sb.append('\n');
            sb.append(INDENT).append(Theme.header(extension, action));
            if (!indicator.isEmpty()) {
                sb.append(INDENT).append(Theme.MUTED_TEXT.render(indicator));
            }
            sb.append("\n\n");
        }
        sb.append(body);
        if (!keys.isEmpty()) {
            sb.append('\n');
            sb.append(INDENT).append(keys).append('\n');
        }
        return sb.toString();
    }
}

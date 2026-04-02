package com.github.auties00.daedalus.cli.tui;

import com.github.auties00.daedalus.cli.spi.DaedalusExtension;
import com.williamcallahan.tui4j.compat.bubbletea.*;

import java.util.List;

/**
 * The welcome screen model shown when the CLI is launched without arguments.
 *
 * <p>Displays a large ASCII art "DAEDALUS" title and a list of available
 * extensions for the user to select from. Each extension is shown with
 * its name and description.
 */
public final class WelcomeModel implements Model {
    private static final String[] LOGO_LINES = {
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2557    \u2588\u2588\u2557  \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557",
            "\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2551    \u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D",
            "\u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551    \u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557",
            "\u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u255D \u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551    \u2588\u2588\u2551  \u2588\u2588\u2551\u255A\u2550\u2550\u2550\u2550\u2588\u2588\u2551",
            "\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2551 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551",
            "\u255A\u2550\u2550\u2550\u2550\u2550\u255D \u255A\u2550\u255D \u255A\u2550\u255D\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u2550\u2550\u2550\u2550\u255D \u255A\u2550\u255D \u255A\u2550\u255D\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D"
    };

    private final List<DaedalusExtension> extensions;
    private int cursor;
    private boolean confirmed;
    private boolean cancelled;

    /**
     * Constructs the welcome screen model with the given list of extensions.
     *
     * @param extensions the available extensions to display
     */
    public WelcomeModel(List<DaedalusExtension> extensions) {
        this.extensions = extensions;
        this.cursor = 0;
        this.confirmed = false;
        this.cancelled = false;
    }

    @Override
    public Command init() {
        return Command.none();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof KeyPressMessage key) {
            var keyStr = key.key();
            return switch (keyStr) {
                case "up", "k" -> {
                    cursor = Math.max(0, cursor - 1);
                    yield UpdateResult.from(this);
                }
                case "down", "j" -> {
                    cursor = Math.min(extensions.size() - 1, cursor + 1);
                    yield UpdateResult.from(this);
                }
                case "enter" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "q", "esc", "ctrl+c" -> {
                    cancelled = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                default -> UpdateResult.from(this);
            };
        }
        return UpdateResult.from(this);
    }

    @Override
    public String view() {
        var sb = new StringBuilder();

        sb.append(renderLogo());
        sb.append("\n");

        sb.append(Theme.MUTED_TEXT.render("Modular code generation toolkit"));
        sb.append("\n");
        sb.append(Theme.DIM_TEXT.render("v4.0.0"));
        sb.append("\n\n");

        sb.append(Theme.SUBTITLE.render("Select a module:"));
        sb.append("\n\n");

        for (var i = 0; i < extensions.size(); i++) {
            var ext = extensions.get(i);
            sb.append(renderMenuItem(i, capitalize(ext.name()), ext.description()));
        }
        sb.append("\n");

        var status = new StatusBar()
                .add("\u2191\u2193", "navigate")
                .add("enter", "select")
                .add("q", "quit");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    private String renderLogo() {
        var sb = new StringBuilder();
        for (var line : LOGO_LINES) {
            sb.append(Theme.LOGO.render(line));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String renderMenuItem(int index, String label, String description) {
        var sb = new StringBuilder();
        if (index == cursor) {
            sb.append(Theme.SELECTED_CURSOR.render("  \u25b8 "));
            sb.append(Theme.SELECTED_ITEM.render(label));
            sb.append(Theme.MUTED_TEXT.render("  " + description));
        } else {
            sb.append(Theme.UNSELECTED_ITEM.render("    " + label));
            sb.append(Theme.DIM_TEXT.render("  " + description));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Returns the extension selected by the user.
     *
     * @return the selected extension
     */
    public DaedalusExtension selectedExtension() {
        return extensions.get(cursor);
    }

    /**
     * Returns whether the user confirmed a selection.
     *
     * @return {@code true} if confirmed
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns whether the user cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
}

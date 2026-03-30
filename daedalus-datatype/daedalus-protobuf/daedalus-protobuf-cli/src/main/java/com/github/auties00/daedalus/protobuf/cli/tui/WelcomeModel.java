package com.github.auties00.daedalus.protobuf.cli.tui;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.cli.tui.Theme;
import com.github.auties00.daedalus.cli.tui.shared.StatusBar;

/**
 * The welcome screen model shown when the CLI is launched without arguments.
 *
 * <p>Displays a large ASCII art "DAEDALUS" title with a mode selector
 * for choosing between generation and update workflows.
 */
public final class WelcomeModel implements Model {
    private static final String[] LOGO_LINES = {
            "██████╗  █████╗ ███████╗██████╗  █████╗ ██╗     ██╗   ██╗███████╗",
            "██╔══██╗██╔══██╗██╔════╝██╔══██╗██╔══██╗██║     ██║   ██║██╔════╝",
            "██║  ██║███████║█████╗  ██║  ██║███████║██║     ██║   ██║███████╗",
            "██║  ██║██╔══██║██╔══╝  ██║  ██║██╔══██║██║     ██║   ██║╚════██║",
            "██████╔╝██║  ██║███████╗██████╔╝██║  ██║███████╗╚██████╔╝███████║",
            "╚═════╝ ╚═╝  ╚═╝╚══════╝╚═════╝ ╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚══════╝"
    };

    /**
     * The mode selected by the user.
     */
    public enum Mode {
        /**
         * Generate Java models from proto schemas.
         */
        GENERATE,
        /**
         * Update existing Java models from changed proto schemas.
         */
        UPDATE
    }

    /**
     * A message indicating the user selected a mode.
     *
     * @param mode the selected mode
     */
    public record ModeSelectedMessage(Mode mode) implements Message {
    }

    private int cursor;
    private boolean confirmed;
    private boolean cancelled;

    /**
     * Constructs the welcome screen model.
     */
    public WelcomeModel() {
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
                    cursor = Math.min(1, cursor + 1);
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

        // Logo
        sb.append(renderLogo());
        sb.append("\n");

        // Tagline
        sb.append(Theme.MUTED_TEXT.render("Proto-to-Java code generation & update tool"));
        sb.append("\n");
        sb.append(Theme.DIM_TEXT.render("v4.0.0"));
        sb.append("\n\n");

        // Menu
        sb.append(Theme.SUBTITLE.render("What would you like to do?"));
        sb.append("\n\n");

        sb.append(renderMenuItem(0, "Generate", "Create Java models from .proto schemas"));
        sb.append(renderMenuItem(1, "Update", "Update existing Java models from changed schemas"));
        sb.append("\n");

        // Status bar
        var status = new StatusBar()
                .add("↑↓", "navigate")
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
            sb.append(Theme.SELECTED_CURSOR.render("  ▸ "));
            sb.append(Theme.SELECTED_ITEM.render(label));
            sb.append(Theme.MUTED_TEXT.render("  " + description));
        } else {
            sb.append(Theme.UNSELECTED_ITEM.render("    " + label));
            sb.append(Theme.DIM_TEXT.render("  " + description));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Returns the selected mode.
     *
     * @return the selected mode
     */
    public Mode selectedMode() {
        return cursor == 0 ? Mode.GENERATE : Mode.UPDATE;
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

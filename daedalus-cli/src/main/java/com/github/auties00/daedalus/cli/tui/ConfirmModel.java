package com.github.auties00.daedalus.cli.tui;

import com.williamcallahan.tui4j.compat.bubbletea.*;

/**
 * A simple yes/no confirmation prompt following the Bubble Tea architecture.
 *
 * <p>Displays a question with {@code [Y/n]} options and resolves to a
 * boolean result on confirmation.
 */
public final class ConfirmModel implements Model {
    private final String question;
    private boolean confirmed;
    private boolean resolved;

    /**
     * Constructs a confirmation prompt with the given question.
     *
     * @param question the question to display
     */
    public ConfirmModel(String question) {
        this.question = question;
        this.confirmed = true;
        this.resolved = false;
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
                case "y", "Y" -> {
                    confirmed = true;
                    resolved = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "n", "N" -> {
                    confirmed = false;
                    resolved = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "enter" -> {
                    resolved = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "ctrl+c", "esc" -> {
                    confirmed = false;
                    resolved = true;
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
        sb.append("\n");
        sb.append(Theme.BODY.render("  " + question + " "));
        if (confirmed) {
            sb.append(Theme.SELECTED_ITEM.render("[Y/n]"));
        } else {
            sb.append(Theme.SELECTED_ITEM.render("[y/N]"));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Returns whether the user confirmed.
     *
     * @return {@code true} if the user answered yes
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns whether the prompt has been resolved.
     *
     * @return {@code true} if the user has made a choice
     */
    public boolean isResolved() {
        return resolved;
    }
}

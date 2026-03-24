package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.tui.Theme;
import com.github.auties00.daedalus.protobuf.cli.tui.shared.StatusBar;

/**
 * A wizard step that collects a single text value from the user.
 *
 * <p>Displays a prompt with a default value and allows the user to type
 * a custom value or press enter to accept the default.
 */
public final class TextInputStep implements Model {
    private final String title;
    private final String prompt;
    private final String defaultValue;
    private final int stepNumber;
    private final int totalSteps;
    private final StringBuilder input;
    private boolean confirmed;
    private boolean cancelled;
    private int cursorPos;

    /**
     * Constructs a text input step.
     *
     * @param title the step title
     * @param prompt the input prompt
     * @param defaultValue the default value shown if the user presses enter without typing
     * @param stepNumber the 1-based step number
     * @param totalSteps the total number of steps
     */
    public TextInputStep(String title, String prompt, String defaultValue,
                         int stepNumber, int totalSteps) {
        this.title = title;
        this.prompt = prompt;
        this.defaultValue = defaultValue;
        this.stepNumber = stepNumber;
        this.totalSteps = totalSteps;
        this.input = new StringBuilder();
        this.confirmed = false;
        this.cancelled = false;
        this.cursorPos = 0;
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
                case "enter" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "esc", "ctrl+c" -> {
                    cancelled = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "backspace" -> {
                    if (cursorPos > 0) {
                        input.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                    yield UpdateResult.from(this);
                }
                case "left" -> {
                    cursorPos = Math.max(0, cursorPos - 1);
                    yield UpdateResult.from(this);
                }
                case "right" -> {
                    cursorPos = Math.min(input.length(), cursorPos + 1);
                    yield UpdateResult.from(this);
                }
                case "ctrl+a", "home" -> {
                    cursorPos = 0;
                    yield UpdateResult.from(this);
                }
                case "ctrl+e", "end" -> {
                    cursorPos = input.length();
                    yield UpdateResult.from(this);
                }
                case "ctrl+u" -> {
                    input.setLength(0);
                    cursorPos = 0;
                    yield UpdateResult.from(this);
                }
                default -> {
                    if (keyStr.length() == 1 && !keyStr.startsWith("ctrl+")) {
                        input.insert(cursorPos, keyStr);
                        cursorPos++;
                    }
                    yield UpdateResult.from(this);
                }
            };
        }
        return UpdateResult.from(this);
    }

    @Override
    public String view() {
        var sb = new StringBuilder();

        // Header
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Generator"));
        sb.append("  ");
        sb.append(Theme.stepIndicator(stepNumber, totalSteps));
        sb.append("\n\n");

        // Prompt
        sb.append("  ");
        sb.append(Theme.BODY.render(prompt));
        sb.append("\n\n");

        // Input field
        sb.append("  ");
        sb.append(Theme.SUBTITLE.render("▸ "));
        if (input.isEmpty()) {
            sb.append(Theme.MUTED_TEXT.render(defaultValue));
            sb.append(Theme.BODY.render("█"));
        } else {
            var text = input.toString();
            if (cursorPos < text.length()) {
                sb.append(Theme.BODY.render(text.substring(0, cursorPos)));
                sb.append(Theme.BODY.render("█"));
                sb.append(Theme.BODY.render(text.substring(cursorPos + 1)));
            } else {
                sb.append(Theme.BODY.render(text));
                sb.append(Theme.BODY.render("█"));
            }
        }
        sb.append("\n");

        if (!defaultValue.isEmpty()) {
            sb.append("  ");
            sb.append(Theme.DIM_TEXT.render("  default: " + defaultValue));
            sb.append("\n");
        }

        sb.append("\n");

        // Status bar
        var status = new StatusBar()
                .add("enter", "confirm")
                .add("esc", "cancel");
        sb.append("  ");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Returns the entered value, or the default value if nothing was typed.
     *
     * @return the resolved text value
     */
    public String value() {
        return input.isEmpty() ? defaultValue : input.toString();
    }

    /**
     * Returns whether the step was confirmed.
     *
     * @return {@code true} if the user pressed enter
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns whether the step was cancelled.
     *
     * @return {@code true} if the user pressed escape
     */
    public boolean isCancelled() {
        return cancelled;
    }
}

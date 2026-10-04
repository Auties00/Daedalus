package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.cli.tui.Screen;
import com.github.auties00.daedalus.cli.tui.StatusBar;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.cli.tui.Theme;

/**
 * A wizard step that collects a single text value from the user.
 *
 * <p>Displays a prompt with a default value and allows the user to type
 * a custom value or press enter to accept the default.
 */
public final class TextInputStep implements Model {
    private final String title;
    private final String prompt;
    private final String hint;
    private final String defaultValue;
    private final int stepNumber;
    private final int totalSteps;
    private final StringBuilder input;
    private boolean confirmed;
    private boolean cancelled;
    private int cursorPos;
    private String error;
    private int termHeight;

    /**
     * Constructs a text input step.
     *
     * @param title the step title
     * @param prompt the input prompt
     * @param hint a line of help shown below the input field, or empty if there is none
     * @param defaultValue the default value shown if the user presses enter without typing
     * @param stepNumber the 1-based step number
     * @param totalSteps the total number of steps
     */
    public TextInputStep(String title, String prompt, String hint, String defaultValue,
                         int stepNumber, int totalSteps) {
        this.title = title;
        this.prompt = prompt;
        this.hint = hint;
        this.defaultValue = defaultValue;
        this.stepNumber = stepNumber;
        this.totalSteps = totalSteps;
        this.input = new StringBuilder();
        this.confirmed = false;
        this.cancelled = false;
        this.cursorPos = 0;
        this.termHeight = 40;
    }

    @Override
    public Command init() {
        return Command.none();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage size) {
            termHeight = size.height();
            return UpdateResult.from(this);
        }
        if (msg instanceof KeyPressMessage key) {
            var keyStr = key.key();
            error = null;
            return switch (keyStr) {
                case "enter" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "esc" -> UpdateResult.from(this, Command.quit());
                case "ctrl+c" -> {
                    cancelled = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "backspace", "ctrl+h" -> {
                    if (cursorPos > 0) {
                        input.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                    yield UpdateResult.from(this);
                }
                case "delete" -> {
                    if (cursorPos < input.length()) {
                        input.deleteCharAt(cursorPos);
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
        var field = new StringBuilder();
        field.append(Theme.SUBTITLE.render("▸ "));
        if (input.isEmpty()) {
            field.append(Theme.MUTED_TEXT.render(defaultValue));
            field.append(Theme.BODY.render("█"));
        } else {
            var text = input.toString();
            if (cursorPos < text.length()) {
                field.append(Theme.BODY.render(text.substring(0, cursorPos)));
                field.append(Theme.BODY.render("█"));
                field.append(Theme.BODY.render(text.substring(cursorPos + 1)));
            } else {
                field.append(Theme.BODY.render(text));
                field.append(Theme.BODY.render("█"));
            }
        }

        var screen = Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Step " + stepNumber + "/" + totalSteps)
                .heading(prompt)
                .block(field.toString());

        if (!hint.isEmpty()) {
            screen.block(Theme.DIM_TEXT.render("  " + hint));
        }

        if (error != null) {
            screen.block(Theme.DANGER_TEXT.render("✗ " + error));
        }

        if (!defaultValue.isEmpty()) {
            screen.block(Theme.DIM_TEXT.render("  default: " + defaultValue));
        }

        return screen
                .keys(new StatusBar()
                        .add("enter", "confirm")
                        .add("esc", "back"))
                .render();
    }

    /**
     * Rejects the confirmed value, which keeps the step open so that the user can correct
     * the text, and shows the reason below the input field until the next key press.
     *
     * @param message the reason why the value was rejected
     */
    public void reject(String message) {
        confirmed = false;
        error = message;
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

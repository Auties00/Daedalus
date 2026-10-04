package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.cli.tui.ModifierKeys;
import com.github.auties00.daedalus.cli.tui.Screen;
import com.github.auties00.daedalus.cli.tui.StatusBar;
import com.github.auties00.daedalus.cli.tui.Theme;
import com.williamcallahan.tui4j.compat.bubbletea.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A wizard step that collects a list of values, each one in its own input field.
 *
 * <p>The fields are edited like the field of a {@link TextInputStep}. Shift and enter, or
 * control and N, add a field below the focused one, the arrow keys and tab move between the
 * fields, and backspace in an empty field removes it. Enter confirms the step.
 *
 * <p>Shift and enter can only be told apart from enter on a platform where
 * {@link ModifierKeys} can read the keyboard, so control and N works everywhere.
 */
public final class SourcesInputStep implements Model {
    private final String prompt;
    private final String hint;
    private final int stepNumber;
    private final int totalSteps;
    private final List<StringBuilder> inputs;
    private int focus;
    private int cursorPos;
    private boolean confirmed;
    private boolean cancelled;
    private String error;
    private int termHeight;

    /**
     * Constructs a step that collects a list of values.
     *
     * @param prompt the question that the values answer
     * @param hint a line of help shown below the fields, or empty if there is none
     * @param initial the values to start with, which may be empty
     * @param stepNumber the 1-based step number
     * @param totalSteps the total number of steps
     */
    public SourcesInputStep(String prompt, String hint, List<String> initial, int stepNumber, int totalSteps) {
        this.prompt = prompt;
        this.hint = hint;
        this.stepNumber = stepNumber;
        this.totalSteps = totalSteps;
        this.inputs = new ArrayList<>();
        for (var value : initial) {
            inputs.add(new StringBuilder(value));
        }
        if (inputs.isEmpty()) {
            inputs.add(new StringBuilder());
        }
        this.focus = inputs.size() - 1;
        this.cursorPos = inputs.get(focus).length();
        this.confirmed = false;
        this.cancelled = false;
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

        if (!(msg instanceof KeyPressMessage key)) {
            return UpdateResult.from(this);
        }

        var keyStr = key.key();
        error = null;
        return switch (keyStr) {
            case "enter" -> {
                if (ModifierKeys.isShiftPressed()) {
                    addField();
                    yield UpdateResult.from(this);
                }

                confirmed = true;
                yield UpdateResult.from(this, Command.quit());
            }
            case "shift+enter", "ctrl+n", "ctrl+j" -> {
                addField();
                yield UpdateResult.from(this);
            }
            case "esc" -> UpdateResult.from(this, Command.quit());
            case "ctrl+c" -> {
                cancelled = true;
                yield UpdateResult.from(this, Command.quit());
            }
            case "up", "shift+tab" -> {
                moveFocus(focus - 1);
                yield UpdateResult.from(this);
            }
            case "down", "tab" -> {
                moveFocus(focus + 1);
                yield UpdateResult.from(this);
            }
            case "backspace", "ctrl+h" -> {
                deleteBackward();
                yield UpdateResult.from(this);
            }
            case "delete" -> {
                var input = inputs.get(focus);
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
                cursorPos = Math.min(inputs.get(focus).length(), cursorPos + 1);
                yield UpdateResult.from(this);
            }
            case "ctrl+a", "home" -> {
                cursorPos = 0;
                yield UpdateResult.from(this);
            }
            case "ctrl+e", "end" -> {
                cursorPos = inputs.get(focus).length();
                yield UpdateResult.from(this);
            }
            case "ctrl+u" -> {
                inputs.get(focus).setLength(0);
                cursorPos = 0;
                yield UpdateResult.from(this);
            }
            default -> {
                if (keyStr.length() == 1 && !keyStr.startsWith("ctrl+")) {
                    inputs.get(focus).insert(cursorPos, keyStr);
                    cursorPos++;
                }
                yield UpdateResult.from(this);
            }
        };
    }

    private void addField() {
        focus++;
        inputs.add(focus, new StringBuilder());
        cursorPos = 0;
    }

    private void moveFocus(int index) {
        focus = Math.max(0, Math.min(inputs.size() - 1, index));
        cursorPos = inputs.get(focus).length();
    }

    private void deleteBackward() {
        var input = inputs.get(focus);
        if (cursorPos > 0) {
            input.deleteCharAt(cursorPos - 1);
            cursorPos--;
            return;
        }

        if (input.isEmpty() && inputs.size() > 1) {
            inputs.remove(focus);
            moveFocus(focus - 1);
        }
    }

    @Override
    public String view() {
        var fields = new StringBuilder();
        for (var i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                fields.append('\n');
            }
            fields.append(renderField(i));
        }

        var screen = Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Step " + stepNumber + "/" + totalSteps)
                .heading(prompt)
                .block(fields.toString());

        if (!hint.isEmpty()) {
            screen.block(Theme.DIM_TEXT.render("  " + hint));
        }

        if (error != null) {
            screen.block(Theme.DANGER_TEXT.render("✗ " + error));
        }

        var add = ModifierKeys.isSupported() ? "shift+enter/ctrl+n" : "ctrl+n";
        return screen
                .keys(new StatusBar()
                        .add("enter", "confirm")
                        .add(add, "add file")
                        .add("↑↓", "move")
                        .add("esc", "back"))
                .render();
    }

    private String renderField(int index) {
        var text = inputs.get(index).toString();
        if (index != focus) {
            return Theme.DIM_TEXT.render("  ") + Theme.BODY.render(text);
        }

        return Theme.SUBTITLE.render("▸ ")
                + Theme.BODY.render(text.substring(0, cursorPos))
                + Theme.BODY.render("█")
                + Theme.BODY.render(text.substring(Math.min(text.length(), cursorPos + 1)));
    }

    /**
     * Rejects the confirmed values, which keeps the step open so that the user can correct
     * them, and shows the reason below the fields until the next key press.
     *
     * @param message the reason why the values were rejected
     */
    public void reject(String message) {
        confirmed = false;
        error = message;
    }

    /**
     * Returns the entered values.
     *
     * @return the values of the fields that are not blank, trimmed and in the order of the fields
     */
    public List<String> values() {
        return inputs.stream()
                .map(input -> input.toString().strip())
                .filter(value -> !value.isEmpty())
                .toList();
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
     * @return {@code true} if the user pressed control and C
     */
    public boolean isCancelled() {
        return cancelled;
    }
}

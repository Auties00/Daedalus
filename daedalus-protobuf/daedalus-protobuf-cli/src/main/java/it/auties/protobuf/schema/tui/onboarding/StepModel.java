package it.auties.protobuf.schema.tui.onboarding;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Join;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import it.auties.protobuf.schema.tui.Theme;
import it.auties.protobuf.schema.tui.shared.StatusBar;

import java.util.List;
import java.util.function.Function;

/**
 * A single wizard step presenting a list of options with a live code preview.
 *
 * <p>The user navigates options with arrow keys. As the cursor moves,
 * the preview panel on the right updates to show how generated code
 * will look with the currently highlighted option.
 *
 * @param <T> the type of option values (e.g., {@code TypeForm}, {@code EnumForm})
 */
public final class StepModel<T> implements Model {
    private final String title;
    private final String question;
    private final List<Option<T>> options;
    private final Function<T, String> previewFn;
    private final int stepNumber;
    private final int totalSteps;
    private final int maxPreviewHeight;
    private int cursor;
    private boolean confirmed;
    private boolean cancelled;

    /**
     * An option in the selection list.
     *
     * @param <T> the type of the option value
     * @param label the display label
     * @param description a short description
     * @param value the underlying value
     */
    public record Option<T>(String label, String description, T value) {
    }

    /**
     * A message indicating this step has been confirmed with a selected value.
     *
     * @param <T> the type of the selected value
     * @param value the selected value
     */
    public record StepConfirmedMessage<T>(T value) implements Message {
    }

    /**
     * A message indicating the user wants to go back to the previous step.
     */
    public record StepBackMessage() implements Message {
    }

    /**
     * A message indicating the user cancelled the wizard.
     */
    public record StepCancelledMessage() implements Message {
    }

    /**
     * Constructs a wizard step.
     *
     * @param title the step title
     * @param question the question to display
     * @param options the available options
     * @param previewFn a function that generates a code preview for a given option value
     * @param stepNumber the 1-based step number
     * @param totalSteps the total number of steps
     */
    public StepModel(String title, String question, List<Option<T>> options,
                     Function<T, String> previewFn, int stepNumber, int totalSteps) {
        this.title = title;
        this.question = question;
        this.options = options;
        this.previewFn = previewFn;
        this.stepNumber = stepNumber;
        this.totalSteps = totalSteps;
        this.cursor = 0;
        this.confirmed = false;
        this.cancelled = false;
        this.maxPreviewHeight = options.stream()
                .mapToInt(opt -> previewFn.apply(opt.value()).split("\n", -1).length)
                .max()
                .orElse(0);
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
                    cursor = Math.min(options.size() - 1, cursor + 1);
                    yield UpdateResult.from(this);
                }
                case "enter" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "left", "backspace" -> {
                    yield UpdateResult.from(this, Command.quit());
                }
                case "esc", "ctrl+c" -> {
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

        // Header
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Generator"));
        sb.append("  ");
        sb.append(Theme.stepIndicator(stepNumber, totalSteps));
        sb.append("\n\n");

        // Question
        sb.append("  ");
        sb.append(Theme.BODY.render(question));
        sb.append("\n\n");

        // Options list
        var optionsBlock = renderOptions();

        // Preview panel
        var previewBlock = renderPreview();

        // Join horizontally
        sb.append(Join.joinHorizontal(Position.Top, optionsBlock, "    ", previewBlock));
        sb.append("\n\n");

        // Status bar
        var status = new StatusBar()
                .add("↑↓", "navigate")
                .add("enter", "confirm")
                .add("←", "back")
                .add("esc", "cancel");
        sb.append("  ");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    private String renderOptions() {
        var sb = new StringBuilder();
        for (var i = 0; i < options.size(); i++) {
            var option = options.get(i);
            sb.append("  ");
            if (i == cursor) {
                sb.append(Theme.SELECTED_CURSOR.render("● "));
                sb.append(Theme.SELECTED_ITEM.render(option.label()));
                if (!option.description().isEmpty()) {
                    sb.append(Theme.MUTED_TEXT.render("  " + option.description()));
                }
            } else {
                sb.append(Theme.UNSELECTED_ITEM.render("○ " + option.label()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String renderPreview() {
        var selected = options.get(cursor).value();
        var preview = previewFn.apply(selected);
        var previewLines = preview.split("\n", -1).length;

        var sb = new StringBuilder();
        sb.append(Theme.DIM_TEXT.render("─── Preview "));
        sb.append(Theme.DIM_TEXT.render("─".repeat(40)));
        sb.append("\n\n");
        sb.append(preview);
        sb.append("\n");
        for (var i = previewLines; i < maxPreviewHeight; i++) {
            sb.append("\n");
        }

        return Theme.PREVIEW_PANEL.render(sb.toString());
    }

    /**
     * Returns the selected option value.
     *
     * @return the selected value
     */
    public T selectedValue() {
        return options.get(cursor).value();
    }

    /**
     * Returns whether the step was confirmed.
     *
     * @return {@code true} if the user pressed enter to confirm
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns whether the wizard was cancelled.
     *
     * @return {@code true} if the user pressed escape
     */
    public boolean isCancelled() {
        return cancelled;
    }
}

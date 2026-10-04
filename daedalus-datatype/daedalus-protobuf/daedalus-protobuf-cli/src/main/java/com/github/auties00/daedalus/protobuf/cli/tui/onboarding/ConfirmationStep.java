package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.cli.tui.Screen;
import com.github.auties00.daedalus.cli.tui.StatusBar;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;
import com.github.auties00.daedalus.cli.tui.Theme;

import java.util.List;

/**
 * The final onboarding step showing a configuration summary for confirmation.
 *
 * <p>Displays all selected values in a readable format and prompts the user
 * to confirm or go back and change settings.
 */
public final class ConfirmationStep implements Model {
    private static final int PANEL_CHROME_ROWS = 4;
    private static final int SUMMARY_ROWS = 14;

    private final List<String> protoFiles;
    private final String javaSourceDir;
    private final TypeForm typeForm;
    private final EnumForm enumForm;
    private final boolean includeUnknownFields;
    private final int totalSteps;
    private boolean confirmed;
    private boolean cancelled;
    private int termHeight;

    /**
     * Constructs the confirmation step with all collected values.
     *
     * @param protoFiles the proto sources
     * @param javaSourceDir the Java output directory
     * @param typeForm the selected message type form
     * @param enumForm the selected enum form
     * @param includeUnknownFields whether to include unknown fields
     * @param totalSteps the total number of wizard steps
     */
    public ConfirmationStep(List<String> protoFiles, String javaSourceDir,
                             TypeForm typeForm, EnumForm enumForm,
                             boolean includeUnknownFields,
                             int totalSteps) {
        this.protoFiles = protoFiles;
        this.javaSourceDir = javaSourceDir;
        this.typeForm = typeForm;
        this.enumForm = enumForm;
        this.includeUnknownFields = includeUnknownFields;
        this.totalSteps = totalSteps;
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
        if (msg instanceof KeyPressMessage key) {
            var keyStr = key.key();
            return switch (keyStr) {
                case "enter", "y", "Y" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "left", "backspace", "ctrl+h", "esc" -> {
                    yield UpdateResult.from(this, Command.quit());
                }
                case "ctrl+c", "n", "N" -> {
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
        var screen = Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Step " + totalSteps + "/" + totalSteps)
                .heading("Configuration Summary")
                .entry("Proto files", String.join(", ", protoFiles))
                .entry("Java output directory", javaSourceDir)
                .entry("Message type form", formatTypeForm(typeForm))
                .entry("Enum form", formatEnumForm(enumForm))
                .entry("Unknown fields", includeUnknownFields ? "Yes" : "No")
                .blank();

        var yaml = renderYamlPreview().stripTrailing();
        if (yamlFits(yaml)) {
            screen.block(Screen.panel(".protobuf-schema.yml", yaml)).blank();
        }

        return screen
                .block(Theme.BODY.render("Write configuration and start generating? ")
                        + Theme.SELECTED_ITEM.render("[Y/n]"))
                .keys(new StatusBar()
                        .add("enter", "confirm")
                        .add("←/esc", "go back")
                        .add("n", "cancel"))
                .render();
    }

    private boolean yamlFits(String yaml) {
        var rows = yaml.split("\n", -1).length + PANEL_CHROME_ROWS;
        return termHeight - Screen.headerRows(termHeight) - SUMMARY_ROWS >= rows;
    }

    private String protoYaml() {
        var yaml = new StringBuilder("protoFiles:");
        for (var source : protoFiles) {
            yaml.append("\n  - ").append(quoteIfNeeded(source));
        }
        return yaml.toString();
    }

    private String quoteIfNeeded(String value) {
        var indicators = "*&!|>%@`'{[]},#?:";
        var needsQuotes = indicators.indexOf(value.charAt(0)) >= 0
                || value.contains(": ")
                || value.contains(" #");
        return needsQuotes ? "'" + value.replace("'", "''") + "'" : value;
    }

    private String renderYamlPreview() {
        var yaml = """
                  %s
                  javaSourceDir: %s
                  defaults:
                    typeForm: %s
                    enumForm: %s
                    includeUnknownFields: %s
                  """.formatted(
                protoYaml(), javaSourceDir,
                typeForm.name(), enumForm.name(),
                includeUnknownFields
        );

        var sb = new StringBuilder();
        for (var line : yaml.split("\n")) {
            var trimmed = line.stripLeading();
            var indent = line.substring(0, line.length() - trimmed.length());
            if (trimmed.contains(":")) {
                var parts = trimmed.split(":", 2);
                sb.append(Theme.CODE_PLAIN.render(indent));
                sb.append(Theme.CODE_KEYWORD.render(parts[0]));
                sb.append(Theme.CODE_PLAIN.render(":"));
                if (parts.length > 1) {
                    sb.append(Theme.CODE_STRING.render(parts[1]));
                }
            } else {
                sb.append(Theme.CODE_PLAIN.render(line));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatTypeForm(TypeForm form) {
        return switch (form) {
            case RECORD -> "Record";
            case CLASS -> "Class";
            case INTERFACE -> "Interface";
        };
    }

    private String formatEnumForm(EnumForm form) {
        return switch (form) {
            case JAVA_ENUM -> "Java Enum";
            case CLASS -> "Class";
            case RECORD -> "Record";
        };
    }

    /**
     * Returns whether the configuration was confirmed.
     *
     * @return {@code true} if the user confirmed
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns whether the wizard was cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
}

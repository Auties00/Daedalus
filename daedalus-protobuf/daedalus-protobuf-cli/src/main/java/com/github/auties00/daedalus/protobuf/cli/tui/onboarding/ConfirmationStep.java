package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.model.EnumConstantMode;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;
import com.github.auties00.daedalus.protobuf.cli.tui.Theme;
import com.github.auties00.daedalus.protobuf.cli.tui.shared.StatusBar;

/**
 * The final onboarding step showing a configuration summary for confirmation.
 *
 * <p>Displays all selected values in a readable format and prompts the user
 * to confirm or go back and change settings.
 */
public final class ConfirmationStep implements Model {
    private final String protoDir;
    private final String javaSourceDir;
    private final TypeForm typeForm;
    private final EnumForm enumForm;
    private final EnumConstantMode enumConstantMode;
    private final boolean generateBuilders;
    private final boolean includeUnknownFields;
    private final int totalSteps;
    private boolean confirmed;
    private boolean cancelled;

    /**
     * Constructs the confirmation step with all collected values.
     *
     * @param protoDir the proto source directory
     * @param javaSourceDir the Java output directory
     * @param typeForm the selected message type form
     * @param enumForm the selected enum form
     * @param enumConstantMode the selected enum constant mode
     * @param generateBuilders whether to generate builders
     * @param includeUnknownFields whether to include unknown fields
     * @param totalSteps the total number of wizard steps
     */
    public ConfirmationStep(String protoDir, String javaSourceDir,
                             TypeForm typeForm, EnumForm enumForm,
                             EnumConstantMode enumConstantMode,
                             boolean generateBuilders, boolean includeUnknownFields,
                             int totalSteps) {
        this.protoDir = protoDir;
        this.javaSourceDir = javaSourceDir;
        this.typeForm = typeForm;
        this.enumForm = enumForm;
        this.enumConstantMode = enumConstantMode;
        this.generateBuilders = generateBuilders;
        this.includeUnknownFields = includeUnknownFields;
        this.totalSteps = totalSteps;
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
                case "enter", "y", "Y" -> {
                    confirmed = true;
                    yield UpdateResult.from(this, Command.quit());
                }
                case "left", "backspace" -> {
                    yield UpdateResult.from(this, Command.quit());
                }
                case "esc", "ctrl+c", "n", "N" -> {
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
        sb.append(Theme.stepIndicator(totalSteps, totalSteps));
        sb.append("\n\n");

        // Summary title
        sb.append("  ");
        sb.append(Theme.SUBTITLE.render("Configuration Summary"));
        sb.append("\n\n");

        // Config entries
        sb.append(configEntry("Proto directory", protoDir));
        sb.append(configEntry("Java output directory", javaSourceDir));
        sb.append(configEntry("Message type form", formatTypeForm(typeForm)));
        sb.append(configEntry("Enum form", formatEnumForm(enumForm)));
        sb.append(configEntry("Enum constant mode", formatConstantMode(enumConstantMode)));
        sb.append(configEntry("Generate builders", generateBuilders ? "Yes" : "No"));
        sb.append(configEntry("Unknown fields", includeUnknownFields ? "Yes" : "No"));
        sb.append("\n");

        // YAML preview
        sb.append("  ");
        sb.append(Theme.DIM_TEXT.render("─── .protobuf-schema.yml "));
        sb.append(Theme.DIM_TEXT.render("─".repeat(30)));
        sb.append("\n\n");
        sb.append(renderYamlPreview());
        sb.append("\n");

        // Prompt
        sb.append("  ");
        sb.append(Theme.BODY.render("Write configuration and start generating? "));
        sb.append(Theme.SELECTED_ITEM.render("[Y/n]"));
        sb.append("\n\n");

        // Status bar
        var status = new StatusBar()
                .add("enter", "confirm")
                .add("←", "go back")
                .add("esc", "cancel");
        sb.append("  ");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    private String configEntry(String label, String value) {
        return "  " + Theme.MUTED_TEXT.render(label + ": ")
                + Theme.BODY.render(value) + "\n";
    }

    private String renderYamlPreview() {
        var yaml = """
                  protoDir: %s
                  javaSourceDir: %s
                  defaults:
                    typeForm: %s
                    enumForm: %s
                    enumConstantMode: %s
                    generateBuilders: %s
                    includeUnknownFields: %s
                """.formatted(
                protoDir, javaSourceDir,
                typeForm.name(), enumForm.name(),
                enumConstantMode.name(),
                generateBuilders, includeUnknownFields
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
            case SEALED_CLASS -> "Sealed Class";
            case SEALED_INTERFACE -> "Sealed Interface";
        };
    }

    private String formatEnumForm(EnumForm form) {
        return switch (form) {
            case JAVA_ENUM -> "Java Enum";
            case CLASS -> "Class";
            case RECORD -> "Record";
            case SEALED_INTERFACE -> "Sealed Interface";
            case SEALED_ABSTRACT_CLASS -> "Sealed Abstract Class";
        };
    }

    private String formatConstantMode(EnumConstantMode mode) {
        return switch (mode) {
            case CONSTANT_ANNOTATION -> "@ProtobufEnum.Constant";
            case SERIALIZER_DESERIALIZER -> "Serializer / Deserializer";
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

package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;
import com.github.auties00.daedalus.protobuf.cli.tui.onboarding.StepModel.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the multi-step onboarding wizard for configuring schema generation.
 *
 * <p>Presents a sequence of interactive steps where the user selects options
 * with live code previews, producing a complete {@link SchemaConfig} at the end.
 *
 * <p>Steps:
 * <ol>
 *   <li>Proto files, directories and patterns (text input)
 *   <li>Java output directory (text input)
 *   <li>Message type form (selection with preview)
 *   <li>Enum form (selection with preview)
 *   <li>Include unknown fields? (yes/no with preview)
 *   <li>Confirmation (summary)
 * </ol>
 *
 * <p>The two directories are validated as soon as they are confirmed, and the step stays
 * open with the reason below the input field until the user enters a valid path.
 */
public final class OnboardingModel implements Model {
    private static final int TOTAL_STEPS = 6;

    private final Path workingDir;
    private int currentStep;
    private Model activeModel;
    private boolean cancelled;
    private boolean completed;
    private WindowSizeMessage lastSize;

    // Collected values
    private List<String> protoFiles;
    private String javaSourceDir;
    private TypeForm typeForm;
    private EnumForm enumForm;
    private boolean includeUnknownFields;

    /**
     * Constructs a new onboarding wizard starting at step 1.
     *
     * @param workingDir the directory that relative paths entered by the user are resolved against
     */
    public OnboardingModel(Path workingDir) {
        this.workingDir = workingDir;
        this.currentStep = 1;
        this.cancelled = false;
        this.completed = false;
        this.protoFiles = List.of();
        this.javaSourceDir = "./src/main/java";
        this.typeForm = TypeForm.RECORD;
        this.enumForm = EnumForm.JAVA_ENUM;
        this.includeUnknownFields = false;
        this.activeModel = createStep(1);
    }

    @Override
    public Command init() {
        return activeModel.init();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage size) {
            lastSize = size;
        }

        var result = activeModel.update(msg);
        activeModel = result.model();

        if (result.command() != null && isQuitCommand(result)) {
            return handleStepCompletion();
        }

        return UpdateResult.from(this);
    }

    @Override
    public String view() {
        return activeModel.view();
    }

    private UpdateResult<? extends Model> handleStepCompletion() {
        // Check for cancellation
        if (isStepCancelled()) {
            cancelled = true;
            return UpdateResult.from(this, Command.quit());
        }

        // Collect the value from the completed step
        if (isStepConfirmed()) {
            var error = validateStep();
            if (error.isPresent()) {
                reject(error.get());
                return UpdateResult.from(this);
            }

            collectStepValue();

            if (currentStep < TOTAL_STEPS) {
                currentStep++;
                activeModel = enter(createStep(currentStep));
                return UpdateResult.from(this, activeModel.init());
            } else {
                completed = true;
                return UpdateResult.from(this, Command.quit());
            }
        }

        // User pressed back
        if (currentStep > 1) {
            currentStep--;
            activeModel = enter(createStep(currentStep));
            return UpdateResult.from(this, activeModel.init());
        }

        cancelled = true;
        return UpdateResult.from(this, Command.quit());
    }

    private Model enter(Model step) {
        if (lastSize == null) {
            return step;
        }

        return step.update(lastSize).model();
    }

    private Optional<String> validateStep() {
        if (activeModel instanceof SourcesInputStep step) {
            return PathValidator.protoSources(workingDir, step.values());
        }

        if (activeModel instanceof TextInputStep step && currentStep == 2) {
            return PathValidator.outputDirectory(workingDir, step.value());
        }

        return Optional.empty();
    }

    private void reject(String message) {
        if (activeModel instanceof SourcesInputStep step) {
            step.reject(message);
        } else if (activeModel instanceof TextInputStep step) {
            step.reject(message);
        }
    }

    private boolean isQuitCommand(UpdateResult<?> result) {
        return result.command() != null;
    }

    private boolean isStepCancelled() {
        if (activeModel instanceof SourcesInputStep step) {
            return step.isCancelled();
        }
        if (activeModel instanceof TextInputStep step) {
            return step.isCancelled();
        }
        if (activeModel instanceof StepModel<?> step) {
            return step.isCancelled();
        }
        if (activeModel instanceof ConfirmationStep step) {
            return step.isCancelled();
        }
        return false;
    }

    private boolean isStepConfirmed() {
        if (activeModel instanceof SourcesInputStep step) {
            return step.isConfirmed();
        }
        if (activeModel instanceof TextInputStep step) {
            return step.isConfirmed();
        }
        if (activeModel instanceof StepModel<?> step) {
            return step.isConfirmed();
        }
        if (activeModel instanceof ConfirmationStep step) {
            return step.isConfirmed();
        }
        return false;
    }

    private void collectStepValue() {
        switch (currentStep) {
            case 1 -> {
                if (activeModel instanceof SourcesInputStep step) {
                    protoFiles = step.values();
                }
            }
            case 2 -> {
                if (activeModel instanceof TextInputStep step) {
                    javaSourceDir = step.value();
                }
            }
            case 3 -> {
                if (activeModel instanceof StepModel<?> step) {
                    typeForm = (TypeForm) step.selectedValue();
                }
            }
            case 4 -> {
                if (activeModel instanceof StepModel<?> step) {
                    enumForm = (EnumForm) step.selectedValue();
                }
            }
            case 5 -> {
                if (activeModel instanceof StepModel<?> step) {
                    includeUnknownFields = (Boolean) step.selectedValue();
                }
            }
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Model createStep(int step) {
        return switch (step) {
            case 1 -> new SourcesInputStep(
                    "Which .proto files should be used?",
                    "Files, directories or patterns such as src/**/*.proto",
                    protoFiles, 1, TOTAL_STEPS);
            case 2 -> new TextInputStep(
                    "Java Output Directory",
                    "Where should generated Java files be written?",
                    "",
                    javaSourceDir, 2, TOTAL_STEPS);
            case 3 -> new StepModel<>(
                    "Message Type Form",
                    "How should proto messages be represented in Java?",
                    List.of(
                            new Option<>("Record", "Annotated record components", TypeForm.RECORD),
                            new Option<>("Class", "Private fields + constructor + accessors", TypeForm.CLASS),
                            new Option<>("Interface", "Abstract getter methods", TypeForm.INTERFACE)
                    ),
                    CodePreview::forTypeForm, 3, TOTAL_STEPS);
            case 4 -> new StepModel<>(
                    "Enum Form",
                    "How should proto enums be represented in Java?",
                    List.of(
                            new Option<>("Java Enum", "Standard Java enum type", EnumForm.JAVA_ENUM),
                            new Option<>("Class", "Class with static constant fields", EnumForm.CLASS),
                            new Option<>("Record", "Record with static constant fields", EnumForm.RECORD)
                    ),
                    CodePreview::forEnumForm, 4, TOTAL_STEPS);
            case 5 -> new StepModel<>(
                    "Unknown Fields",
                    "Include @ProtobufUnknownFields support?",
                    List.of(
                            new Option<>("No", "Unknown fields will be discarded", false),
                            new Option<>("Yes", "Store unknown fields for round-tripping", true)
                    ),
                    CodePreview::forUnknownFields, 5, TOTAL_STEPS);
            case 6 -> new ConfirmationStep(
                    protoFiles, javaSourceDir, typeForm, enumForm,
                    includeUnknownFields,
                    TOTAL_STEPS);
            default -> throw new IllegalStateException("Invalid step: " + step);
        };
    }

    /**
     * Returns whether the wizard was cancelled by the user.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns whether the wizard completed successfully.
     *
     * @return {@code true} if all steps were confirmed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Builds a {@link SchemaConfig} from the collected wizard values.
     *
     * @return the configured schema config
     */
    public SchemaConfig buildConfig() {
        var config = new SchemaConfig();
        config.setProtoFiles(protoFiles);
        config.setJavaSourceDir(javaSourceDir);
        config.defaults().setTypeForm(typeForm);
        config.defaults().setEnumForm(enumForm);
        config.defaults().setIncludeUnknownFields(includeUnknownFields);
        return config;
    }
}

package it.auties.protobuf.schema.tui.onboarding;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import it.auties.protobuf.schema.config.SchemaConfig;
import it.auties.protobuf.schema.model.EnumConstantMode;
import it.auties.protobuf.schema.model.EnumForm;
import it.auties.protobuf.schema.model.TypeForm;
import it.auties.protobuf.schema.tui.Theme;
import it.auties.protobuf.schema.tui.onboarding.StepModel.Option;
import it.auties.protobuf.schema.tui.shared.StatusBar;

import java.util.List;

/**
 * Orchestrates the multi-step onboarding wizard for configuring schema generation.
 *
 * <p>Presents a sequence of interactive steps where the user selects options
 * with live code previews, producing a complete {@link SchemaConfig} at the end.
 *
 * <p>Steps:
 * <ol>
 *   <li>Proto source directory (text input)
 *   <li>Java output directory (text input)
 *   <li>Message type form (selection with preview)
 *   <li>Enum form (selection with preview)
 *   <li>Enum constant mode (selection with preview)
 *   <li>Generate builders? (yes/no with preview)
 *   <li>Include unknown fields? (yes/no with preview)
 *   <li>Confirmation (summary)
 * </ol>
 */
public final class OnboardingModel implements Model {
    private static final int TOTAL_STEPS = 8;

    private int currentStep;
    private Model activeModel;
    private boolean cancelled;
    private boolean completed;

    // Collected values
    private String protoDir;
    private String javaSourceDir;
    private TypeForm typeForm;
    private EnumForm enumForm;
    private EnumConstantMode enumConstantMode;
    private boolean generateBuilders;
    private boolean includeUnknownFields;

    /**
     * Constructs a new onboarding wizard starting at step 1.
     */
    public OnboardingModel() {
        this.currentStep = 1;
        this.cancelled = false;
        this.completed = false;
        this.protoDir = "./proto";
        this.javaSourceDir = "./src/main/java";
        this.typeForm = TypeForm.RECORD;
        this.enumForm = EnumForm.JAVA_ENUM;
        this.enumConstantMode = EnumConstantMode.CONSTANT_ANNOTATION;
        this.generateBuilders = true;
        this.includeUnknownFields = false;
        this.activeModel = createStep(1);
    }

    @Override
    public Command init() {
        return activeModel.init();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
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
            collectStepValue();

            if (currentStep < TOTAL_STEPS) {
                currentStep++;
                activeModel = createStep(currentStep);
                return UpdateResult.from(this, activeModel.init());
            } else {
                completed = true;
                return UpdateResult.from(this, Command.quit());
            }
        }

        // User pressed back
        if (currentStep > 1) {
            currentStep--;
            activeModel = createStep(currentStep);
            return UpdateResult.from(this, activeModel.init());
        }

        return UpdateResult.from(this);
    }

    private boolean isQuitCommand(UpdateResult<?> result) {
        return result.command() != null;
    }

    private boolean isStepCancelled() {
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
                if (activeModel instanceof TextInputStep step) {
                    protoDir = step.value();
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
                    enumConstantMode = (EnumConstantMode) step.selectedValue();
                }
            }
            case 6 -> {
                if (activeModel instanceof StepModel<?> step) {
                    generateBuilders = (Boolean) step.selectedValue();
                }
            }
            case 7 -> {
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
            case 1 -> new TextInputStep(
                    "Proto Directory",
                    "Where are your .proto files located?",
                    protoDir, 1, TOTAL_STEPS);
            case 2 -> new TextInputStep(
                    "Java Output Directory",
                    "Where should generated Java files be written?",
                    javaSourceDir, 2, TOTAL_STEPS);
            case 3 -> new StepModel<>(
                    "Message Type Form",
                    "How should proto messages be represented in Java?",
                    List.of(
                            new Option<>("Record", "Annotated record components", TypeForm.RECORD),
                            new Option<>("Class", "Private fields + constructor + accessors", TypeForm.CLASS),
                            new Option<>("Interface", "Abstract getter methods", TypeForm.INTERFACE),
                            new Option<>("Sealed Class", "Abstract parent + permitted subtypes", TypeForm.SEALED_CLASS),
                            new Option<>("Sealed Interface", "Sealed + record subtypes", TypeForm.SEALED_INTERFACE)
                    ),
                    CodePreview::forTypeForm, 3, TOTAL_STEPS);
            case 4 -> new StepModel<>(
                    "Enum Form",
                    "How should proto enums be represented in Java?",
                    List.of(
                            new Option<>("Java Enum", "Standard Java enum type", EnumForm.JAVA_ENUM),
                            new Option<>("Class", "Class with static constant fields", EnumForm.CLASS),
                            new Option<>("Record", "Record with static constant fields", EnumForm.RECORD),
                            new Option<>("Sealed Interface", "Sealed interface + record subtypes", EnumForm.SEALED_INTERFACE),
                            new Option<>("Sealed Abstract Class", "Sealed abstract class + subtypes", EnumForm.SEALED_ABSTRACT_CLASS)
                    ),
                    CodePreview::forEnumForm, 4, TOTAL_STEPS);
            case 5 -> new StepModel<>(
                    "Enum Constant Mode",
                    "How should enum constants be annotated?",
                    List.of(
                            new Option<>("@ProtobufEnum.Constant", "Annotate each constant with its index",
                                    EnumConstantMode.CONSTANT_ANNOTATION),
                            new Option<>("Serializer / Deserializer", "Custom serializer/deserializer methods",
                                    EnumConstantMode.SERIALIZER_DESERIALIZER)
                    ),
                    CodePreview::forEnumConstantMode, 5, TOTAL_STEPS);
            case 6 -> new StepModel<>(
                    "Builder Generation",
                    "Generate @ProtobufBuilder constructors?",
                    List.of(
                            new Option<>("Yes", "Auto-generate fluent builders", true),
                            new Option<>("No", "No builder generation", false)
                    ),
                    CodePreview::forBuilders, 6, TOTAL_STEPS);
            case 7 -> new StepModel<>(
                    "Unknown Fields",
                    "Include @ProtobufUnknownFields support?",
                    List.of(
                            new Option<>("No", "Unknown fields will be discarded", false),
                            new Option<>("Yes", "Store unknown fields for round-tripping", true)
                    ),
                    CodePreview::forUnknownFields, 7, TOTAL_STEPS);
            case 8 -> new ConfirmationStep(
                    protoDir, javaSourceDir, typeForm, enumForm,
                    enumConstantMode, generateBuilders, includeUnknownFields,
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
        config.setProtoDir(protoDir);
        config.setJavaSourceDir(javaSourceDir);
        config.defaults().setTypeForm(typeForm);
        config.defaults().setEnumForm(enumForm);
        config.defaults().setEnumConstantMode(enumConstantMode);
        config.defaults().setGenerateBuilders(generateBuilders);
        config.defaults().setIncludeUnknownFields(includeUnknownFields);
        return config;
    }
}

package it.auties.protobuf.schema.tui;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.PlacementDecorator;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import it.auties.protobuf.schema.config.ConfigLoader;
import it.auties.protobuf.schema.config.SchemaConfig;
import it.auties.protobuf.schema.tui.generate.GenerateModel;
import it.auties.protobuf.schema.tui.onboarding.OnboardingModel;
import it.auties.protobuf.schema.tui.update.UpdateModel;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The single top-level TUI model for the entire schema tool.
 *
 * <p>Manages all state transitions within one {@link Program} instance
 * to avoid terminal capture/release issues between sequential programs.
 * Transitions through: welcome screen, onboarding wizard (if needed),
 * and generation/update flows.
 */
public final class SchemaApp implements Model {

    /**
     * The possible application states.
     */
    enum AppState {
        /**
         * Showing the welcome/mode-selection screen.
         */
        WELCOME,
        /**
         * Running the onboarding wizard.
         */
        ONBOARDING,
        /**
         * Running the generation flow.
         */
        GENERATING,
        /**
         * Running the update flow.
         */
        UPDATING,
        /**
         * Application is done.
         */
        DONE
    }

    private AppState state;
    private Model activeModel;
    private SchemaConfig config;
    private final Path workingDir;
    private int termWidth;
    private int termHeight;

    /**
     * Constructs the schema application.
     *
     * <p>Starts at the welcome screen for mode selection. If a config
     * is already loaded, it will skip onboarding when the user picks generate.
     *
     * @param workingDir the working directory
     */
    public SchemaApp(Path workingDir) {
        this.workingDir = workingDir;
        this.config = null;
        this.state = AppState.WELCOME;
        this.activeModel = new WelcomeModel();
        this.termWidth = 80;
        this.termHeight = 24;

        try {
            var loaded = ConfigLoader.load(workingDir);
            loaded.ifPresent(c -> config = c);
        } catch (IOException ignored) {
        }
    }

    @Override
    public Command init() {
        var childInit = activeModel.init();
        return Command.batch(Command.checkWindowSize(), childInit);
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage wsm) {
            termWidth = wsm.width();
            termHeight = wsm.height();
        }

        var result = activeModel.update(msg);
        activeModel = result.model();

        if (isQuitSignal(result)) {
            return handleTransition();
        }

        return UpdateResult.from(this, result.command());
    }

    @Override
    public String view() {
        return PlacementDecorator.place(
                termWidth, termHeight,
                Position.Center, Position.Center,
                activeModel.view()
        );
    }

    private boolean isQuitSignal(UpdateResult<?> result) {
        if (result.command() == null) {
            return false;
        }

        return switch (state) {
            case WELCOME -> activeModel instanceof WelcomeModel w && (w.isConfirmed() || w.isCancelled());
            case ONBOARDING -> activeModel instanceof OnboardingModel o && (o.isCompleted() || o.isCancelled());
            case GENERATING -> activeModel instanceof GenerateModel g && g.isQuit();
            case UPDATING -> activeModel instanceof UpdateModel u && u.isQuit();
            case DONE -> true;
        };
    }

    private UpdateResult<? extends Model> handleTransition() {
        return switch (state) {
            case WELCOME -> transitionFromWelcome();
            case ONBOARDING -> transitionFromOnboarding();
            case GENERATING, UPDATING, DONE -> {
                state = AppState.DONE;
                yield UpdateResult.from(this, Command.quit());
            }
        };
    }

    private UpdateResult<? extends Model> transitionFromWelcome() {
        if (!(activeModel instanceof WelcomeModel welcome) || welcome.isCancelled()) {
            state = AppState.DONE;
            return UpdateResult.from(this, Command.quit());
        }

        return switch (welcome.selectedMode()) {
            case GENERATE -> {
                if (config != null) {
                    state = AppState.GENERATING;
                    activeModel = new GenerateModel(config);
                    yield UpdateResult.from(this, activeModel.init());
                } else {
                    state = AppState.ONBOARDING;
                    activeModel = new OnboardingModel();
                    yield UpdateResult.from(this, activeModel.init());
                }
            }
            case UPDATE -> {
                if (config != null) {
                    state = AppState.UPDATING;
                    activeModel = new UpdateModel(config);
                    yield UpdateResult.from(this, activeModel.init());
                } else {
                    state = AppState.ONBOARDING;
                    activeModel = new OnboardingModel();
                    yield UpdateResult.from(this, activeModel.init());
                }
            }
        };
    }

    private UpdateResult<? extends Model> transitionFromOnboarding() {
        if (!(activeModel instanceof OnboardingModel onboarding) || onboarding.isCancelled()) {
            state = AppState.WELCOME;
            activeModel = new WelcomeModel();
            return UpdateResult.from(this, activeModel.init());
        }

        if (onboarding.isCompleted()) {
            config = onboarding.buildConfig();
            try {
                ConfigLoader.save(config, workingDir);
            } catch (IOException ignored) {
            }

            state = AppState.GENERATING;
            activeModel = new GenerateModel(config);
            return UpdateResult.from(this, activeModel.init());
        }

        return UpdateResult.from(this);
    }

    /**
     * Returns the resolved schema configuration, or {@code null} if the wizard was cancelled.
     *
     * @return the config
     */
    public SchemaConfig config() {
        return config;
    }
}

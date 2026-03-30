package com.github.auties00.daedalus.cli.tui;

import com.github.auties00.daedalus.cli.spi.DaedalusExtension;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.PlacementDecorator;
import com.williamcallahan.tui4j.compat.lipgloss.Position;

import java.nio.file.Path;
import java.util.List;

/**
 * The top-level TUI model for the Daedalus application.
 *
 * <p>Manages transitions between the central welcome screen (extension
 * selection) and the selected extension's own TUI model. When only one
 * extension is available, skips the welcome screen and delegates directly
 * to that extension.
 */
public final class DaedalusApp implements Model {

    /**
     * The possible application states.
     */
    enum AppState {
        /**
         * Showing the extension selection screen.
         */
        WELCOME,
        /**
         * Running the selected extension's TUI.
         */
        EXTENSION,
        /**
         * Application is done.
         */
        DONE
    }

    private AppState state;
    private Model activeModel;
    private final List<DaedalusExtension> extensions;
    private final Path workingDir;
    private int termWidth;
    private int termHeight;

    /**
     * Constructs the Daedalus application.
     *
     * <p>If exactly one extension is available, skips the welcome screen
     * and delegates directly to that extension's TUI model. Otherwise,
     * displays the welcome screen for extension selection.
     *
     * @param extensions the available extensions discovered via {@link java.util.ServiceLoader}
     * @param workingDir the current working directory
     */
    public DaedalusApp(List<DaedalusExtension> extensions, Path workingDir) {
        this.extensions = extensions;
        this.workingDir = workingDir;
        this.termWidth = 80;
        this.termHeight = 24;

        if (extensions.size() == 1) {
            this.state = AppState.EXTENSION;
            this.activeModel = extensions.getFirst().createTuiModel(workingDir);
        } else {
            this.state = AppState.WELCOME;
            this.activeModel = new WelcomeModel(extensions);
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
        if (state == AppState.WELCOME) {
            return PlacementDecorator.place(
                    termWidth, termHeight,
                    Position.Center, Position.Center,
                    activeModel.view()
            );
        }
        return activeModel.view();
    }

    private boolean isQuitSignal(UpdateResult<?> result) {
        if (result.command() == null) {
            return false;
        }

        return switch (state) {
            case WELCOME -> activeModel instanceof WelcomeModel w && (w.isConfirmed() || w.isCancelled());
            case EXTENSION -> true;
            case DONE -> true;
        };
    }

    private UpdateResult<? extends Model> handleTransition() {
        return switch (state) {
            case WELCOME -> transitionFromWelcome();
            case EXTENSION, DONE -> {
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

        var selected = welcome.selectedExtension();
        state = AppState.EXTENSION;
        activeModel = selected.createTuiModel(workingDir);
        return UpdateResult.from(this, activeModel.init());
    }
}

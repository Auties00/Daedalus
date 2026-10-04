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
 * <p>Always starts at the welcome screen, which lists every loaded extension, and
 * hands control to the TUI model of the extension that the user selects. Messages
 * and commands of the selected extension are passed through unchanged, so the
 * extension decides when the application ends by returning {@link Command#quit()}, and
 * asks to go back to the welcome screen through {@link BackNavigation}.
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
    private boolean forwardNextSize;
    private int selection;

    /**
     * Constructs the Daedalus application, which starts at the welcome screen
     * that lists the given extensions.
     *
     * @param extensions the available extensions discovered via {@link java.util.ServiceLoader}
     * @param workingDir the current working directory
     */
    public DaedalusApp(List<DaedalusExtension> extensions, Path workingDir) {
        this.extensions = extensions;
        this.workingDir = workingDir;
        this.selection = 0;
        this.termWidth = 80;
        this.termHeight = 24;
        this.state = AppState.WELCOME;
        this.activeModel = new WelcomeModel(extensions);
    }

    @Override
    public Command init() {
        return Command.batch(Command.checkWindowSize(), activeModel.init());
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage wsm) {
            var changed = wsm.width() != termWidth || wsm.height() != termHeight;
            termWidth = wsm.width();
            termHeight = wsm.height();
            if (!changed && !forwardNextSize) {
                return UpdateResult.from(this);
            }
            forwardNextSize = false;
        }

        var result = activeModel.update(msg);
        activeModel = result.model();

        if (state == AppState.EXTENSION && activeModel instanceof BackNavigation navigation && navigation.isBackRequested()) {
            return transitionToWelcome();
        }

        if (state == AppState.WELCOME && isWelcomeFinished()) {
            return transitionFromWelcome();
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

    private boolean isWelcomeFinished() {
        return activeModel instanceof WelcomeModel welcome && (welcome.isConfirmed() || welcome.isCancelled());
    }

    private UpdateResult<? extends Model> transitionToWelcome() {
        state = AppState.WELCOME;
        activeModel = new WelcomeModel(extensions, selection);
        return UpdateResult.from(this);
    }

    private UpdateResult<? extends Model> transitionFromWelcome() {
        if (!(activeModel instanceof WelcomeModel welcome) || welcome.isCancelled()) {
            state = AppState.DONE;
            return UpdateResult.from(this, Command.quit());
        }

        selection = welcome.selectedIndex();
        var selected = welcome.selectedExtension();
        state = AppState.EXTENSION;
        activeModel = selected.createTuiModel(workingDir);
        forwardNextSize = true;
        return UpdateResult.from(this, Command.batch(activeModel.init(), Command.checkWindowSize()));
    }
}

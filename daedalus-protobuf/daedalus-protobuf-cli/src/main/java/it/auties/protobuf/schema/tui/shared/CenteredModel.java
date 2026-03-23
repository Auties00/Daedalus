package it.auties.protobuf.schema.tui.shared;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.PlacementDecorator;
import com.williamcallahan.tui4j.compat.lipgloss.Position;

/**
 * A model wrapper that centers its child model's output within the terminal.
 *
 * <p>Intercepts {@link WindowSizeMessage} to track terminal dimensions and
 * uses {@link PlacementDecorator#place} to center the child's rendered view
 * both horizontally and vertically. All other messages are delegated to
 * the wrapped model unchanged.
 */
public final class CenteredModel implements Model {
    private Model child;
    private int termWidth;
    private int termHeight;

    /**
     * Constructs a centered model wrapping the given child.
     *
     * @param child the model whose output should be centered
     */
    public CenteredModel(Model child) {
        this.child = child;
        this.termWidth = 80;
        this.termHeight = 24;
    }

    @Override
    public Command init() {
        return Command.batch(Command.checkWindowSize(), child.init());
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage wsm) {
            termWidth = wsm.width();
            termHeight = wsm.height();
        }

        var result = child.update(msg);
        child = result.model();
        return UpdateResult.from(this, result.command());
    }

    @Override
    public String view() {
        return PlacementDecorator.place(
                termWidth, termHeight,
                Position.Center, Position.Center,
                child.view()
        );
    }

    /**
     * Returns the wrapped child model.
     *
     * @return the child model
     */
    public Model child() {
        return child;
    }
}
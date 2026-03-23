package it.auties.protobuf.schema.tui.shared;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import it.auties.protobuf.schema.tui.Theme;

/**
 * A progress indicator following the Bubble Tea architecture.
 *
 * <p>Displays a progress bar with a percentage label and an optional
 * status message. The bar animates frame-by-frame via a tick message.
 */
public final class ProgressModel implements Model {
    private static final int BAR_WIDTH = 40;
    private static final String FILLED_CHAR = "█";
    private static final String EMPTY_CHAR = "░";

    private double percent;
    private String status;
    private boolean done;

    /**
     * Constructs a progress model at zero percent.
     */
    public ProgressModel() {
        this.percent = 0.0;
        this.status = "";
        this.done = false;
    }

    /**
     * A message indicating the progress has changed.
     *
     * @param percent the new progress percentage (0.0 to 1.0)
     * @param status the new status message
     */
    public record ProgressMessage(double percent, String status) implements Message {
    }

    /**
     * A message indicating the operation is complete.
     */
    public record DoneMessage() implements Message {
    }

    @Override
    public Command init() {
        return Command.none();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof ProgressMessage pm) {
            percent = Math.min(1.0, Math.max(0.0, pm.percent()));
            status = pm.status();
            return UpdateResult.from(this);
        }
        if (msg instanceof DoneMessage) {
            percent = 1.0;
            done = true;
            return UpdateResult.from(this);
        }
        return UpdateResult.from(this);
    }

    @Override
    public String view() {
        var sb = new StringBuilder();
        var filled = (int) (percent * BAR_WIDTH);
        var empty = BAR_WIDTH - filled;

        sb.append("  ");
        sb.append(Theme.PROGRESS_FILLED.render(FILLED_CHAR.repeat(filled)));
        sb.append(Theme.PROGRESS_EMPTY.render(EMPTY_CHAR.repeat(empty)));
        sb.append(" ");
        sb.append(Theme.BODY.render(String.format("%3d%%", (int) (percent * 100))));
        sb.append("\n");

        if (!status.isEmpty()) {
            sb.append("  ");
            sb.append(Theme.MUTED_TEXT.render(status));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns whether the progress is complete.
     *
     * @return {@code true} if the progress has reached 100%
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Returns the current progress percentage.
     *
     * @return the progress from 0.0 to 1.0
     */
    public double percent() {
        return percent;
    }
}

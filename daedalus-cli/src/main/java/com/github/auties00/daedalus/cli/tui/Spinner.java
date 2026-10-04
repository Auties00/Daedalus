package com.github.auties00.daedalus.cli.tui;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.time.Duration;

/**
 * An animated activity indicator for screens that wait for a background task.
 *
 * <p>A model schedules {@link #tick()} while it waits, advances its frame counter every time
 * it receives a {@link TickMessage}, renders the frame with {@link #frame(int)}, and stops
 * scheduling ticks once the task is done.
 */
public final class Spinner {
    private static final String[] FRAMES = {
            "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
    };
    private static final Duration INTERVAL = Duration.ofMillis(80);

    private Spinner() {
    }

    /**
     * A message that tells the model that the spinner has to advance to its next frame.
     */
    public record TickMessage() implements Message {
    }

    /**
     * Schedules the next {@link TickMessage}.
     *
     * @return the command that delivers the message after the frame interval
     */
    public static Command tick() {
        return Command.tick(INTERVAL, _ -> new TickMessage());
    }

    /**
     * Renders a frame of the spinner.
     *
     * @param index the number of ticks received so far
     * @return the styled frame
     */
    public static String frame(int index) {
        return Theme.INFO_TEXT.render(FRAMES[Math.floorMod(index, FRAMES.length)]);
    }
}

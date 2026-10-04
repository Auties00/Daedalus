package com.github.auties00.daedalus.protobuf.cli.tui.update;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufParserException;
import com.github.auties00.daedalus.cli.tui.Screen;
import com.github.auties00.daedalus.cli.tui.Spinner;
import com.github.auties00.daedalus.cli.tui.StatusBar;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Join;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.SourceChange;
import com.github.auties00.daedalus.cli.tui.Theme;
import com.github.auties00.daedalus.protobuf.cli.update.UpdateEngine;
import com.github.auties00.daedalus.protobuf.cli.update.UpdatePlan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The TUI model for the update flow.
 *
 * <p>Scans existing Java sources, matches them against proto definitions,
 * computes diffs, and displays a summary of proposed changes: the changed files
 * are listed next to a preview of the diff of the highlighted file, the same way
 * the options of the onboarding wizard are listed next to their previews. The user
 * can open the full diff of a file and accept or reject the changes.
 */
public final class UpdateModel implements Model {
    private static final int LIST_ROWS = 10;
    private static final int MAX_PREVIEW_LINES = 12;
    private static final int MIN_PREVIEW_LINES = 3;
    private static final int LISTED_ITEMS = 5;
    private static final int DIFF_BODY_CHROME_LINES = 10;
    private static final int SUMMARY_BODY_CHROME_LINES = 15;
    private static final int MIN_DIFF_LINES = 4;
    private static final int MAX_DIFF_WIDTH = 68;

    /**
     * The possible states of the update flow.
     */
    enum State {
        /**
         * Scanning and computing the update plan.
         */
        COMPUTING,
        /**
         * Showing the update plan summary.
         */
        SUMMARY,
        /**
         * Showing diffs for a specific file.
         */
        VIEWING_DIFF,
        /**
         * Update has been applied.
         */
        APPLIED,
        /**
         * An error occurred.
         */
        ERROR
    }

    private State state;
    private final SchemaConfig config;
    private UpdateEngine engine;
    private UpdatePlan plan;
    private String errorMessage;
    private boolean quit;
    private boolean back;
    private int selectedIndex;
    private int diffScroll;
    private int termHeight;
    private int spinnerFrame;
    private List<UpdatePlan.PlannedUpdate> updatesWithChanges;
    private List<Path> appliedFiles;

    /**
     * A message indicating the update plan computation has completed.
     *
     * @param plan the computed update plan
     */
    public record UpdatePlanReadyMessage(UpdatePlan plan) implements Message {
    }

    /**
     * A message indicating the update plan computation failed.
     *
     * @param error the error message
     */
    public record UpdateErrorMessage(String error) implements Message {
    }

    /**
     * A message indicating the changes have been applied.
     *
     * @param files the list of modified file paths
     */
    public record ChangesAppliedMessage(List<Path> files) implements Message {
    }

    /**
     * Constructs an update model with the given configuration.
     *
     * @param config the schema configuration
     */
    public UpdateModel(SchemaConfig config) {
        this.config = config;
        this.state = State.COMPUTING;
        this.quit = false;
        this.back = false;
        this.selectedIndex = 0;
        this.diffScroll = 0;
        this.termHeight = 40;
        this.spinnerFrame = 0;
        this.updatesWithChanges = new ArrayList<>();
        this.appliedFiles = new ArrayList<>();
    }

    @Override
    public Command init() {
        Command compute = () -> {
            try {
                var eng = new UpdateEngine(config);
                this.engine = eng;
                var updatePlan = eng.computeUpdatePlan();
                return new UpdatePlanReadyMessage(updatePlan);
            } catch (IOException | ProtobufParserException e) {
                return new UpdateErrorMessage(e.getMessage());
            }
        };
        return Command.batch(compute, Spinner.tick());
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage size) {
            termHeight = size.height();
            return UpdateResult.from(this);
        }

        if (msg instanceof Spinner.TickMessage) {
            if (state != State.COMPUTING) {
                return UpdateResult.from(this);
            }
            spinnerFrame++;
            return UpdateResult.from(this, Spinner.tick());
        }

        if (msg instanceof UpdatePlanReadyMessage ready) {
            this.plan = ready.plan();
            this.updatesWithChanges = plan.updates().stream()
                    .filter(u -> u.sourceChange().hasChanges())
                    .toList();
            state = State.SUMMARY;
            return UpdateResult.from(this);
        }

        if (msg instanceof UpdateErrorMessage error) {
            state = State.ERROR;
            errorMessage = error.error();
            return UpdateResult.from(this);
        }

        if (msg instanceof ChangesAppliedMessage applied) {
            state = State.APPLIED;
            appliedFiles = applied.files();
            return UpdateResult.from(this);
        }

        if (msg instanceof KeyPressMessage key) {
            return handleKey(key);
        }

        return UpdateResult.from(this);
    }

    private UpdateResult<? extends Model> handleKey(KeyPressMessage key) {
        var keyStr = key.key();

        switch (state) {
            case SUMMARY -> {
                switch (keyStr) {
                    case "esc" -> {
                        back = true;
                        return UpdateResult.from(this, Command.quit());
                    }
                    case "q", "ctrl+c" -> {
                        quit = true;
                        return UpdateResult.from(this, Command.quit());
                    }
                    case "up", "k" -> select(selectedIndex - 1);
                    case "down", "j" -> select(selectedIndex + 1);
                    case "enter" -> {
                        if (!updatesWithChanges.isEmpty()) {
                            diffScroll = 0;
                            state = State.VIEWING_DIFF;
                        }
                    }
                    case "a" -> {
                        if (plan != null && plan.hasChanges()) {
                            return applyChanges();
                        }
                    }
                }
            }
            case VIEWING_DIFF -> {
                switch (keyStr) {
                    case "q", "esc", "backspace", "ctrl+h" -> state = State.SUMMARY;
                    case "up", "k" -> scrollDiff(-1);
                    case "down", "j" -> scrollDiff(1);
                    case "n" -> {
                        select(selectedIndex + 1);
                        diffScroll = 0;
                    }
                    case "p" -> {
                        select(selectedIndex - 1);
                        diffScroll = 0;
                    }
                }
            }
            case APPLIED, ERROR -> {
                if (keyStr.equals("esc")) {
                    back = true;
                    return UpdateResult.from(this, Command.quit());
                }
                if (keyStr.equals("q") || keyStr.equals("enter") || keyStr.equals("ctrl+c")) {
                    quit = true;
                    return UpdateResult.from(this, Command.quit());
                }
            }
            default -> {
            }
        }

        return UpdateResult.from(this);
    }

    private void select(int index) {
        selectedIndex = Math.max(0, Math.min(updatesWithChanges.size() - 1, index));
    }

    private void scrollDiff(int delta) {
        var lines = selectedChange().unifiedDiff().size();
        var maxScroll = Math.max(0, lines - visibleDiffLines());
        diffScroll = Math.max(0, Math.min(maxScroll, diffScroll + delta));
    }

    private int visibleDiffLines() {
        return Math.max(MIN_DIFF_LINES, termHeight - Screen.headerRows(termHeight) - DIFF_BODY_CHROME_LINES);
    }

    private int previewLines() {
        var sections = sectionRows(plan.unmatchedProtoTypes().size()) + sectionRows(plan.conflicts().size());
        var available = termHeight - Screen.headerRows(termHeight) - SUMMARY_BODY_CHROME_LINES - sections;
        return Math.max(MIN_PREVIEW_LINES, Math.min(MAX_PREVIEW_LINES, available));
    }

    private int sectionRows(int items) {
        return items == 0 ? 0 : 3 + Math.min(items, LISTED_ITEMS) + (items > LISTED_ITEMS ? 1 : 0);
    }

    private SourceChange selectedChange() {
        return updatesWithChanges.get(selectedIndex).sourceChange();
    }

    private UpdateResult<? extends Model> applyChanges() {
        return UpdateResult.from(this, () -> {
            try {
                var files = engine.applyPlan(plan);
                return new ChangesAppliedMessage(files);
            } catch (IOException e) {
                return new UpdateErrorMessage(e.getMessage());
            }
        });
    }

    @Override
    public String view() {
        return switch (state) {
            case COMPUTING -> viewComputing();
            case SUMMARY -> viewSummary();
            case VIEWING_DIFF -> viewDiff();
            case APPLIED -> viewApplied();
            case ERROR -> viewError();
        };
    }

    private String viewComputing() {
        return Screen.of("Protobuf", "Update", termHeight)
                .indicator("Working")
                .result(Spinner.frame(spinnerFrame), "Scanning Java sources and computing the update plan")
                .entry("Proto files", String.join(", ", config.protoFiles()))
                .entry("Source directory", config.javaSourceDir())
                .render();
    }

    private String viewSummary() {
        if (!plan.hasChanges() && plan.unmatchedProtoTypes().isEmpty() && plan.conflicts().isEmpty()) {
            return Screen.of("Protobuf", "Update", termHeight)
                    .indicator("Up to date")
                    .result(Theme.SUCCESS_TEXT.render("✓"), "All Java sources are up to date")
                    .entry("Proto files", String.join(", ", config.protoFiles()))
                    .entry("Source directory", config.javaSourceDir())
                    .keys(new StatusBar().add("esc", "back").add("q", "quit"))
                    .render();
        }

        var screen = Screen.of("Protobuf", "Update", termHeight)
                .indicator("Review")
                .block(renderStats())
                .blank();

        if (!updatesWithChanges.isEmpty()) {
            screen.heading("Files with changes");
            screen.block(Join.joinHorizontal(Position.Top, renderFileList(), "    ", renderDiffPreview()));
            screen.blank();
        }

        if (!plan.unmatchedProtoTypes().isEmpty()) {
            screen.heading("Unmatched proto types");
            var types = plan.unmatchedProtoTypes().stream()
                    .map(type -> Theme.WARNING_TEXT.render("? ") + Theme.BODY.render(String.valueOf(type)))
                    .toList();
            screen.block(renderLimited(types)).blank();
        }

        if (!plan.conflicts().isEmpty()) {
            screen.heading("Conflicts");
            var conflicts = plan.conflicts().stream()
                    .map(conflict -> Theme.DANGER_TEXT.render("! ") + Theme.BODY.render(conflict.message()))
                    .toList();
            screen.block(renderLimited(conflicts)).blank();
        }

        return screen
                .keys(new StatusBar()
                        .add("↑↓", "navigate")
                        .add("enter", "view diff")
                        .add("a", "apply all")
                        .add("esc", "back")
                        .add("q", "quit"))
                .render();
    }

    private String renderStats() {
        var separator = Theme.DIM_TEXT.render("   ");
        var sb = new StringBuilder();
        sb.append(Theme.MUTED_TEXT.render("Matched "));
        sb.append(Theme.BODY.render(String.valueOf(plan.updates().size())));
        sb.append(separator);
        sb.append(Theme.MUTED_TEXT.render("Changes "));
        sb.append(Theme.SUCCESS_TEXT.render(String.valueOf(plan.changeCount())));
        if (!plan.unmatchedProtoTypes().isEmpty()) {
            sb.append(separator);
            sb.append(Theme.MUTED_TEXT.render("Unmatched "));
            sb.append(Theme.WARNING_TEXT.render(String.valueOf(plan.unmatchedProtoTypes().size())));
        }
        if (!plan.conflicts().isEmpty()) {
            sb.append(separator);
            sb.append(Theme.MUTED_TEXT.render("Conflicts "));
            sb.append(Theme.DANGER_TEXT.render(String.valueOf(plan.conflicts().size())));
        }
        return sb.toString();
    }

    private String renderFileList() {
        var total = updatesWithChanges.size();
        var first = Math.max(0, Math.min(selectedIndex - LIST_ROWS / 2, total - LIST_ROWS));
        var last = Math.min(total, first + LIST_ROWS);

        var sb = new StringBuilder();
        if (first > 0) {
            sb.append(Theme.MUTED_TEXT.render("↑ " + first + " more")).append('\n');
        }

        for (var i = first; i < last; i++) {
            var change = updatesWithChanges.get(i).sourceChange();
            var name = change.filePath().getFileName().toString();
            if (i == selectedIndex) {
                sb.append(Theme.SELECTED_CURSOR.render("● "));
                sb.append(Theme.SELECTED_ITEM.render(name));
                sb.append(Theme.MUTED_TEXT.render("  " + change.description()));
            } else {
                sb.append(Theme.UNSELECTED_ITEM.render("○ " + name));
            }
            sb.append('\n');
        }

        if (last < total) {
            sb.append(Theme.MUTED_TEXT.render("↓ " + (total - last) + " more")).append('\n');
        }

        return sb.toString();
    }

    private String renderDiffPreview() {
        var diff = List.copyOf(selectedChange().unifiedDiff());
        var budget = previewLines();
        var shown = Math.min(diff.size(), budget);

        var sb = new StringBuilder();
        for (var i = 0; i < shown; i++) {
            sb.append(renderDiffLine(diff.get(i))).append('\n');
        }
        if (diff.size() > shown) {
            sb.append(Theme.MUTED_TEXT.render("… " + (diff.size() - shown) + " more lines")).append('\n');
        }
        for (var i = shown + (diff.size() > shown ? 1 : 0); i <= budget; i++) {
            sb.append('\n');
        }

        return Screen.panel("Diff", sb.toString().stripTrailing());
    }

    private String renderLimited(List<String> lines) {
        var sb = new StringBuilder();
        var listed = Math.min(lines.size(), LISTED_ITEMS);
        for (var i = 0; i < listed; i++) {
            sb.append(lines.get(i)).append('\n');
        }
        if (lines.size() > listed) {
            sb.append(Theme.MUTED_TEXT.render("and " + (lines.size() - listed) + " more")).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String renderDiffLine(String line) {
        var text = line.length() > MAX_DIFF_WIDTH
                ? line.substring(0, MAX_DIFF_WIDTH - 1) + "…"
                : line;
        if (line.startsWith("+") && !line.startsWith("+++")) {
            return Theme.DIFF_ADDED.render(text);
        }
        if (line.startsWith("-") && !line.startsWith("---")) {
            return Theme.DIFF_REMOVED.render(text);
        }
        if (line.startsWith("@@")) {
            return Theme.DIFF_HEADER.render(text);
        }
        return Theme.DIFF_CONTEXT.render(text);
    }

    private String viewDiff() {
        var change = selectedChange();
        var diff = List.copyOf(change.unifiedDiff());
        var visible = visibleDiffLines();
        var last = Math.min(diff.size(), diffScroll + visible);

        var sb = new StringBuilder();
        for (var i = diffScroll; i < last; i++) {
            sb.append(renderDiffLine(diff.get(i))).append('\n');
        }

        var position = diff.isEmpty()
                ? "no changes"
                : "lines " + (diffScroll + 1) + "-" + last + " of " + diff.size();

        return Screen.of("Protobuf", "Update", termHeight)
                .indicator("File " + (selectedIndex + 1) + "/" + updatesWithChanges.size())
                .heading(change.filePath().getFileName().toString())
                .block(Screen.panel(change.description(), sb.toString().stripTrailing()))
                .block(Theme.MUTED_TEXT.render(position))
                .keys(new StatusBar()
                        .add("↑↓", "scroll")
                        .add("n", "next")
                        .add("p", "prev")
                        .add("esc", "back"))
                .render();
    }

    private String viewApplied() {
        var count = appliedFiles.size();
        var files = new StringBuilder();
        var listed = Math.min(count, LIST_ROWS);
        for (var i = 0; i < listed; i++) {
            files.append(Theme.SUCCESS_TEXT.render("~ "));
            files.append(Theme.BODY.render(appliedFiles.get(i).getFileName().toString()));
            files.append('\n');
        }
        if (count > listed) {
            files.append(Theme.MUTED_TEXT.render("and " + (count - listed) + " more")).append('\n');
        }

        var screen = Screen.of("Protobuf", "Update", termHeight)
                .indicator("Done")
                .result(Theme.SUCCESS_TEXT.render("✓"), "Updated " + count + " file" + (count != 1 ? "s" : ""));
        if (count > 0) {
            screen.block(Screen.panel("Updated files", files.toString().stripTrailing())).blank();
        }

        return screen
                .entry("Source directory", config.javaSourceDir())
                .keys(new StatusBar()
                        .add("enter", "exit")
                        .add("esc", "back")
                        .add("q", "quit"))
                .render();
    }

    private String viewError() {
        var message = errorMessage != null ? errorMessage : "Unknown error";
        return Screen.of("Protobuf", "Update", termHeight)
                .indicator("Failed")
                .result(Theme.DANGER_TEXT.render("✗"), "Update failed")
                .block(Screen.panel("Details", Theme.BODY.render(message))).blank()
                .entry("Proto files", String.join(", ", config.protoFiles()))
                .entry("Source directory", config.javaSourceDir())
                .keys(new StatusBar()
                        .add("enter", "exit")
                        .add("esc", "back")
                        .add("q", "quit"))
                .render();
    }

    /**
     * Returns whether the user has quit the update view.
     *
     * @return {@code true} if quit
     */
    public boolean isQuit() {
        return quit;
    }

    /**
     * Returns whether the user asked to go back to the previous screen.
     *
     * @return {@code true} if the user pressed escape
     */
    public boolean isBack() {
        return back;
    }
}

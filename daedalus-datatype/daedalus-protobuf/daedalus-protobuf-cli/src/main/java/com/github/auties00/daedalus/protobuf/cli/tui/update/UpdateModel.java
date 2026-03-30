package com.github.auties00.daedalus.protobuf.cli.tui.update;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.cli.tui.Theme;
import com.github.auties00.daedalus.cli.tui.shared.StatusBar;
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
 * computes diffs, and displays a summary of proposed changes. The user
 * can review changes and accept or reject them.
 */
public final class UpdateModel implements Model {

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
    private int selectedIndex;
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
        this.selectedIndex = 0;
        this.updatesWithChanges = new ArrayList<>();
        this.appliedFiles = new ArrayList<>();
    }

    @Override
    public Command init() {
        return () -> {
            try {
                var eng = new UpdateEngine(config);
                this.engine = eng;
                var updatePlan = eng.computeUpdatePlan();
                return new UpdatePlanReadyMessage(updatePlan);
            } catch (IOException e) {
                return new UpdateErrorMessage(e.getMessage());
            }
        };
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
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
                    case "q", "esc", "ctrl+c" -> {
                        quit = true;
                        return UpdateResult.from(this, Command.quit());
                    }
                    case "up", "k" -> {
                        if (selectedIndex > 0) {
                            selectedIndex--;
                        }
                    }
                    case "down", "j" -> {
                        if (selectedIndex < updatesWithChanges.size() - 1) {
                            selectedIndex++;
                        }
                    }
                    case "enter" -> {
                        if (!updatesWithChanges.isEmpty()) {
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
                    case "q", "esc", "backspace" -> state = State.SUMMARY;
                    case "n" -> {
                        if (selectedIndex < updatesWithChanges.size() - 1) {
                            selectedIndex++;
                        }
                    }
                    case "p" -> {
                        if (selectedIndex > 0) {
                            selectedIndex--;
                        }
                    }
                }
            }
            case APPLIED, ERROR -> {
                if (keyStr.equals("q") || keyStr.equals("esc") || keyStr.equals("enter") || keyStr.equals("ctrl+c")) {
                    quit = true;
                    return UpdateResult.from(this, Command.quit());
                }
            }
            default -> {
            }
        }

        return UpdateResult.from(this);
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
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Updater"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.INFO_TEXT.render("⠋ "));
        sb.append(Theme.BODY.render("Scanning Java sources and computing update plan..."));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.MUTED_TEXT.render("Proto directory: " + config.protoDir()));
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.MUTED_TEXT.render("Source directory: " + config.javaSourceDir()));
        sb.append("\n");
        return sb.toString();
    }

    private String viewSummary() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Updater"));
        sb.append("\n\n");

        if (!plan.hasChanges() && plan.unmatchedProtoTypes().isEmpty() && plan.conflicts().isEmpty()) {
            sb.append("  ");
            sb.append(Theme.SUCCESS_TEXT.render("✓ "));
            sb.append(Theme.BODY.render("All Java sources are up to date."));
            sb.append("\n\n");

            var status = new StatusBar().add("q", "quit");
            sb.append("  ").append(status.render()).append("\n");
            return sb.toString();
        }

        // Stats line
        sb.append("  ");
        sb.append(Theme.BODY.render("Matched: " + plan.updates().size()));
        sb.append(Theme.MUTED_TEXT.render(" | "));
        sb.append(Theme.SUCCESS_TEXT.render("Changes: " + plan.changeCount()));
        if (!plan.unmatchedProtoTypes().isEmpty()) {
            sb.append(Theme.MUTED_TEXT.render(" | "));
            sb.append(Theme.WARNING_TEXT.render("Unmatched: " + plan.unmatchedProtoTypes().size()));
        }
        if (!plan.conflicts().isEmpty()) {
            sb.append(Theme.MUTED_TEXT.render(" | "));
            sb.append(Theme.DANGER_TEXT.render("Conflicts: " + plan.conflicts().size()));
        }
        sb.append("\n\n");

        // File list with changes
        if (!updatesWithChanges.isEmpty()) {
            sb.append("  ");
            sb.append(Theme.SUBTITLE.render("Files with changes:"));
            sb.append("\n");

            for (var i = 0; i < updatesWithChanges.size(); i++) {
                var update = updatesWithChanges.get(i);
                var prefix = i == selectedIndex ? "  ▸ " : "    ";
                var style = i == selectedIndex ? Theme.SELECTED_ITEM : Theme.BODY;
                sb.append(style.render(prefix + update.sourceChange().filePath().getFileName()));
                sb.append("\n");
                sb.append("    ");
                sb.append(Theme.MUTED_TEXT.render("  " + update.sourceChange().description()));
                sb.append("\n");
            }
        }

        // Unmatched types
        if (!plan.unmatchedProtoTypes().isEmpty()) {
            sb.append("\n  ");
            sb.append(Theme.SUBTITLE.render("Unmatched proto types:"));
            sb.append("\n");
            for (var type : plan.unmatchedProtoTypes()) {
                sb.append("    ");
                sb.append(Theme.WARNING_TEXT.render("? " + type));
                sb.append("\n");
            }
        }

        // Conflicts
        if (!plan.conflicts().isEmpty()) {
            sb.append("\n  ");
            sb.append(Theme.SUBTITLE.render("Conflicts:"));
            sb.append("\n");
            for (var conflict : plan.conflicts()) {
                sb.append("    ");
                sb.append(Theme.DANGER_TEXT.render("! " + conflict.message()));
                sb.append("\n");
            }
        }

        sb.append("\n");
        var status = new StatusBar()
                .add("↑↓", "navigate")
                .add("enter", "view diff")
                .add("a", "apply all")
                .add("q", "quit");
        sb.append("  ").append(status.render()).append("\n");

        return sb.toString();
    }

    private String viewDiff() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Updater"));
        sb.append("\n\n");

        if (selectedIndex >= 0 && selectedIndex < updatesWithChanges.size()) {
            var update = updatesWithChanges.get(selectedIndex);
            var change = update.sourceChange();

            sb.append("  ");
            sb.append(Theme.SUBTITLE.render(change.filePath().getFileName().toString()));
            sb.append(Theme.MUTED_TEXT.render(" (" + (selectedIndex + 1) + "/"
                    + updatesWithChanges.size() + ")"));
            sb.append("\n");
            sb.append("  ");
            sb.append(Theme.MUTED_TEXT.render(change.description()));
            sb.append("\n\n");

            for (var line : change.unifiedDiff()) {
                sb.append("  ");
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    sb.append(Theme.DIFF_ADDED.render(line));
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    sb.append(Theme.DIFF_REMOVED.render(line));
                } else if (line.startsWith("@@")) {
                    sb.append(Theme.DIFF_HEADER.render(line));
                } else {
                    sb.append(Theme.DIFF_CONTEXT.render(line));
                }
                sb.append("\n");
            }
        }

        sb.append("\n");
        var status = new StatusBar()
                .add("n", "next")
                .add("p", "prev")
                .add("esc", "back");
        sb.append("  ").append(status.render()).append("\n");

        return sb.toString();
    }

    private String viewApplied() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Updater"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.SUCCESS_TEXT.render("✓ "));
        sb.append(Theme.BODY.render("Updated " + appliedFiles.size() + " file"
                + (appliedFiles.size() != 1 ? "s" : "")));
        sb.append("\n\n");

        for (var file : appliedFiles) {
            sb.append("  ");
            sb.append(Theme.SUCCESS_TEXT.render("  ~ "));
            sb.append(Theme.BODY.render(file.getFileName().toString()));
            sb.append("\n");
        }

        sb.append("\n");
        var status = new StatusBar()
                .add("enter", "exit")
                .add("q", "quit");
        sb.append("  ").append(status.render()).append("\n");

        return sb.toString();
    }

    private String viewError() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Updater"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.DANGER_TEXT.render("✗ Update failed"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.BODY.render(errorMessage != null ? errorMessage : "Unknown error"));
        sb.append("\n\n");

        var status = new StatusBar()
                .add("enter", "exit")
                .add("q", "quit");
        sb.append("  ").append(status.render()).append("\n");

        return sb.toString();
    }

    /**
     * Returns whether the user has quit the update view.
     *
     * @return {@code true} if quit
     */
    public boolean isQuit() {
        return quit;
    }
}

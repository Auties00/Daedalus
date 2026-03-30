package com.github.auties00.daedalus.protobuf.cli.command;

import com.github.auties00.daedalus.protobuf.cli.config.ConfigLoader;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.update.UpdateEngine;
import com.github.auties00.daedalus.protobuf.cli.update.UpdatePlan;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Updates existing Java model classes when {@code .proto} schemas change.
 *
 * <p>Performs additive updates: new fields and enum constants are inserted
 * without removing any existing user-written code. Detects conflicts such
 * as index collisions and presents them for review.
 *
 * <p>In TUI mode, displays a diff viewer with accept/reject per file.
 * In plain mode (with {@code --no-tui}), prints changes to stdout.
 */
@Command(
        name = "update",
        description = "Update existing Java models from changed .proto schemas",
        mixinStandardHelpOptions = true
)
public final class UpdateCommand implements Runnable {
    @Option(names = {"--proto-dir"}, description = "Directory containing .proto files")
    private Path protoDir;

    @Option(names = {"--source-dir"}, description = "Java source directory to scan and update")
    private Path sourceDir;

    @Option(names = {"--config"}, description = "Path to config file (.protobuf-schema.yml)")
    private Path configPath;

    @Option(names = {"--dry-run"}, description = "Preview changes without writing files")
    private boolean dryRun;

    @Option(names = {"--no-tui"}, description = "Disable interactive TUI, use plain output")
    private boolean noTui;

    @Override
    public void run() {
        try {
            var workingDir = Path.of(System.getProperty("user.dir"));
            var config = loadConfig(workingDir);

            if (config == null) {
                System.err.println("No configuration found. Run 'generate' first or provide --config.");
                return;
            }

            if (protoDir != null) {
                config.setProtoDir(protoDir.toString());
            }
            if (sourceDir != null) {
                config.setJavaSourceDir(sourceDir.toString());
            }

            runPlain(config);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private SchemaConfig loadConfig(Path workingDir) throws IOException {
        if (configPath != null) {
            return ConfigLoader.loadFrom(configPath);
        }

        var loaded = ConfigLoader.load(workingDir);
        return loaded.orElse(null);
    }

    private void runPlain(SchemaConfig config) {
        try {
            var engine = new UpdateEngine(config);
            var plan = engine.computeUpdatePlan();

            printPlanSummary(plan);

            if (!plan.hasChanges()) {
                System.out.println("All Java sources are up to date.");
                return;
            }

            if (!plan.conflicts().isEmpty()) {
                printConflicts(plan);
            }

            if (dryRun) {
                printDiffs(plan);
                System.out.println("\nDry run complete. No files were modified.");
            } else {
                var modifiedFiles = engine.applyPlan(plan);
                System.out.println("\nUpdated " + modifiedFiles.size() + " file(s):");
                for (var file : modifiedFiles) {
                    System.out.println("  ~ " + file);
                }
            }
        } catch (IOException e) {
            System.err.println("Update failed: " + e.getMessage());
        }
    }

    private void printPlanSummary(UpdatePlan plan) {
        var total = plan.updates().size();
        var withChanges = plan.changeCount();
        var unmatched = plan.unmatchedProtoTypes().size();

        System.out.println("Update plan:");
        System.out.println("  Matched types: " + total);
        System.out.println("  Types with changes: " + withChanges);

        if (unmatched > 0) {
            System.out.println("  Unmatched proto types: " + unmatched);
            for (var type : plan.unmatchedProtoTypes()) {
                System.out.println("    ? " + type);
            }
        }

        if (!plan.conflicts().isEmpty()) {
            System.out.println("  Conflicts: " + plan.conflicts().size());
        }
    }

    private void printConflicts(UpdatePlan plan) {
        System.out.println("\nConflicts detected:");
        for (var conflict : plan.conflicts()) {
            System.out.println("  [" + conflict.type() + "] " + conflict.message());
        }
    }

    private void printDiffs(UpdatePlan plan) {
        for (var update : plan.updates()) {
            var change = update.sourceChange();
            if (!change.hasChanges()) {
                continue;
            }

            System.out.println("\n--- " + change.filePath() + " ---");
            System.out.println(change.description());
            for (var line : change.unifiedDiff()) {
                System.out.println(line);
            }
        }
    }
}

package it.auties.protobuf.schema.cli;

import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.ProgramOption;
import it.auties.protobuf.schema.config.ConfigLoader;
import it.auties.protobuf.schema.config.SchemaConfig;
import it.auties.protobuf.schema.generation.JavaSourceGenerator;
import it.auties.protobuf.schema.tui.generate.GenerateModel;
import it.auties.protobuf.schema.tui.onboarding.OnboardingModel;
import it.auties.protobuf.schema.tui.shared.CenteredModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates Java model classes from {@code .proto} schema files.
 *
 * <p>When no configuration file is found and TUI is enabled, launches
 * an interactive onboarding wizard to create one. After configuration
 * is available, runs the generation engine and displays results.
 */
@Command(
        name = "generate",
        description = "Generate Java models from .proto schemas",
        mixinStandardHelpOptions = true
)
public final class GenerateCommand implements Runnable {
    @Option(names = {"--proto-dir"}, description = "Directory containing .proto files")
    private Path protoDir;

    @Option(names = {"--output-dir"}, description = "Directory for generated Java files")
    private Path outputDir;

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
                return;
            }

            if (protoDir != null) {
                config.setProtoDir(protoDir.toString());
            }
            if (outputDir != null) {
                config.setJavaSourceDir(outputDir.toString());
            }

            if (noTui) {
                runPlain(config);
            } else {
                runTui(config);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private SchemaConfig loadConfig(Path workingDir) throws IOException {
        if (configPath != null) {
            return ConfigLoader.loadFrom(configPath);
        }

        var loaded = ConfigLoader.load(workingDir);
        if (loaded.isPresent()) {
            return loaded.get();
        }

        if (noTui) {
            System.out.println("No .protobuf-schema.yml found. Using defaults.");
            return new SchemaConfig();
        }

        return runOnboarding(workingDir);
    }

    private SchemaConfig runOnboarding(Path workingDir) {
        var centered = new CenteredModel(new OnboardingModel());
        var program = new Program(centered, ProgramOption.withAltScreen());
        var finalModel = program.runWithFinalModel();

        if (finalModel instanceof CenteredModel c
                && c.child() instanceof OnboardingModel model) {
            if (model.isCancelled()) {
                System.out.println("Configuration cancelled.");
                return null;
            }
            if (model.isCompleted()) {
                var config = model.buildConfig();
                try {
                    ConfigLoader.save(config, workingDir);
                    System.out.println("Configuration saved to .protobuf-schema.yml");
                } catch (IOException e) {
                    System.err.println("Warning: Could not save config: " + e.getMessage());
                }
                return config;
            }
        }
        return new SchemaConfig();
    }

    private void runTui(SchemaConfig config) {
        var centered = new CenteredModel(new GenerateModel(config));
        var program = new Program(centered, ProgramOption.withAltScreen());
        program.run();
    }

    private void runPlain(SchemaConfig config) {
        try {
            var generator = new JavaSourceGenerator(config);
            var results = generator.generate();

            System.out.println("Generated " + results.size() + " Java source file(s).");
            for (var result : results) {
                System.out.println("  + " + result);
            }
        } catch (IOException e) {
            System.err.println("Generation failed: " + e.getMessage());
        }
    }
}

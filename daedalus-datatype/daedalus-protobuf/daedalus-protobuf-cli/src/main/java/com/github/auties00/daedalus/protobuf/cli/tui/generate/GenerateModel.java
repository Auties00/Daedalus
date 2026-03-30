package com.github.auties00.daedalus.protobuf.cli.tui.generate;

import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.generation.JavaSourceGenerator;
import com.github.auties00.daedalus.cli.tui.Theme;
import com.github.auties00.daedalus.cli.tui.shared.StatusBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The TUI model for the generation flow.
 *
 * <p>Shows a progress view while generating Java source files from proto
 * schemas, then displays a summary of generated files with success/error status.
 */
public final class GenerateModel implements Model {

    /**
     * The possible states of the generation flow.
     */
    enum State {
        /**
         * Generation is in progress.
         */
        GENERATING,
        /**
         * Generation is complete, showing results.
         */
        DONE,
        /**
         * An error occurred during generation.
         */
        ERROR
    }

    private State state;
    private final SchemaConfig config;
    private final List<String> generatedFiles;
    private String errorMessage;
    private boolean quit;

    /**
     * A message indicating generation should start.
     */
    public record StartGenerationMessage() implements Message {
    }

    /**
     * A message indicating generation has completed.
     *
     * @param files the list of generated file paths
     */
    public record GenerationCompleteMessage(List<String> files) implements Message {
    }

    /**
     * A message indicating generation failed.
     *
     * @param error the error message
     */
    public record GenerationErrorMessage(String error) implements Message {
    }

    /**
     * Constructs a generation model with the given configuration.
     *
     * @param config the schema configuration
     */
    public GenerateModel(SchemaConfig config) {
        this.config = config;
        this.state = State.GENERATING;
        this.generatedFiles = new ArrayList<>();
        this.quit = false;
    }

    @Override
    public Command init() {
        return () -> {
            try {
                var generator = new JavaSourceGenerator(config);
                var files = generator.generate();
                return new GenerationCompleteMessage(files);
            } catch (IOException e) {
                return new GenerationErrorMessage(e.getMessage());
            }
        };
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof GenerationCompleteMessage complete) {
            state = State.DONE;
            generatedFiles.addAll(complete.files());
            return UpdateResult.from(this);
        }
        if (msg instanceof GenerationErrorMessage error) {
            state = State.ERROR;
            errorMessage = error.error();
            return UpdateResult.from(this);
        }
        if (msg instanceof KeyPressMessage key) {
            var keyStr = key.key();
            if (keyStr.equals("q") || keyStr.equals("esc") || keyStr.equals("enter") || keyStr.equals("ctrl+c")) {
                quit = true;
                return UpdateResult.from(this, Command.quit());
            }
        }
        return UpdateResult.from(this);
    }

    @Override
    public String view() {
        return switch (state) {
            case GENERATING -> viewGenerating();
            case DONE -> viewDone();
            case ERROR -> viewError();
        };
    }

    private String viewGenerating() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Generator"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.INFO_TEXT.render("⠋ "));
        sb.append(Theme.BODY.render("Generating Java source files..."));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.MUTED_TEXT.render("Proto directory: " + config.protoDir()));
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.MUTED_TEXT.render("Output directory: " + config.javaSourceDir()));
        sb.append("\n");
        return sb.toString();
    }

    private String viewDone() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Generator"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.SUCCESS_TEXT.render("✓ "));
        sb.append(Theme.BODY.render("Generated " + generatedFiles.size() + " file"
                + (generatedFiles.size() != 1 ? "s" : "")));
        sb.append("\n\n");

        for (var file : generatedFiles) {
            sb.append("  ");
            sb.append(Theme.SUCCESS_TEXT.render("  + "));
            sb.append(Theme.BODY.render(file));
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.MUTED_TEXT.render("Output: " + config.javaSourceDir()));
        sb.append("\n\n");

        var status = new StatusBar()
                .add("enter", "exit")
                .add("q", "quit");
        sb.append("  ");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    private String viewError() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append("  ");
        sb.append(Theme.TITLE.render("ModernProtobuf Schema Generator"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.DANGER_TEXT.render("✗ Generation failed"));
        sb.append("\n\n");
        sb.append("  ");
        sb.append(Theme.BODY.render(errorMessage != null ? errorMessage : "Unknown error"));
        sb.append("\n\n");

        var status = new StatusBar()
                .add("enter", "exit")
                .add("q", "quit");
        sb.append("  ");
        sb.append(status.render());
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Returns the list of generated file paths.
     *
     * @return the generated files
     */
    public List<String> generatedFiles() {
        return generatedFiles;
    }

    /**
     * Returns whether the user has quit the results view.
     *
     * @return {@code true} if quit
     */
    public boolean isQuit() {
        return quit;
    }
}

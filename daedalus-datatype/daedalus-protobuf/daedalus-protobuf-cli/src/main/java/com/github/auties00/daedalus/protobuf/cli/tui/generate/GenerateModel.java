package com.github.auties00.daedalus.protobuf.cli.tui.generate;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufParserException;
import com.github.auties00.daedalus.cli.tui.Screen;
import com.github.auties00.daedalus.cli.tui.Spinner;
import com.github.auties00.daedalus.cli.tui.StatusBar;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.generation.JavaSourceGenerator;
import com.github.auties00.daedalus.cli.tui.Theme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

/**
 * The TUI model for the generation flow.
 *
 * <p>Shows a spinner while generating Java source files from proto
 * schemas, then displays a summary of generated files with success/error status.
 */
public final class GenerateModel implements Model {
    private static final int MAX_LISTED_FILES = 12;

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
    private boolean back;
    private int spinnerFrame;
    private int termHeight;

    /**
     * A message indicating generation should start.
     */
    public record StartGenerationMessage() implements Message {
    }

    /**
     * A message indicating generation has completed.
     *
     * @param files the generated file paths
     */
    public record GenerationCompleteMessage(SequencedCollection<String> files) implements Message {
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
        this.back = false;
        this.spinnerFrame = 0;
        this.termHeight = 40;
    }

    @Override
    public Command init() {
        Command generate = () -> {
            try {
                var generator = new JavaSourceGenerator(config);
                var files = generator.generate();
                return new GenerationCompleteMessage(files);
            } catch (IOException | ProtobufParserException e) {
                return new GenerationErrorMessage(e.getMessage());
            }
        };
        return Command.batch(generate, Spinner.tick());
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage size) {
            termHeight = size.height();
            return UpdateResult.from(this);
        }
        if (msg instanceof Spinner.TickMessage) {
            if (state != State.GENERATING) {
                return UpdateResult.from(this);
            }
            spinnerFrame++;
            return UpdateResult.from(this, Spinner.tick());
        }
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
            if (keyStr.equals("esc")) {
                if (state == State.GENERATING) {
                    return UpdateResult.from(this);
                }
                back = true;
                return UpdateResult.from(this, Command.quit());
            }
            if (keyStr.equals("q") || keyStr.equals("enter") || keyStr.equals("ctrl+c")) {
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
        return Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Working")
                .result(Spinner.frame(spinnerFrame), "Generating Java source files")
                .entry("Proto files", String.join(", ", config.protoFiles()))
                .entry("Output directory", config.javaSourceDir())
                .render();
    }

    private String viewDone() {
        var count = generatedFiles.size();
        var screen = Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Done")
                .result(Theme.SUCCESS_TEXT.render("✓"),
                        "Generated " + count + " file" + (count != 1 ? "s" : ""));

        if (count > 0) {
            screen.block(Screen.panel("Generated files", renderFiles())).blank();
        }

        return screen
                .entry("Output directory", config.javaSourceDir())
                .keys(new StatusBar()
                        .add("enter", "exit")
                        .add("esc", "back")
                        .add("q", "quit"))
                .render();
    }

    private String renderFiles() {
        var sb = new StringBuilder();
        var listed = Math.min(generatedFiles.size(), MAX_LISTED_FILES);
        for (var i = 0; i < listed; i++) {
            sb.append(Theme.SUCCESS_TEXT.render("+ "));
            sb.append(Theme.BODY.render(generatedFiles.get(i)));
            sb.append('\n');
        }

        var hidden = generatedFiles.size() - listed;
        if (hidden > 0) {
            sb.append(Theme.MUTED_TEXT.render("and " + hidden + " more"));
            sb.append('\n');
        }

        return sb.toString().stripTrailing();
    }

    private String viewError() {
        var message = errorMessage != null ? errorMessage : "Unknown error";
        return Screen.of("Protobuf", "Generate", termHeight)
                .indicator("Failed")
                .result(Theme.DANGER_TEXT.render("✗"), "Generation failed")
                .block(Screen.panel("Details", Theme.BODY.render(message))).blank()
                .entry("Proto files", String.join(", ", config.protoFiles()))
                .entry("Output directory", config.javaSourceDir())
                .keys(new StatusBar()
                        .add("enter", "exit")
                        .add("esc", "back")
                        .add("q", "quit"))
                .render();
    }

    /**
     * Returns whether the user has quit the generation view.
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

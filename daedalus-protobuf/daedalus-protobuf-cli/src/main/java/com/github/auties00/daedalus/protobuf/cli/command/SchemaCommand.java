package com.github.auties00.daedalus.protobuf.cli.command;

import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.ProgramOption;
import com.github.auties00.daedalus.protobuf.cli.tui.SchemaApp;
import picocli.CommandLine.Command;

import java.nio.file.Path;

/**
 * Root command for the protobuf schema CLI tool.
 *
 * <p>When invoked without subcommands, launches a full-screen TUI with
 * a welcome screen showing a large ASCII art "PROTOBUF" title and a
 * mode selector for generation or update workflows. All screens run
 * inside a single {@link Program} instance to avoid terminal state issues.
 *
 * <p>Subcommands ({@code generate}, {@code update}) are available for
 * non-interactive or CI usage.
 */
@Command(
        name = "protobuf-schema",
        description = "Generate and update Java models from .proto schemas",
        mixinStandardHelpOptions = true,
        version = "4.0.0",
        subcommands = {
                GenerateCommand.class,
                UpdateCommand.class
        }
)
public final class SchemaCommand implements Runnable {
    @Override
    public void run() {
        var workingDir = Path.of(System.getProperty("user.dir"));
        var app = new SchemaApp(workingDir);
        new Program(app, ProgramOption.withAltScreen()).run();
    }
}

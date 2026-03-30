package com.github.auties00.daedalus.cli.spi;

import com.williamcallahan.tui4j.compat.bubbletea.Model;

import java.nio.file.Path;

/**
 * A service provider interface for Daedalus CLI extensions.
 *
 * <p>Each extension (such as protobuf or JSON support) implements this
 * interface and registers itself via the {@link java.util.ServiceLoader}
 * mechanism. The central CLI discovers all available extensions at startup
 * and integrates their commands and TUI models into the application.
 *
 * <p>Extensions are registered in {@code module-info.java} using the
 * {@code provides} directive:
 *
 * <pre>{@code
 *     provides DaedalusExtension with MyExtension;
 * }</pre>
 */
public interface DaedalusExtension {
    /**
     * Returns the unique name of this extension.
     *
     * <p>This name is used as the picocli subcommand name. For example,
     * an extension returning {@code "protobuf"} would be invoked as
     * {@code daedalus protobuf generate}.
     *
     * @return the extension name, never {@code null}
     */
    String name();

    /**
     * Returns a short human-readable description of this extension.
     *
     * <p>This description appears in the CLI help output and in the
     * TUI welcome screen.
     *
     * @return the extension description, never {@code null}
     */
    String description();

    /**
     * Returns the picocli command object for this extension.
     *
     * <p>The returned object must be annotated with
     * {@link picocli.CommandLine.Command} and is registered as a subcommand
     * of the root Daedalus command. It may itself declare further subcommands.
     *
     * @return the picocli command object, never {@code null}
     */
    Object command();

    /**
     * Creates the TUI model for this extension's interactive mode.
     *
     * <p>The returned model takes full control of the terminal when the
     * user selects this extension from the welcome screen or invokes it
     * without subcommand arguments.
     *
     * @param workingDir the current working directory
     * @return the root TUI model for this extension, never {@code null}
     */
    Model createTuiModel(Path workingDir);
}

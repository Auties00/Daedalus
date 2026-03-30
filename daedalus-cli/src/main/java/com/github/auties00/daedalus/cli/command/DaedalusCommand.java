package com.github.auties00.daedalus.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The root command for the Daedalus CLI tool.
 *
 * <p>This command serves as the parent for all extension subcommands
 * (such as {@code protobuf} and {@code json}). When invoked without
 * a subcommand, the main entry point launches the interactive TUI
 * instead of executing this command directly.
 */
@Command(
        name = "daedalus",
        description = "Daedalus - a modular code generation toolkit",
        mixinStandardHelpOptions = true,
        version = "4.0.0"
)
public final class DaedalusCommand implements Runnable {
    @Override
    public void run() {
        var cmd = new CommandLine(this);
        cmd.usage(System.out);
    }
}

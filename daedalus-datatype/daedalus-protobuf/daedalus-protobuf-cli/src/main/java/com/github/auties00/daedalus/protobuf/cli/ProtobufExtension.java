package com.github.auties00.daedalus.protobuf.cli;

import com.github.auties00.daedalus.cli.spi.DaedalusExtension;
import com.github.auties00.daedalus.protobuf.cli.command.SchemaCommand;
import com.github.auties00.daedalus.protobuf.cli.tui.SchemaApp;
import com.williamcallahan.tui4j.compat.bubbletea.Model;

import java.nio.file.Path;

/**
 * The Daedalus extension for Protocol Buffers.
 *
 * <p>Registers the protobuf schema CLI commands ({@code generate} and
 * {@code update}) and provides the interactive TUI for protobuf workflows.
 */
public final class ProtobufExtension implements DaedalusExtension {
    @Override
    public String name() {
        return "protobuf";
    }

    @Override
    public String description() {
        return "Generate and update Java models from .proto schemas";
    }

    @Override
    public Object command() {
        return new SchemaCommand();
    }

    @Override
    public Model createTuiModel(Path workingDir) {
        return new SchemaApp(workingDir);
    }
}

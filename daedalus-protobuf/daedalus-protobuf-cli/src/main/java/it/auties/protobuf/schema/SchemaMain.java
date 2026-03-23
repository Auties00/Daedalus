import it.auties.protobuf.schema.cli.SchemaCommand;
import picocli.CommandLine;

void main(String... args) {
    var command = new SchemaCommand();
    var commandLine = new CommandLine(command);

    if (args.length == 0) {
        command.run();
    } else {
        var exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
import com.github.auties00.daedalus.cli.command.DaedalusCommand;
import com.github.auties00.daedalus.cli.spi.DaedalusExtension;
import com.github.auties00.daedalus.cli.tui.DaedalusApp;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.ProgramOption;
import picocli.CommandLine;

void main(String[] args) {
    var extensions = ServiceLoader.load(DaedalusExtension.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList();

    var rootCommand = new DaedalusCommand();
    var commandLine = new CommandLine(rootCommand);

    for (var extension : extensions) {
        commandLine.addSubcommand(extension.name(), extension.command());
    }

    if (args.length == 0) {
        var workingDir = Path.of(System.getProperty("user.dir"));
        var app = new DaedalusApp(extensions, workingDir);
        new Program(app, ProgramOption.withAltScreen()).run();
    } else {
        var exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
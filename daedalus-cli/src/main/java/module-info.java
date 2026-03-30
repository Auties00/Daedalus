module com.github.auties00.daedalus.cli {
    requires transitive info.picocli;
    requires transitive tui4j;

    uses com.github.auties00.daedalus.cli.spi.DaedalusExtension;

    exports com.github.auties00.daedalus.cli.spi;
    exports com.github.auties00.daedalus.cli.tui;

    opens com.github.auties00.daedalus.cli.command to info.picocli;
}
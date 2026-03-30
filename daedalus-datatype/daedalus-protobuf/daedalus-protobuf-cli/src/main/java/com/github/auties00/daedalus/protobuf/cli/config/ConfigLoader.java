package com.github.auties00.daedalus.protobuf.cli.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads and saves {@link SchemaConfig} from and to YAML files.
 *
 * <p>Looks for {@code .protobuf-schema.yml} in the working directory.
 * If the file does not exist, returns an empty optional to signal that
 * the onboarding wizard should be launched.
 */
public final class ConfigLoader {
    private static final String CONFIG_FILE_NAME = ".protobuf-schema.yml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    );

    private ConfigLoader() {
    }

    /**
     * Loads the configuration from the default config file in the given directory.
     *
     * @param workingDir the directory to search for the config file
     * @return the loaded config, or empty if the config file does not exist
     * @throws IOException if the file exists but cannot be read or parsed
     */
    public static Optional<SchemaConfig> load(Path workingDir) throws IOException {
        var configPath = workingDir.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }
        return Optional.of(YAML_MAPPER.readValue(configPath.toFile(), SchemaConfig.class));
    }

    /**
     * Loads the configuration from a specific file path.
     *
     * @param configPath the path to the config file
     * @return the loaded config
     * @throws IOException if the file cannot be read or parsed
     */
    public static SchemaConfig loadFrom(Path configPath) throws IOException {
        return YAML_MAPPER.readValue(configPath.toFile(), SchemaConfig.class);
    }

    /**
     * Saves the configuration to the default config file in the given directory.
     *
     * @param config the configuration to save
     * @param workingDir the directory to write the config file in
     * @throws IOException if the file cannot be written
     */
    public static void save(SchemaConfig config, Path workingDir) throws IOException {
        var configPath = workingDir.resolve(CONFIG_FILE_NAME);
        YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
    }
}

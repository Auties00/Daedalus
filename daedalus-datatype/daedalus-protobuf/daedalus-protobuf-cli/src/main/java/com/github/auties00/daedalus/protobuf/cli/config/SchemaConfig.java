package com.github.auties00.daedalus.protobuf.cli.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.auties00.daedalus.protobuf.cli.model.EnumConstantMode;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root configuration model for the protobuf schema generator and updater.
 *
 * <p>Serialized to and deserialized from {@code .protobuf-schema.yml}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaConfig {
    @JsonProperty("protoDir")
    private String protoDir = "proto";

    @JsonProperty("javaSourceDir")
    private String javaSourceDir = "./src/main/java";

    @JsonProperty("defaults")
    private Defaults defaults = new Defaults();

    @JsonProperty("naming")
    private Naming naming = new Naming();

    @JsonProperty("packageMappings")
    private Map<String, String> packageMappings = new LinkedHashMap<>();

    @JsonProperty("typeOverrides")
    private Map<String, TypeOverride> typeOverrides = new LinkedHashMap<>();

    /**
     * Returns the directory containing {@code .proto} files.
     *
     * @return the proto source directory path
     */
    public String protoDir() {
        return protoDir;
    }

    /**
     * Sets the directory containing {@code .proto} files.
     *
     * @param protoDir the proto source directory path
     */
    public void setProtoDir(String protoDir) {
        this.protoDir = protoDir;
    }

    /**
     * Returns the directory where generated Java files are written.
     *
     * @return the Java output directory path
     */
    public String javaSourceDir() {
        return javaSourceDir;
    }

    /**
     * Sets the directory where generated Java files are written.
     *
     * @param javaSourceDir the Java output directory path
     */
    public void setJavaSourceDir(String javaSourceDir) {
        this.javaSourceDir = javaSourceDir;
    }

    /**
     * Returns the default generation settings.
     *
     * @return the defaults configuration
     */
    public Defaults defaults() {
        return defaults;
    }

    /**
     * Returns the naming convention settings.
     *
     * @return the naming configuration
     */
    public Naming naming() {
        return naming;
    }

    /**
     * Returns the proto package to Java package mappings.
     *
     * @return an ordered map from proto package names to Java package names
     */
    public Map<String, String> packageMappings() {
        return packageMappings;
    }

    /**
     * Returns per-type configuration overrides keyed by proto fully-qualified name.
     *
     * @return an ordered map from proto FQN to override configuration
     */
    public Map<String, TypeOverride> typeOverrides() {
        return typeOverrides;
    }

    /**
     * Resolves the Java package for a given proto package name.
     *
     * <p>Looks up the package in {@link #packageMappings()}, falling back to
     * the proto package itself if no mapping is defined. The empty-string key
     * serves as the default mapping for unmapped packages.
     *
     * @param protoPackage the proto package name, or {@code null} for no package
     * @return the Java package name
     */
    public String resolveJavaPackage(String protoPackage) {
        if (protoPackage != null && packageMappings.containsKey(protoPackage)) {
            return packageMappings.get(protoPackage);
        }
        if (packageMappings.containsKey("")) {
            return packageMappings.get("");
        }
        return protoPackage != null ? protoPackage : "";
    }

    /**
     * Default generation settings applied when no per-type override is specified.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Defaults {
        @JsonProperty("typeForm")
        private TypeForm typeForm = TypeForm.RECORD;

        @JsonProperty("enumForm")
        private EnumForm enumForm = EnumForm.JAVA_ENUM;

        @JsonProperty("enumConstantMode")
        private EnumConstantMode enumConstantMode = EnumConstantMode.CONSTANT_ANNOTATION;

        @JsonProperty("generateBuilders")
        private boolean generateBuilders = true;

        @JsonProperty("includeUnknownFields")
        private boolean includeUnknownFields = false;

        @JsonProperty("generateDefaultValue")
        private boolean generateDefaultValue = true;

        /**
         * Returns the default type form for messages and groups.
         *
         * @return the default type form
         */
        public TypeForm typeForm() {
            return typeForm;
        }

        /**
         * Sets the default type form for messages and groups.
         *
         * @param typeForm the type form to set
         */
        public void setTypeForm(TypeForm typeForm) {
            this.typeForm = typeForm;
        }

        /**
         * Returns the default enum form.
         *
         * @return the default enum form
         */
        public EnumForm enumForm() {
            return enumForm;
        }

        /**
         * Sets the default enum form.
         *
         * @param enumForm the enum form to set
         */
        public void setEnumForm(EnumForm enumForm) {
            this.enumForm = enumForm;
        }

        /**
         * Returns the default enum constant mode.
         *
         * @return the default enum constant mode
         */
        public EnumConstantMode enumConstantMode() {
            return enumConstantMode;
        }

        /**
         * Sets the default enum constant mode.
         *
         * @param enumConstantMode the enum constant mode to set
         */
        public void setEnumConstantMode(EnumConstantMode enumConstantMode) {
            this.enumConstantMode = enumConstantMode;
        }

        /**
         * Returns whether builder classes should be generated.
         *
         * @return {@code true} if builders should be generated
         */
        public boolean generateBuilders() {
            return generateBuilders;
        }

        /**
         * Sets whether builder classes should be generated.
         *
         * @param generateBuilders whether to generate builders
         */
        public void setGenerateBuilders(boolean generateBuilders) {
            this.generateBuilders = generateBuilders;
        }

        /**
         * Returns whether an {@code @ProtobufUnknownFields} field should be included.
         *
         * @return {@code true} if unknown fields should be included
         */
        public boolean includeUnknownFields() {
            return includeUnknownFields;
        }

        /**
         * Sets whether an {@code @ProtobufUnknownFields} field should be included.
         *
         * @param includeUnknownFields whether to include unknown fields
         */
        public void setIncludeUnknownFields(boolean includeUnknownFields) {
            this.includeUnknownFields = includeUnknownFields;
        }

        /**
         * Returns whether {@code @ProtobufDefaultValue} should be generated.
         *
         * @return {@code true} if default values should be generated
         */
        public boolean generateDefaultValue() {
            return generateDefaultValue;
        }

        /**
         * Sets whether {@code @ProtobufDefaultValue} should be generated.
         *
         * @param generateDefaultValue whether to generate default values
         */
        public void setGenerateDefaultValue(boolean generateDefaultValue) {
            this.generateDefaultValue = generateDefaultValue;
        }
    }

    /**
     * Naming convention settings for converting proto names to Java names.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Naming {
        @JsonProperty("typeCase")
        private String typeCase = "PASCAL_CASE";

        @JsonProperty("fieldCase")
        private String fieldCase = "CAMEL_CASE";

        @JsonProperty("enumConstantCase")
        private String enumConstantCase = "UPPER_SNAKE_CASE";

        /**
         * Returns the case convention for type names.
         *
         * @return the type case convention
         */
        public String typeCase() {
            return typeCase;
        }

        /**
         * Returns the case convention for field names.
         *
         * @return the field case convention
         */
        public String fieldCase() {
            return fieldCase;
        }

        /**
         * Returns the case convention for enum constant names.
         *
         * @return the enum constant case convention
         */
        public String enumConstantCase() {
            return enumConstantCase;
        }
    }

    /**
     * Per-type configuration override, keyed by proto fully-qualified name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeOverride {
        @JsonProperty("typeForm")
        private TypeForm typeForm;

        @JsonProperty("enumForm")
        private EnumForm enumForm;

        @JsonProperty("enumConstantMode")
        private EnumConstantMode enumConstantMode;

        @JsonProperty("includeUnknownFields")
        private Boolean includeUnknownFields;

        /**
         * Returns the overridden type form, or {@code null} if not overridden.
         *
         * @return the type form override, or {@code null}
         */
        public TypeForm typeForm() {
            return typeForm;
        }

        /**
         * Returns the overridden enum form, or {@code null} if not overridden.
         *
         * @return the enum form override, or {@code null}
         */
        public EnumForm enumForm() {
            return enumForm;
        }

        /**
         * Returns the overridden enum constant mode, or {@code null} if not overridden.
         *
         * @return the enum constant mode override, or {@code null}
         */
        public EnumConstantMode enumConstantMode() {
            return enumConstantMode;
        }

        /**
         * Returns the overridden unknown fields setting, or {@code null} if not overridden.
         *
         * @return the unknown fields override, or {@code null}
         */
        public Boolean includeUnknownFields() {
            return includeUnknownFields;
        }
    }
}

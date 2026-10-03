package com.google.protobuf.compiler;

import com.google.protobuf.DescriptorProtos;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SequencedCollection;

/**
 * Holder of the Daedalus models declared by {@code google/protobuf/compiler/plugin.proto}.
 *
 * <p>These models represent the protocol compiler plugin messages and enums
 * exchanged between {@code protoc} and its code generator plugins, such as
 * {@link PluginProtos.CodeGeneratorRequest}.
 *
 * <p>The models mirror the {@code proto2} definitions of the official protobuf
 * distribution field by field, including field numbers, wire types, field
 * presence and repeated field encodings. This class only scopes the nested
 * types and cannot be instantiated.
 */
public final class PluginProtos {
    /**
     * Prevents the instantiation of this holder class.
     */
    private PluginProtos() {}

    /**
     * The version number of protocol compiler.
     *
     * <p>Protobuf type {@code google.protobuf.compiler.Version}
     */
    @ProtobufMessage
    public static final class Version {

        /**
         * <p><code>int32 major = 1;</code>
         */
        @ProtobufMessage.Int32Field(index = 1)
        Integer major;

        /**
         * <p><code>int32 minor = 2;</code>
         */
        @ProtobufMessage.Int32Field(index = 2)
        Integer minor;

        /**
         * <p><code>int32 patch = 3;</code>
         */
        @ProtobufMessage.Int32Field(index = 3)
        Integer patch;

        /**
         * A suffix for alpha, beta or rc release, e.g., "alpha-1", "rc2". It should
         * be empty for mainline stable releases.
         *
         * <p><code>string suffix = 4;</code>
         */
        @ProtobufMessage.StringField(index = 4)
        String suffix;

        /**
         * Constructs a new {@code Version} with the specified field values.
         *
         * @param major the value of the {@code major} field, or {@code null} if the
         *              field is not set
         * @param minor the value of the {@code minor} field, or {@code null} if the
         *              field is not set
         * @param patch the value of the {@code patch} field, or {@code null} if the
         *              field is not set
         * @param suffix the value of the {@code suffix} field, or {@code null} if the
         *               field is not set
         */
        Version(Integer major, Integer minor, Integer patch, String suffix) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.suffix = suffix;
        }

        /**
         * Returns the value of the {@code major} field, if present.
         *
         * @return an {@code OptionalInt} containing the value of the {@code major}
         *         field, or an empty {@code OptionalInt} if the field is not set
         */
        public OptionalInt major() {
            return major == null ? OptionalInt.empty() : OptionalInt.of(major);
        }

        /**
         * Returns the value of the {@code minor} field, if present.
         *
         * @return an {@code OptionalInt} containing the value of the {@code minor}
         *         field, or an empty {@code OptionalInt} if the field is not set
         */
        public OptionalInt minor() {
            return minor == null ? OptionalInt.empty() : OptionalInt.of(minor);
        }

        /**
         * Returns the value of the {@code patch} field, if present.
         *
         * @return an {@code OptionalInt} containing the value of the {@code patch}
         *         field, or an empty {@code OptionalInt} if the field is not set
         */
        public OptionalInt patch() {
            return patch == null ? OptionalInt.empty() : OptionalInt.of(patch);
        }

        /**
         * Returns the value of the {@code suffix} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code suffix}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<String> suffix() {
            return Optional.ofNullable(suffix);
        }
    }

    /**
     * An encoded CodeGeneratorRequest is written to the plugin's stdin.
     *
     * <p>Protobuf type {@code google.protobuf.compiler.CodeGeneratorRequest}
     */
    @ProtobufMessage
    public static final class CodeGeneratorRequest {

        /**
         * The .proto files that were explicitly listed on the command-line.  The
         * code generator should generate code only for these files.  Each file's
         * descriptor will be included in proto_file, below.
         *
         * <p><code>repeated string file_to_generate = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        List<String> fileToGenerate;

        /**
         * The generator parameter passed on the command-line.
         *
         * <p><code>string parameter = 2;</code>
         */
        @ProtobufMessage.StringField(index = 2)
        String parameter;

        /**
         * The version number of protocol compiler.
         *
         * <p><code>Version compiler_version = 3;</code>
         */
        @ProtobufMessage.MessageField(index = 3)
        PluginProtos.Version compilerVersion;

        /**
         * FileDescriptorProtos for all files in files_to_generate and everything
         * they import.  The files will appear in topological order, so each file
         * appears before any file that imports it.
         *
         * <p>Note: the files listed in files_to_generate will include runtime-retention
         * options only, but all other files will include source-retention options.
         * The source_file_descriptors field below is available in case you need
         * source-retention options for files_to_generate.
         *
         * <p>protoc guarantees that all proto_files will be written after
         * the fields above, even though this is not technically guaranteed by the
         * protobuf wire format.  This theoretically could allow a plugin to stream
         * in the FileDescriptorProtos and handle them one by one rather than read
         * the entire set into memory at once.  However, as of this writing, this
         * is not similarly optimized on protoc's end -- it will store all fields in
         * memory at once before sending them to the plugin.
         *
         * <p>Type names of fields and extensions in the FileDescriptorProto are always
         * fully qualified.
         *
         * <p><code>repeated FileDescriptorProto proto_file = 15;</code>
         */
        @ProtobufMessage.MessageField(index = 15)
        List<DescriptorProtos.FileDescriptorProto> protoFile;

        /**
         * File descriptors with all options, including source-retention options.
         * These descriptors are only provided for the files listed in
         * files_to_generate.
         *
         * <p><code>repeated FileDescriptorProto source_file_descriptors = 17;</code>
         */
        @ProtobufMessage.MessageField(index = 17)
        List<DescriptorProtos.FileDescriptorProto> sourceFileDescriptors;

        /**
         * Constructs a new {@code CodeGeneratorRequest} with the specified field values.
         *
         * @param fileToGenerate the values of the repeated {@code file_to_generate} field
         * @param parameter the value of the {@code parameter} field, or {@code null} if the
         *                  field is not set
         * @param compilerVersion the value of the {@code compiler_version} field, or {@code null} if the
         *                        field is not set
         * @param protoFile the values of the repeated {@code proto_file} field
         * @param sourceFileDescriptors the values of the repeated {@code source_file_descriptors} field
         */
        CodeGeneratorRequest(
                List<String> fileToGenerate,
                String parameter,
                PluginProtos.Version compilerVersion,
                List<DescriptorProtos.FileDescriptorProto> protoFile,
                List<DescriptorProtos.FileDescriptorProto> sourceFileDescriptors
        ) {
            this.fileToGenerate = fileToGenerate;
            this.parameter = parameter;
            this.compilerVersion = compilerVersion;
            this.protoFile = protoFile;
            this.sourceFileDescriptors = sourceFileDescriptors;
        }

        /**
         * Returns an unmodifiable view of the values of the repeated
         * {@code file_to_generate} field.
         *
         * @return an unmodifiable view of the values of the {@code file_to_generate} field,
         *         in encounter order
         */
        public SequencedCollection<String> fileToGenerate() {
            return Collections.unmodifiableSequencedCollection(fileToGenerate);
        }

        /**
         * Returns the value of the {@code parameter} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code parameter}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<String> parameter() {
            return Optional.ofNullable(parameter);
        }

        /**
         * Returns an unmodifiable view of the values of the repeated
         * {@code proto_file} field.
         *
         * @return an unmodifiable view of the values of the {@code proto_file} field,
         *         in encounter order
         */
        public SequencedCollection<DescriptorProtos.FileDescriptorProto> protoFile() {
            return Collections.unmodifiableSequencedCollection(protoFile);
        }

        /**
         * Returns an unmodifiable view of the values of the repeated
         * {@code source_file_descriptors} field.
         *
         * @return an unmodifiable view of the values of the {@code source_file_descriptors} field,
         *         in encounter order
         */
        public SequencedCollection<DescriptorProtos.FileDescriptorProto> sourceFileDescriptors() {
            return Collections.unmodifiableSequencedCollection(sourceFileDescriptors);
        }

        /**
         * Returns the value of the {@code compiler_version} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code compiler_version}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<PluginProtos.Version> compilerVersion() {
            return Optional.ofNullable(compilerVersion);
        }
    }

    /**
     * The plugin writes an encoded CodeGeneratorResponse to stdout.
     *
     * <p>Protobuf type {@code google.protobuf.compiler.CodeGeneratorResponse}
     */
    @ProtobufMessage
    public static final class CodeGeneratorResponse {

        /**
         * Sync with code_generator.h.
         *
         * <p>Protobuf enum {@code google.protobuf.compiler.CodeGeneratorResponse.Feature}
         */
        @ProtobufEnum
        public enum Feature {
            @ProtobufEnum.Constant(index = 0)
            FEATURE_NONE,

            @ProtobufEnum.Constant(index = 1)
            FEATURE_PROTO3_OPTIONAL,

            @ProtobufEnum.Constant(index = 2)
            FEATURE_SUPPORTS_EDITIONS;
        }

        /**
         * Represents a single generated file.
         *
         * <p>Protobuf type {@code google.protobuf.compiler.CodeGeneratorResponse.File}
         */
        @ProtobufMessage
        public static final class File {

            /**
             * The file name, relative to the output directory.  The name must not
             * contain "." or ".." components and must be relative, not be absolute (so,
             * the file cannot lie outside the output directory).  "/" must be used as
             * the path separator, not "\".
             *
             * <p>If the name is omitted, the content will be appended to the previous
             * file.  This allows the generator to break large files into small chunks,
             * and allows the generated text to be streamed back to protoc so that large
             * files need not reside completely in memory at one time.  Note that as of
             * this writing protoc does not optimize for this -- it will read the entire
             * CodeGeneratorResponse before writing files to disk.
             *
             * <p><code>string name = 1;</code>
             */
            @ProtobufMessage.StringField(index = 1)
            String name;

            /**
             * If non-empty, indicates that the named file should already exist, and the
             * content here is to be inserted into that file at a defined insertion
             * point.  This feature allows a code generator to extend the output
             * produced by another code generator.  The original generator may provide
             * insertion points by placing special annotations in the file that look
             * like:
             *
             * <pre>
             * @@protoc_insertion_point(NAME)
             * </pre>
             *
             * <p>The annotation can have arbitrary text before and after it on the line,
             * which allows it to be placed in a comment.  NAME should be replaced with
             * an identifier naming the point -- this is what other generators will use
             * as the insertion_point.  Code inserted at this point will be placed
             * immediately above the line containing the insertion point (thus multiple
             * insertions to the same point will come out in the order they were added).
             * The double-@ is intended to make it unlikely that the generated code
             * could contain things that look like insertion points by accident.
             *
             * <p>For example, the C++ code generator places the following line in the
             * .pb.h files that it generates:
             *
             * <pre>
             * // @@protoc_insertion_point(namespace_scope)
             * </pre>
             *
             * <p>This line appears within the scope of the file's package namespace, but
             * outside of any particular class.  Another plugin can then specify the
             * insertion_point "namespace_scope" to generate additional classes or
             * other declarations that should be placed in this scope.
             *
             * <p>Note that if the line containing the insertion point begins with
             * whitespace, the same whitespace will be added to every line of the
             * inserted text.  This is useful for languages like Python, where
             * indentation matters.  In these languages, the insertion point comment
             * should be indented the same amount as any inserted code will need to be
             * in order to work correctly in that context.
             *
             * <p>The code generator that generates the initial file and the one which
             * inserts into it must both run as part of a single invocation of protoc.
             * Code generators are executed in the order in which they appear on the
             * command line.
             *
             * <p>If |insertion_point| is present, |name| must also be present.
             *
             * <p><code>string insertion_point = 2;</code>
             */
            @ProtobufMessage.StringField(index = 2)
            String insertionPoint;

            /**
             * The file contents.
             *
             * <p><code>string content = 15;</code>
             */
            @ProtobufMessage.StringField(index = 15)
            String content;

            /**
             * Information describing the file content being inserted. If an insertion
             * point is used, this information will be appropriately offset and inserted
             * into the code generation metadata for the generated files.
             *
             * <p><code>GeneratedCodeInfo generated_code_info = 16;</code>
             */
            @ProtobufMessage.MessageField(index = 16)
            DescriptorProtos.GeneratedCodeInfo generatedCodeInfo;

            /**
             * Constructs a new {@code File} with the specified field values.
             *
             * @param name the value of the {@code name} field, or {@code null} if the
             *             field is not set
             * @param insertionPoint the value of the {@code insertion_point} field, or {@code null} if the
             *                       field is not set
             * @param content the value of the {@code content} field, or {@code null} if the
             *                field is not set
             * @param generatedCodeInfo the value of the {@code generated_code_info} field, or {@code null} if the
             *                          field is not set
             */
            File(
                    String name,
                    String insertionPoint,
                    String content,
                    DescriptorProtos.GeneratedCodeInfo generatedCodeInfo
            ) {
                this.name = name;
                this.insertionPoint = insertionPoint;
                this.content = content;
                this.generatedCodeInfo = generatedCodeInfo;
            }

            /**
             * Returns the value of the {@code name} field, if present.
             *
             * @return an {@code Optional} containing the value of the {@code name}
             *         field, or an empty {@code Optional} if the field is not set
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the value of the {@code insertion_point} field, if present.
             *
             * @return an {@code Optional} containing the value of the {@code insertion_point}
             *         field, or an empty {@code Optional} if the field is not set
             */
            public Optional<String> insertionPoint() {
                return Optional.ofNullable(insertionPoint);
            }

            /**
             * Returns the value of the {@code content} field, if present.
             *
             * @return an {@code Optional} containing the value of the {@code content}
             *         field, or an empty {@code Optional} if the field is not set
             */
            public Optional<String> content() {
                return Optional.ofNullable(content);
            }

            /**
             * Returns the value of the {@code generated_code_info} field, if present.
             *
             * @return an {@code Optional} containing the value of the {@code generated_code_info}
             *         field, or an empty {@code Optional} if the field is not set
             */
            public Optional<DescriptorProtos.GeneratedCodeInfo> generatedCodeInfo() {
                return Optional.ofNullable(generatedCodeInfo);
            }
        }

        /**
         * Error message.  If non-empty, code generation failed.  The plugin process
         * should exit with status code zero even if it reports an error in this way.
         *
         * <p>This should be used to indicate errors in .proto files which prevent the
         * code generator from generating correct code.  Errors which indicate a
         * problem in protoc itself -- such as the input CodeGeneratorRequest being
         * unparseable -- should be reported by writing a message to stderr and
         * exiting with a non-zero status code.
         *
         * <p><code>string error = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String error;

        /**
         * A bitmask of supported features that the code generator supports.
         * This is a bitwise "or" of values from the Feature enum.
         *
         * <p><code>uint64 supported_features = 2;</code>
         */
        @ProtobufMessage.Uint64Field(index = 2)
        Long supportedFeatures;

        /**
         * The minimum edition this plugin supports.  This will be treated as an
         * Edition enum, but we want to allow unknown values.  It should be specified
         * according the edition enum value, *not* the edition number.  Only takes
         * effect for plugins that have FEATURE_SUPPORTS_EDITIONS set.
         *
         * <p><code>int32 minimum_edition = 3;</code>
         */
        @ProtobufMessage.Int32Field(index = 3)
        Integer minimumEdition;

        /**
         * The maximum edition this plugin supports.  This will be treated as an
         * Edition enum, but we want to allow unknown values.  It should be specified
         * according the edition enum value, *not* the edition number.  Only takes
         * effect for plugins that have FEATURE_SUPPORTS_EDITIONS set.
         *
         * <p><code>int32 maximum_edition = 4;</code>
         */
        @ProtobufMessage.Int32Field(index = 4)
        Integer maximumEdition;

        /**
         * <p><code>repeated File file = 15;</code>
         */
        @ProtobufMessage.MessageField(index = 15)
        List<PluginProtos.CodeGeneratorResponse.File> file;

        /**
         * Constructs a new {@code CodeGeneratorResponse} with the specified field values.
         *
         * @param error the value of the {@code error} field, or {@code null} if the
         *              field is not set
         * @param supportedFeatures the value of the {@code supported_features} field, or {@code null} if the
         *                          field is not set
         * @param minimumEdition the value of the {@code minimum_edition} field, or {@code null} if the
         *                       field is not set
         * @param maximumEdition the value of the {@code maximum_edition} field, or {@code null} if the
         *                       field is not set
         * @param file the values of the repeated {@code file} field
         */
        CodeGeneratorResponse(
                String error,
                Long supportedFeatures,
                Integer minimumEdition,
                Integer maximumEdition,
                List<PluginProtos.CodeGeneratorResponse.File> file
        ) {
            this.error = error;
            this.supportedFeatures = supportedFeatures;
            this.minimumEdition = minimumEdition;
            this.maximumEdition = maximumEdition;
            this.file = file;
        }

        /**
         * Returns the value of the {@code error} field, if present.
         *
         * @return an {@code Optional} containing the value of the {@code error}
         *         field, or an empty {@code Optional} if the field is not set
         */
        public Optional<String> error() {
            return Optional.ofNullable(error);
        }

        /**
         * Returns the value of the {@code supported_features} field, if present.
         *
         * @return an {@code OptionalLong} containing the value of the {@code supported_features}
         *         field, or an empty {@code OptionalLong} if the field is not set
         */
        public OptionalLong supportedFeatures() {
            return supportedFeatures == null ? OptionalLong.empty() : OptionalLong.of(supportedFeatures);
        }

        /**
         * Returns the value of the {@code minimum_edition} field, if present.
         *
         * @return an {@code OptionalInt} containing the value of the {@code minimum_edition}
         *         field, or an empty {@code OptionalInt} if the field is not set
         */
        public OptionalInt minimumEdition() {
            return minimumEdition == null ? OptionalInt.empty() : OptionalInt.of(minimumEdition);
        }

        /**
         * Returns the value of the {@code maximum_edition} field, if present.
         *
         * @return an {@code OptionalInt} containing the value of the {@code maximum_edition}
         *         field, or an empty {@code OptionalInt} if the field is not set
         */
        public OptionalInt maximumEdition() {
            return maximumEdition == null ? OptionalInt.empty() : OptionalInt.of(maximumEdition);
        }

        /**
         * Returns an unmodifiable view of the values of the repeated
         * {@code file} field.
         *
         * @return an unmodifiable view of the values of the {@code file} field,
         *         in encounter order
         */
        public SequencedCollection<PluginProtos.CodeGeneratorResponse.File> file() {
            return Collections.unmodifiableSequencedCollection(file);
        }
    }
}

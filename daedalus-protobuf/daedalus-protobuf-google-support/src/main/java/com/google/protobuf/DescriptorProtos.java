package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

public final class DescriptorProtos {
    private DescriptorProtos() {}

    /**
     * The full set of known editions.
     *
     * <p>Protobuf enum {@code google.protobuf.Edition}
     */
    @ProtobufEnum
    public enum Edition {
        /**
         * A placeholder for an unknown edition value.
         */
        @ProtobufEnum.Constant(index = 0)
        EDITION_UNKNOWN,

        /**
         * A placeholder edition for specifying default behaviors *before* a feature
         * was first introduced.  This is effectively an "infinite past".
         */
        @ProtobufEnum.Constant(index = 900)
        EDITION_LEGACY,

        /**
         * Legacy syntax "editions".  These pre-date editions, but behave much like
         * distinct editions.  These can't be used to specify the edition of proto
         * files, but feature definitions must supply proto2/proto3 defaults for
         * backwards compatibility.
         */
        @ProtobufEnum.Constant(index = 998)
        EDITION_PROTO2,

        @ProtobufEnum.Constant(index = 999)
        EDITION_PROTO3,

        /**
         * Editions that have been released.  The specific values are arbitrary and
         * should not be depended on, but they will always be time-ordered for easy
         * comparison.
         */
        @ProtobufEnum.Constant(index = 1000)
        EDITION_2023,

        @ProtobufEnum.Constant(index = 1001)
        EDITION_2024,

        /**
         * A placeholder edition for developing and testing unscheduled features.
         */
        @ProtobufEnum.Constant(index = 9999)
        EDITION_UNSTABLE,

        /**
         * Placeholder editions for testing feature resolution.  These should not be
         * used or relied on outside of tests.
         */
        @ProtobufEnum.Constant(index = 1)
        EDITION_1_TEST_ONLY,

        @ProtobufEnum.Constant(index = 2)
        EDITION_2_TEST_ONLY,

        @ProtobufEnum.Constant(index = 99997)
        EDITION_99997_TEST_ONLY,

        @ProtobufEnum.Constant(index = 99998)
        EDITION_99998_TEST_ONLY,

        @ProtobufEnum.Constant(index = 99999)
        EDITION_99999_TEST_ONLY,

        /**
         * Placeholder for specifying unbounded edition support.  This should only
         * ever be used by plugins that can expect to never require any changes to
         * support a new edition.
         */
        @ProtobufEnum.Constant(index = 2147483647)
        EDITION_MAX;
    }

    /**
     * Describes the 'visibility' of a symbol with respect to the proto import
     * system. Symbols can only be imported when the visibility rules do not prevent
     * it (ex: local symbols cannot be imported).  Visibility modifiers can only set
     * on {@code message} and {@code enum} as they are the only types available to be referenced
     * from other files.
     *
     * <p>Protobuf enum {@code google.protobuf.SymbolVisibility}
     */
    @ProtobufEnum
    public enum SymbolVisibility {
        @ProtobufEnum.Constant(index = 0)
        VISIBILITY_UNSET,

        @ProtobufEnum.Constant(index = 1)
        VISIBILITY_LOCAL,

        @ProtobufEnum.Constant(index = 2)
        VISIBILITY_EXPORT;
    }

    /**
     * The protocol compiler can output a FileDescriptorSet containing the .proto
     * files it parses.
     *
     * <p>Protobuf type {@code google.protobuf.FileDescriptorSet}
     */
    @ProtobufMessage
    public static final class FileDescriptorSet {
        // proto: extensions 536000000 [declaration = { number: 536000000 type: ".buf.descriptor.v1.FileDescriptorSetExtension" full_name: ".buf.descriptor.v1.buf_file_descriptor_set_extension" }]

        /**
         * <p><code>repeated FileDescriptorProto file = 1;</code>
         */
        @ProtobufMessage.MessageField(index = 1)
        List<DescriptorProtos.FileDescriptorProto> file;

        FileDescriptorSet(List<DescriptorProtos.FileDescriptorProto> file) {
            this.file = file;
        }

        public SequencedCollection<DescriptorProtos.FileDescriptorProto> file() {
            return Collections.unmodifiableSequencedCollection(file);
        }
    }

    /**
     * Describes a complete .proto file.
     *
     * <p>Protobuf type {@code google.protobuf.FileDescriptorProto}
     */
    @ProtobufMessage
    public static final class FileDescriptorProto {

        /**
         * file name, relative to root of source tree
         *
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * e.g. "foo", "foo.bar", etc.
         *
         * <p><code>string package = 2;</code>
         */
        @ProtobufMessage.StringField(index = 2)
        String package_;

        /**
         * Names of files imported by this file.
         *
         * <p><code>repeated string dependency = 3;</code>
         */
        @ProtobufMessage.StringField(index = 3)
        List<String> dependency;

        /**
         * Indexes of the public imported files in the dependency list above.
         *
         * <p><code>repeated int32 public_dependency = 10;</code>
         */
        @ProtobufMessage.Int32Field(index = 10)
        List<Integer> publicDependency;

        /**
         * Indexes of the weak imported files in the dependency list.
         * For Google-internal migration only. Do not use.
         *
         * <p><code>repeated int32 weak_dependency = 11;</code>
         */
        @ProtobufMessage.Int32Field(index = 11)
        List<Integer> weakDependency;

        /**
         * Names of files imported by this file purely for the purpose of providing
         * option extensions. These are excluded from the dependency list above.
         *
         * <p><code>repeated string option_dependency = 15;</code>
         */
        @ProtobufMessage.StringField(index = 15)
        List<String> optionDependency;

        /**
         * All top-level definitions in this file.
         *
         * <p><code>repeated DescriptorProto message_type = 4;</code>
         */
        @ProtobufMessage.MessageField(index = 4)
        List<DescriptorProtos.DescriptorProto> messageType;

        /**
         * <p><code>repeated EnumDescriptorProto enum_type = 5;</code>
         */
        @ProtobufMessage.MessageField(index = 5)
        List<DescriptorProtos.EnumDescriptorProto> enumType;

        /**
         * <p><code>repeated ServiceDescriptorProto service = 6;</code>
         */
        @ProtobufMessage.MessageField(index = 6)
        List<DescriptorProtos.ServiceDescriptorProto> service;

        /**
         * <p><code>repeated FieldDescriptorProto extension = 7;</code>
         */
        @ProtobufMessage.MessageField(index = 7)
        List<DescriptorProtos.FieldDescriptorProto> extension;

        /**
         * <p><code>FileOptions options = 8;</code>
         */
        @ProtobufMessage.MessageField(index = 8)
        DescriptorProtos.FileOptions options;

        /**
         * This field contains optional information about the original source code.
         * You may safely remove this entire field without harming runtime
         * functionality of the descriptors -- the information is needed only by
         * development tools.
         *
         * <p><code>SourceCodeInfo source_code_info = 9;</code>
         */
        @ProtobufMessage.MessageField(index = 9)
        DescriptorProtos.SourceCodeInfo sourceCodeInfo;

        /**
         * The syntax of the proto file.
         * The supported values are "proto2", "proto3", and "editions".
         *
         * <p>If {@code edition} is present, this value must be "editions".
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>string syntax = 12;</code>
         */
        @ProtobufMessage.StringField(index = 12)
        String syntax;

        /**
         * The edition of the proto file.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>Edition edition = 14;</code>
         */
        @ProtobufMessage.EnumField(index = 14)
        DescriptorProtos.Edition edition;

        FileDescriptorProto(
                String name,
                String package_,
                List<String> dependency,
                List<Integer> publicDependency,
                List<Integer> weakDependency,
                List<String> optionDependency,
                List<DescriptorProtos.DescriptorProto> messageType,
                List<DescriptorProtos.EnumDescriptorProto> enumType,
                List<DescriptorProtos.ServiceDescriptorProto> service,
                List<DescriptorProtos.FieldDescriptorProto> extension,
                DescriptorProtos.FileOptions options,
                DescriptorProtos.SourceCodeInfo sourceCodeInfo,
                String syntax,
                DescriptorProtos.Edition edition
        ) {
            this.name = name;
            this.package_ = package_;
            this.dependency = dependency;
            this.publicDependency = publicDependency;
            this.weakDependency = weakDependency;
            this.optionDependency = optionDependency;
            this.messageType = messageType;
            this.enumType = enumType;
            this.service = service;
            this.extension = extension;
            this.options = options;
            this.sourceCodeInfo = sourceCodeInfo;
            this.syntax = syntax;
            this.edition = edition;
        }

        public String name() {
            return name;
        }

        public String package_() {
            return package_;
        }

        public SequencedCollection<String> dependency() {
            return Collections.unmodifiableSequencedCollection(dependency);
        }

        public SequencedCollection<Integer> publicDependency() {
            return Collections.unmodifiableSequencedCollection(publicDependency);
        }

        public SequencedCollection<Integer> weakDependency() {
            return Collections.unmodifiableSequencedCollection(weakDependency);
        }

        public SequencedCollection<String> optionDependency() {
            return Collections.unmodifiableSequencedCollection(optionDependency);
        }

        public SequencedCollection<DescriptorProtos.DescriptorProto> messageType() {
            return Collections.unmodifiableSequencedCollection(messageType);
        }

        public SequencedCollection<DescriptorProtos.EnumDescriptorProto> enumType() {
            return Collections.unmodifiableSequencedCollection(enumType);
        }

        public SequencedCollection<DescriptorProtos.ServiceDescriptorProto> service() {
            return Collections.unmodifiableSequencedCollection(service);
        }

        public SequencedCollection<DescriptorProtos.FieldDescriptorProto> extension() {
            return Collections.unmodifiableSequencedCollection(extension);
        }

        public DescriptorProtos.FileOptions options() {
            return options;
        }

        public DescriptorProtos.SourceCodeInfo sourceCodeInfo() {
            return sourceCodeInfo;
        }

        public String syntax() {
            return syntax;
        }

        public DescriptorProtos.Edition edition() {
            return edition;
        }
    }

    /**
     * Describes a message type.
     *
     * <p>Protobuf type {@code google.protobuf.DescriptorProto}
     */
    @ProtobufMessage
    public static final class DescriptorProto {

        /**
         * <p>Protobuf type {@code google.protobuf.DescriptorProto.ExtensionRange}
         */
        @ProtobufMessage
        public static final class ExtensionRange {

            /**
             * Inclusive.
             *
             * <p><code>int32 start = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            int start;

            /**
             * Exclusive.
             *
             * <p><code>int32 end = 2;</code>
             */
            @ProtobufMessage.Int32Field(index = 2)
            int end;

            /**
             * <p><code>ExtensionRangeOptions options = 3;</code>
             */
            @ProtobufMessage.MessageField(index = 3)
            DescriptorProtos.ExtensionRangeOptions options;

            ExtensionRange(int start, int end, DescriptorProtos.ExtensionRangeOptions options) {
                this.start = start;
                this.end = end;
                this.options = options;
            }

            public int start() {
                return start;
            }

            public int end() {
                return end;
            }

            public DescriptorProtos.ExtensionRangeOptions options() {
                return options;
            }
        }

        /**
         * Range of reserved tag numbers. Reserved tag numbers may not be used by
         * fields or extension ranges in the same message. Reserved ranges may
         * not overlap.
         *
         * <p>Protobuf type {@code google.protobuf.DescriptorProto.ReservedRange}
         */
        @ProtobufMessage
        public static final class ReservedRange {

            /**
             * Inclusive.
             *
             * <p><code>int32 start = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            int start;

            /**
             * Exclusive.
             *
             * <p><code>int32 end = 2;</code>
             */
            @ProtobufMessage.Int32Field(index = 2)
            int end;

            ReservedRange(int start, int end) {
                this.start = start;
                this.end = end;
            }

            public int start() {
                return start;
            }

            public int end() {
                return end;
            }
        }

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>repeated FieldDescriptorProto field = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        List<DescriptorProtos.FieldDescriptorProto> field;

        /**
         * <p><code>repeated FieldDescriptorProto extension = 6;</code>
         */
        @ProtobufMessage.MessageField(index = 6)
        List<DescriptorProtos.FieldDescriptorProto> extension;

        /**
         * <p><code>repeated DescriptorProto nested_type = 3;</code>
         */
        @ProtobufMessage.MessageField(index = 3)
        List<DescriptorProtos.DescriptorProto> nestedType;

        /**
         * <p><code>repeated EnumDescriptorProto enum_type = 4;</code>
         */
        @ProtobufMessage.MessageField(index = 4)
        List<DescriptorProtos.EnumDescriptorProto> enumType;

        /**
         * <p><code>repeated ExtensionRange extension_range = 5;</code>
         */
        @ProtobufMessage.MessageField(index = 5)
        List<DescriptorProtos.DescriptorProto.ExtensionRange> extensionRange;

        /**
         * <p><code>repeated OneofDescriptorProto oneof_decl = 8;</code>
         */
        @ProtobufMessage.MessageField(index = 8)
        List<DescriptorProtos.OneofDescriptorProto> oneofDecl;

        /**
         * <p><code>MessageOptions options = 7;</code>
         */
        @ProtobufMessage.MessageField(index = 7)
        DescriptorProtos.MessageOptions options;

        /**
         * <p><code>repeated ReservedRange reserved_range = 9;</code>
         */
        @ProtobufMessage.MessageField(index = 9)
        List<DescriptorProtos.DescriptorProto.ReservedRange> reservedRange;

        /**
         * Reserved field names, which may not be used by fields in the same message.
         * A given name may only be reserved once.
         *
         * <p><code>repeated string reserved_name = 10;</code>
         */
        @ProtobufMessage.StringField(index = 10)
        List<String> reservedName;

        /**
         * Support for {@code export} and {@code local} keywords on enums.
         *
         * <p><code>SymbolVisibility visibility = 11;</code>
         */
        @ProtobufMessage.EnumField(index = 11)
        DescriptorProtos.SymbolVisibility visibility;

        DescriptorProto(
                String name,
                List<DescriptorProtos.FieldDescriptorProto> field,
                List<DescriptorProtos.FieldDescriptorProto> extension,
                List<DescriptorProtos.DescriptorProto> nestedType,
                List<DescriptorProtos.EnumDescriptorProto> enumType,
                List<DescriptorProtos.DescriptorProto.ExtensionRange> extensionRange,
                List<DescriptorProtos.OneofDescriptorProto> oneofDecl,
                DescriptorProtos.MessageOptions options,
                List<DescriptorProtos.DescriptorProto.ReservedRange> reservedRange,
                List<String> reservedName,
                DescriptorProtos.SymbolVisibility visibility
        ) {
            this.name = name;
            this.field = field;
            this.extension = extension;
            this.nestedType = nestedType;
            this.enumType = enumType;
            this.extensionRange = extensionRange;
            this.oneofDecl = oneofDecl;
            this.options = options;
            this.reservedRange = reservedRange;
            this.reservedName = reservedName;
            this.visibility = visibility;
        }

        public String name() {
            return name;
        }

        public SequencedCollection<DescriptorProtos.FieldDescriptorProto> field() {
            return Collections.unmodifiableSequencedCollection(field);
        }

        public SequencedCollection<DescriptorProtos.FieldDescriptorProto> extension() {
            return Collections.unmodifiableSequencedCollection(extension);
        }

        public SequencedCollection<DescriptorProtos.DescriptorProto> nestedType() {
            return Collections.unmodifiableSequencedCollection(nestedType);
        }

        public SequencedCollection<DescriptorProtos.EnumDescriptorProto> enumType() {
            return Collections.unmodifiableSequencedCollection(enumType);
        }

        public SequencedCollection<DescriptorProtos.DescriptorProto.ExtensionRange> extensionRange() {
            return Collections.unmodifiableSequencedCollection(extensionRange);
        }

        public SequencedCollection<DescriptorProtos.OneofDescriptorProto> oneofDecl() {
            return Collections.unmodifiableSequencedCollection(oneofDecl);
        }

        public DescriptorProtos.MessageOptions options() {
            return options;
        }

        public SequencedCollection<DescriptorProtos.DescriptorProto.ReservedRange> reservedRange() {
            return Collections.unmodifiableSequencedCollection(reservedRange);
        }

        public SequencedCollection<String> reservedName() {
            return Collections.unmodifiableSequencedCollection(reservedName);
        }

        public DescriptorProtos.SymbolVisibility visibility() {
            return visibility;
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.ExtensionRangeOptions}
     */
    @ProtobufMessage
    public static final class ExtensionRangeOptions {
        // proto: extensions 1000 to max

        /**
         * The verification state of the extension range.
         *
         * <p>Protobuf enum {@code google.protobuf.ExtensionRangeOptions.VerificationState}
         */
        @ProtobufEnum
        public enum VerificationState {
            /**
             * All the extensions of the range must be declared.
             */
            @ProtobufEnum.Constant(index = 0)
            DECLARATION,

            @ProtobufEnum.Constant(index = 1)
            UNVERIFIED;
        }

        /**
         * <p>Protobuf type {@code google.protobuf.ExtensionRangeOptions.Declaration}
         */
        @ProtobufMessage(reservedIndexes = {4})
        public static final class Declaration {

            /**
             * The extension number declared within the extension range.
             *
             * <p><code>int32 number = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            int number;

            /**
             * The fully-qualified name of the extension field. There must be a leading
             * dot in front of the full name.
             *
             * <p><code>string full_name = 2;</code>
             */
            @ProtobufMessage.StringField(index = 2)
            String fullName;

            /**
             * The fully-qualified type name of the extension field. Unlike
             * Metadata.type, Declaration.type must have a leading dot for messages
             * and enums.
             *
             * <p><code>string type = 3;</code>
             */
            @ProtobufMessage.StringField(index = 3)
            String type;

            /**
             * If true, indicates that the number is reserved in the extension range,
             * and any extension field with the number will fail to compile. Set this
             * when a declared extension field is deleted.
             *
             * <p><code>bool reserved = 5;</code>
             */
            @ProtobufMessage.BoolField(index = 5)
            boolean reserved;

            /**
             * If true, indicates that the extension must be defined as repeated.
             * Otherwise the extension must be defined as optional.
             *
             * <p><code>bool repeated = 6;</code>
             */
            @ProtobufMessage.BoolField(index = 6)
            boolean repeated;

            Declaration(int number, String fullName, String type, boolean reserved, boolean repeated) {
                this.number = number;
                this.fullName = fullName;
                this.type = type;
                this.reserved = reserved;
                this.repeated = repeated;
            }

            public int number() {
                return number;
            }

            public String fullName() {
                return fullName;
            }

            public String type() {
                return type;
            }

            public boolean reserved() {
                return reserved;
            }

            public boolean repeated() {
                return repeated;
            }
        }

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        /**
         * For external users: DO NOT USE. We are in the process of open sourcing
         * extension declaration and executing internal cleanups before it can be
         * used externally.
         *
         * <p><code>repeated Declaration declaration = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        List<DescriptorProtos.ExtensionRangeOptions.Declaration> declaration;

        /**
         * Any features defined in the specific edition.
         *
         * <p><code>FeatureSet features = 50;</code>
         */
        @ProtobufMessage.MessageField(index = 50)
        DescriptorProtos.FeatureSet features;

        /**
         * The verification state of the range.
         * TODO: flip the default to DECLARATION once all empty ranges
         * are marked as UNVERIFIED.
         *
         * <p><code>VerificationState verification = 3;</code>
         */
        // proto default: UNVERIFIED
        @ProtobufMessage.EnumField(index = 3)
        DescriptorProtos.ExtensionRangeOptions.VerificationState verification;

        ExtensionRangeOptions(
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption,
                List<DescriptorProtos.ExtensionRangeOptions.Declaration> declaration,
                DescriptorProtos.FeatureSet features,
                DescriptorProtos.ExtensionRangeOptions.VerificationState verification
        ) {
            this.uninterpretedOption = uninterpretedOption;
            this.declaration = declaration;
            this.features = features;
            this.verification = verification;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }

        public SequencedCollection<DescriptorProtos.ExtensionRangeOptions.Declaration> declaration() {
            return Collections.unmodifiableSequencedCollection(declaration);
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public DescriptorProtos.ExtensionRangeOptions.VerificationState verification() {
            return verification;
        }
    }

    /**
     * Describes a field within a message.
     *
     * <p>Protobuf type {@code google.protobuf.FieldDescriptorProto}
     */
    @ProtobufMessage
    public static final class FieldDescriptorProto {

        /**
         * <p>Protobuf enum {@code google.protobuf.FieldDescriptorProto.Type}
         */
        @ProtobufEnum
        public enum Type {
            /**
             * 0 is reserved for errors.
             * Order is weird for historical reasons.
             */
            @ProtobufEnum.Constant(index = 1)
            TYPE_DOUBLE,

            @ProtobufEnum.Constant(index = 2)
            TYPE_FLOAT,

            /**
             * Not ZigZag encoded.  Negative numbers take 10 bytes.  Use TYPE_SINT64 if
             * negative values are likely.
             */
            @ProtobufEnum.Constant(index = 3)
            TYPE_INT64,

            @ProtobufEnum.Constant(index = 4)
            TYPE_UINT64,

            /**
             * Not ZigZag encoded.  Negative numbers take 10 bytes.  Use TYPE_SINT32 if
             * negative values are likely.
             */
            @ProtobufEnum.Constant(index = 5)
            TYPE_INT32,

            @ProtobufEnum.Constant(index = 6)
            TYPE_FIXED64,

            @ProtobufEnum.Constant(index = 7)
            TYPE_FIXED32,

            @ProtobufEnum.Constant(index = 8)
            TYPE_BOOL,

            @ProtobufEnum.Constant(index = 9)
            TYPE_STRING,

            /**
             * Tag-delimited aggregate.
             * Group type is deprecated and not supported after google.protobuf. However, Proto3
             * implementations should still be able to parse the group wire format and
             * treat group fields as unknown fields.  In Editions, the group wire format
             * can be enabled via the {@code message_encoding} feature.
             */
            @ProtobufEnum.Constant(index = 10)
            TYPE_GROUP,

            /**
             * Length-delimited aggregate.
             */
            @ProtobufEnum.Constant(index = 11)
            TYPE_MESSAGE,

            /**
             * New in version 2.
             */
            @ProtobufEnum.Constant(index = 12)
            TYPE_BYTES,

            @ProtobufEnum.Constant(index = 13)
            TYPE_UINT32,

            @ProtobufEnum.Constant(index = 14)
            TYPE_ENUM,

            @ProtobufEnum.Constant(index = 15)
            TYPE_SFIXED32,

            @ProtobufEnum.Constant(index = 16)
            TYPE_SFIXED64,

            /**
             * Uses ZigZag encoding.
             */
            @ProtobufEnum.Constant(index = 17)
            TYPE_SINT32,

            /**
             * Uses ZigZag encoding.
             */
            @ProtobufEnum.Constant(index = 18)
            TYPE_SINT64;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FieldDescriptorProto.Label}
         */
        @ProtobufEnum
        public enum Label {
            /**
             * 0 is reserved for errors
             */
            @ProtobufEnum.Constant(index = 1)
            LABEL_OPTIONAL,

            @ProtobufEnum.Constant(index = 3)
            LABEL_REPEATED,

            /**
             * The required label is only allowed in google.protobuf.  In proto3 and Editions
             * it's explicitly prohibited.  In Editions, the {@code field_presence} feature
             * can be used to get this behavior.
             */
            @ProtobufEnum.Constant(index = 2)
            LABEL_REQUIRED;
        }

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>int32 number = 3;</code>
         */
        @ProtobufMessage.Int32Field(index = 3)
        int number;

        /**
         * <p><code>Label label = 4;</code>
         */
        @ProtobufMessage.EnumField(index = 4)
        DescriptorProtos.FieldDescriptorProto.Label label;

        /**
         * If type_name is set, this need not be set.  If both this and type_name
         * are set, this must be one of TYPE_ENUM, TYPE_MESSAGE or TYPE_GROUP.
         *
         * <p><code>Type type = 5;</code>
         */
        @ProtobufMessage.EnumField(index = 5)
        Type type;

        /**
         * For message and enum types, this is the name of the type.  If the name
         * starts with a '.', it is fully-qualified.  Otherwise, C++-like scoping
         * rules are used to find the type (i.e. first the nested types within this
         * message are searched, then within the parent, on up to the root
         * namespace).
         *
         * <p><code>string type_name = 6;</code>
         */
        @ProtobufMessage.StringField(index = 6)
        String typeName;

        /**
         * For extensions, this is the name of the type being extended.  It is
         * resolved in the same manner as type_name.
         *
         * <p><code>string extendee = 2;</code>
         */
        @ProtobufMessage.StringField(index = 2)
        String extendee;

        /**
         * For numeric types, contains the original text representation of the value.
         * For booleans, "true" or "false".
         * For strings, contains the default text contents (not escaped in any way).
         * For bytes, contains the C escaped value.  All bytes &gt;= 128 are escaped.
         *
         * <p><code>string default_value = 7;</code>
         */
        @ProtobufMessage.StringField(index = 7)
        String defaultValue;

        /**
         * If set, gives the index of a oneof in the containing type's oneof_decl
         * list.  This field is a member of that oneof.
         *
         * <p><code>int32 oneof_index = 9;</code>
         */
        @ProtobufMessage.Int32Field(index = 9)
        int oneofIndex;

        /**
         * JSON name of this field. The value is set by protocol compiler. If the
         * user has set a "json_name" option on this field, that option's value
         * will be used. Otherwise, it's deduced from the field's name by converting
         * it to camelCase.
         *
         * <p><code>string json_name = 10;</code>
         */
        @ProtobufMessage.StringField(index = 10)
        String jsonName;

        /**
         * <p><code>FieldOptions options = 8;</code>
         */
        @ProtobufMessage.MessageField(index = 8)
        DescriptorProtos.FieldOptions options;

        /**
         * If true, this is a proto3 "optional". When a proto3 field is optional, it
         * tracks presence regardless of field type.
         *
         * <p>When proto3_optional is true, this field must belong to a oneof to signal
         * to old proto3 clients that presence is tracked for this field. This oneof
         * is known as a "synthetic" oneof, and this field must be its sole member
         * (each proto3 optional field gets its own synthetic oneof). Synthetic oneofs
         * exist in the descriptor only, and do not generate any API. Synthetic oneofs
         * must be ordered after all "real" oneofs.
         *
         * <p>For message fields, proto3_optional doesn't create any semantic change,
         * since non-repeated message fields always track presence. However it still
         * indicates the semantic detail of whether the user wrote "optional" or not.
         * This can be useful for round-tripping the .proto file. For consistency we
         * give message fields a synthetic oneof also, even though it is not required
         * to track presence. This is especially important because the parser can't
         * tell if a field is a message or an enum, so it must always create a
         * synthetic oneof.
         *
         * <p>Proto2 optional fields do not set this flag, because they already indicate
         * optional with {@code LABEL_OPTIONAL}.
         *
         * <p><code>bool proto3_optional = 17;</code>
         */
        @ProtobufMessage.BoolField(index = 17)
        boolean proto3Optional;

        FieldDescriptorProto(
                String name,
                int number,
                DescriptorProtos.FieldDescriptorProto.Label label,
                Type type,
                String typeName,
                String extendee,
                String defaultValue,
                int oneofIndex,
                String jsonName,
                DescriptorProtos.FieldOptions options,
                boolean proto3Optional
        ) {
            this.name = name;
            this.number = number;
            this.label = label;
            this.type = type;
            this.typeName = typeName;
            this.extendee = extendee;
            this.defaultValue = defaultValue;
            this.oneofIndex = oneofIndex;
            this.jsonName = jsonName;
            this.options = options;
            this.proto3Optional = proto3Optional;
        }

        public String name() {
            return name;
        }

        public int number() {
            return number;
        }

        public DescriptorProtos.FieldDescriptorProto.Label label() {
            return label;
        }

        public Type type() {
            return type;
        }

        public String typeName() {
            return typeName;
        }

        public String extendee() {
            return extendee;
        }

        public String defaultValue() {
            return defaultValue;
        }

        public int oneofIndex() {
            return oneofIndex;
        }

        public String jsonName() {
            return jsonName;
        }

        public DescriptorProtos.FieldOptions options() {
            return options;
        }

        public boolean proto3Optional() {
            return proto3Optional;
        }
    }

    /**
     * Describes a oneof.
     *
     * <p>Protobuf type {@code google.protobuf.OneofDescriptorProto}
     */
    @ProtobufMessage
    public static final class OneofDescriptorProto {

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>OneofOptions options = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        DescriptorProtos.OneofOptions options;

        OneofDescriptorProto(String name, DescriptorProtos.OneofOptions options) {
            this.name = name;
            this.options = options;
        }

        public String name() {
            return name;
        }

        public DescriptorProtos.OneofOptions options() {
            return options;
        }
    }

    /**
     * Describes an enum type.
     *
     * <p>Protobuf type {@code google.protobuf.EnumDescriptorProto}
     */
    @ProtobufMessage
    public static final class EnumDescriptorProto {

        /**
         * Range of reserved numeric values. Reserved values may not be used by
         * entries in the same enum. Reserved ranges may not overlap.
         *
         * <p>Note that this is distinct from DescriptorProto.ReservedRange in that it
         * is inclusive such that it can appropriately represent the entire int32
         * domain.
         *
         * <p>Protobuf type {@code google.protobuf.EnumDescriptorProto.EnumReservedRange}
         */
        @ProtobufMessage
        public static final class EnumReservedRange {

            /**
             * Inclusive.
             *
             * <p><code>int32 start = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            int start;

            /**
             * Inclusive.
             *
             * <p><code>int32 end = 2;</code>
             */
            @ProtobufMessage.Int32Field(index = 2)
            int end;

            EnumReservedRange(int start, int end) {
                this.start = start;
                this.end = end;
            }

            public int start() {
                return start;
            }

            public int end() {
                return end;
            }
        }

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>repeated EnumValueDescriptorProto value = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        List<DescriptorProtos.EnumValueDescriptorProto> value;

        /**
         * <p><code>EnumOptions options = 3;</code>
         */
        @ProtobufMessage.MessageField(index = 3)
        DescriptorProtos.EnumOptions options;

        /**
         * Range of reserved numeric values. Reserved numeric values may not be used
         * by enum values in the same enum declaration. Reserved ranges may not
         * overlap.
         *
         * <p><code>repeated EnumReservedRange reserved_range = 4;</code>
         */
        @ProtobufMessage.MessageField(index = 4)
        List<DescriptorProtos.EnumDescriptorProto.EnumReservedRange> reservedRange;

        /**
         * Reserved enum value names, which may not be reused. A given name may only
         * be reserved once.
         *
         * <p><code>repeated string reserved_name = 5;</code>
         */
        @ProtobufMessage.StringField(index = 5)
        List<String> reservedName;

        /**
         * Support for {@code export} and {@code local} keywords on enums.
         *
         * <p><code>SymbolVisibility visibility = 6;</code>
         */
        @ProtobufMessage.EnumField(index = 6)
        DescriptorProtos.SymbolVisibility visibility;

        EnumDescriptorProto(
                String name,
                List<DescriptorProtos.EnumValueDescriptorProto> value,
                DescriptorProtos.EnumOptions options,
                List<DescriptorProtos.EnumDescriptorProto.EnumReservedRange> reservedRange,
                List<String> reservedName,
                DescriptorProtos.SymbolVisibility visibility
        ) {
            this.name = name;
            this.value = value;
            this.options = options;
            this.reservedRange = reservedRange;
            this.reservedName = reservedName;
            this.visibility = visibility;
        }

        public String name() {
            return name;
        }

        public SequencedCollection<DescriptorProtos.EnumValueDescriptorProto> value() {
            return Collections.unmodifiableSequencedCollection(value);
        }

        public DescriptorProtos.EnumOptions options() {
            return options;
        }

        public SequencedCollection<DescriptorProtos.EnumDescriptorProto.EnumReservedRange> reservedRange() {
            return Collections.unmodifiableSequencedCollection(reservedRange);
        }

        public SequencedCollection<String> reservedName() {
            return Collections.unmodifiableSequencedCollection(reservedName);
        }

        public DescriptorProtos.SymbolVisibility visibility() {
            return visibility;
        }
    }

    /**
     * Describes a value within an enum.
     *
     * <p>Protobuf type {@code google.protobuf.EnumValueDescriptorProto}
     */
    @ProtobufMessage
    public static final class EnumValueDescriptorProto {

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>int32 number = 2;</code>
         */
        @ProtobufMessage.Int32Field(index = 2)
        int number;

        /**
         * <p><code>EnumValueOptions options = 3;</code>
         */
        @ProtobufMessage.MessageField(index = 3)
        DescriptorProtos.EnumValueOptions options;

        EnumValueDescriptorProto(String name, int number, DescriptorProtos.EnumValueOptions options) {
            this.name = name;
            this.number = number;
            this.options = options;
        }

        public String name() {
            return name;
        }

        public int number() {
            return number;
        }

        public DescriptorProtos.EnumValueOptions options() {
            return options;
        }
    }

    /**
     * Describes a service.
     *
     * <p>Protobuf type {@code google.protobuf.ServiceDescriptorProto}
     */
    @ProtobufMessage(reservedIndexes = {4}, reservedNames = {"stream"})
    public static final class ServiceDescriptorProto {

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * <p><code>repeated MethodDescriptorProto method = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        List<DescriptorProtos.MethodDescriptorProto> method;

        /**
         * <p><code>ServiceOptions options = 3;</code>
         */
        @ProtobufMessage.MessageField(index = 3)
        DescriptorProtos.ServiceOptions options;

        ServiceDescriptorProto(
                String name,
                List<DescriptorProtos.MethodDescriptorProto> method,
                DescriptorProtos.ServiceOptions options
        ) {
            this.name = name;
            this.method = method;
            this.options = options;
        }

        public String name() {
            return name;
        }

        public SequencedCollection<DescriptorProtos.MethodDescriptorProto> method() {
            return Collections.unmodifiableSequencedCollection(method);
        }

        public DescriptorProtos.ServiceOptions options() {
            return options;
        }
    }

    /**
     * Describes a method of a service.
     *
     * <p>Protobuf type {@code google.protobuf.MethodDescriptorProto}
     */
    @ProtobufMessage
    public static final class MethodDescriptorProto {

        /**
         * <p><code>string name = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String name;

        /**
         * Input and output type names.  These are resolved in the same way as
         * FieldDescriptorProto.type_name, but must refer to a message type.
         *
         * <p><code>string input_type = 2;</code>
         */
        @ProtobufMessage.StringField(index = 2)
        String inputType;

        /**
         * <p><code>string output_type = 3;</code>
         */
        @ProtobufMessage.StringField(index = 3)
        String outputType;

        /**
         * <p><code>MethodOptions options = 4;</code>
         */
        @ProtobufMessage.MessageField(index = 4)
        DescriptorProtos.MethodOptions options;

        /**
         * Identifies if client streams multiple client messages
         *
         * <p><code>bool client_streaming = 5;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 5)
        boolean clientStreaming;

        /**
         * Identifies if server streams multiple server messages
         *
         * <p><code>bool server_streaming = 6;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 6)
        boolean serverStreaming;

        MethodDescriptorProto(
                String name,
                String inputType,
                String outputType,
                DescriptorProtos.MethodOptions options,
                boolean clientStreaming,
                boolean serverStreaming
        ) {
            this.name = name;
            this.inputType = inputType;
            this.outputType = outputType;
            this.options = options;
            this.clientStreaming = clientStreaming;
            this.serverStreaming = serverStreaming;
        }

        public String name() {
            return name;
        }

        public String inputType() {
            return inputType;
        }

        public String outputType() {
            return outputType;
        }

        public DescriptorProtos.MethodOptions options() {
            return options;
        }

        public boolean clientStreaming() {
            return clientStreaming;
        }

        public boolean serverStreaming() {
            return serverStreaming;
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.FileOptions}
     */
    @ProtobufMessage(reservedIndexes = {42, 38}, reservedNames = {"php_generic_services"})
    public static final class FileOptions {
        // proto: extensions 1000 to max

        /**
         * Generated classes can be optimized for speed or code size.
         *
         * <p>Protobuf enum {@code google.protobuf.FileOptions.OptimizeMode}
         */
        @ProtobufEnum
        public enum OptimizeMode {
            /**
             * Generate complete code for parsing, serialization,
             */
            @ProtobufEnum.Constant(index = 1)
            SPEED,

            /**
             * etc.
             * Use ReflectionOps to implement these methods.
             */
            @ProtobufEnum.Constant(index = 2)
            CODE_SIZE,

            /**
             * Generate code using MessageLite and the lite runtime.
             */
            @ProtobufEnum.Constant(index = 3)
            LITE_RUNTIME;
        }

        /**
         * Sets the Java package where classes generated from this .proto will be
         * placed.  By default, the proto package is used, but this is often
         * inappropriate because proto packages do not normally start with backwards
         * domain names.
         *
         * <p><code>string java_package = 1;</code>
         */
        @ProtobufMessage.StringField(index = 1)
        String javaPackage;

        /**
         * Controls the name of the wrapper Java class generated for the .proto file.
         * That class will always contain the .proto file's getDescriptor() method as
         * well as any top-level extensions defined in the .proto file.
         * If java_multiple_files is disabled, then all the other classes from the
         * .proto file will be nested inside the single wrapper outer class.
         *
         * <p><code>string java_outer_classname = 8;</code>
         */
        @ProtobufMessage.StringField(index = 8)
        String javaOuterClassname;

        /**
         * If enabled, then the Java code generator will generate a separate .java
         * file for each top-level message, enum, and service defined in the .proto
         * file.  Thus, these types will *not* be nested inside the wrapper class
         * named by java_outer_classname.  However, the wrapper class will still be
         * generated to contain the file's getDescriptor() method as well as any
         * top-level extensions defined in the file.
         *
         * <p><code>bool java_multiple_files = 10;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 10)
        boolean javaMultipleFiles;

        /**
         * This option does nothing.
         *
         * <p><code>bool java_generate_equals_and_hash = 20;</code>
         */
        @Deprecated
        @ProtobufMessage.BoolField(index = 20)
        boolean javaGenerateEqualsAndHash;

        /**
         * A proto2 file can set this to true to opt in to UTF-8 checking for Java,
         * which will throw an exception if invalid UTF-8 is parsed from the wire or
         * assigned to a string field.
         *
         * <p>TODO: clarify exactly what kinds of field types this option
         * applies to, and update these docs accordingly.
         *
         * <p>Proto3 files already perform these checks. Setting the option explicitly to
         * false has no effect: it cannot be used to opt proto3 files out of UTF-8
         * checks.
         *
         * <p><code>bool java_string_check_utf8 = 27;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 27)
        boolean javaStringCheckUtf8;

        /**
         * <p><code>OptimizeMode optimize_for = 9;</code>
         */
        // proto default: SPEED
        @ProtobufMessage.EnumField(index = 9)
        DescriptorProtos.FileOptions.OptimizeMode optimizeFor;

        /**
         * Sets the Go package where structs generated from this .proto will be
         * placed. If omitted, the Go package will be derived from the following:
         *
         * <pre>
         * - The basename of the package import path, if provided.
         * - Otherwise, the package statement in the .proto file, if present.
         * - Otherwise, the basename of the .proto file, without extension.
         * </pre>
         *
         * <p><code>string go_package = 11;</code>
         */
        @ProtobufMessage.StringField(index = 11)
        String goPackage;

        /**
         * Should generic services be generated in each language?  "Generic" services
         * are not specific to any particular RPC system.  They are generated by the
         * main code generators in each language (without additional plugins).
         * Generic services were the only kind of service generation supported by
         * early versions of google.protobuf.
         *
         * <p>Generic services are now considered deprecated in favor of using plugins
         * that generate code specific to your particular RPC system.  Therefore,
         * these default to false.  Old code which depends on generic services should
         * explicitly set them to true.
         *
         * <p><code>bool cc_generic_services = 16;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 16)
        boolean ccGenericServices;

        /**
         * <p><code>bool java_generic_services = 17;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 17)
        boolean javaGenericServices;

        /**
         * <p><code>bool py_generic_services = 18;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 18)
        boolean pyGenericServices;

        /**
         * Is this file deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for everything in the file, or it will be completely ignored; in the very
         * least, this is a formalization for deprecating files.
         *
         * <p><code>bool deprecated = 23;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 23)
        boolean deprecated;

        /**
         * Enables the use of arenas for the proto messages in this file. This applies
         * only to generated classes for C++.
         *
         * <p><code>bool cc_enable_arenas = 31;</code>
         */
        // proto default: true
        @ProtobufMessage.BoolField(index = 31)
        boolean ccEnableArenas;

        /**
         * Sets the objective c class prefix which is prepended to all objective c
         * generated classes from this .proto. There is no default.
         *
         * <p><code>string objc_class_prefix = 36;</code>
         */
        @ProtobufMessage.StringField(index = 36)
        String objcClassPrefix;

        /**
         * Namespace for generated classes; defaults to the package.
         *
         * <p><code>string csharp_namespace = 37;</code>
         */
        @ProtobufMessage.StringField(index = 37)
        String csharpNamespace;

        /**
         * By default Swift generators will take the proto package and CamelCase it
         * replacing '.' with underscore and use that to prefix the types/symbols
         * defined. When this options is provided, they will use this value instead
         * to prefix the types/symbols defined.
         *
         * <p><code>string swift_prefix = 39;</code>
         */
        @ProtobufMessage.StringField(index = 39)
        String swiftPrefix;

        /**
         * Sets the php class prefix which is prepended to all php generated classes
         * from this .proto. Default is empty.
         *
         * <p><code>string php_class_prefix = 40;</code>
         */
        @ProtobufMessage.StringField(index = 40)
        String phpClassPrefix;

        /**
         * Use this option to change the namespace of php generated classes. Default
         * is empty. When this option is empty, the package name will be used for
         * determining the namespace.
         *
         * <p><code>string php_namespace = 41;</code>
         */
        @ProtobufMessage.StringField(index = 41)
        String phpNamespace;

        /**
         * Use this option to change the namespace of php generated metadata classes.
         * Default is empty. When this option is empty, the proto file name will be
         * used for determining the namespace.
         *
         * <p><code>string php_metadata_namespace = 44;</code>
         */
        @ProtobufMessage.StringField(index = 44)
        String phpMetadataNamespace;

        /**
         * Use this option to change the package of ruby generated classes. Default
         * is empty. When this option is not set, the package name will be used for
         * determining the ruby package.
         *
         * <p><code>string ruby_package = 45;</code>
         */
        @ProtobufMessage.StringField(index = 45)
        String rubyPackage;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 50;</code>
         */
        @ProtobufMessage.MessageField(index = 50)
        DescriptorProtos.FeatureSet features;

        /**
         * The parser stores options it doesn't recognize here.
         * See the documentation for the "Options" section above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        FileOptions(
                String javaPackage,
                String javaOuterClassname,
                boolean javaMultipleFiles,
                boolean javaGenerateEqualsAndHash,
                boolean javaStringCheckUtf8,
                DescriptorProtos.FileOptions.OptimizeMode optimizeFor,
                String goPackage,
                boolean ccGenericServices,
                boolean javaGenericServices,
                boolean pyGenericServices,
                boolean deprecated,
                boolean ccEnableArenas,
                String objcClassPrefix,
                String csharpNamespace,
                String swiftPrefix,
                String phpClassPrefix,
                String phpNamespace,
                String phpMetadataNamespace,
                String rubyPackage,
                DescriptorProtos.FeatureSet features,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.javaPackage = javaPackage;
            this.javaOuterClassname = javaOuterClassname;
            this.javaMultipleFiles = javaMultipleFiles;
            this.javaGenerateEqualsAndHash = javaGenerateEqualsAndHash;
            this.javaStringCheckUtf8 = javaStringCheckUtf8;
            this.optimizeFor = optimizeFor;
            this.goPackage = goPackage;
            this.ccGenericServices = ccGenericServices;
            this.javaGenericServices = javaGenericServices;
            this.pyGenericServices = pyGenericServices;
            this.deprecated = deprecated;
            this.ccEnableArenas = ccEnableArenas;
            this.objcClassPrefix = objcClassPrefix;
            this.csharpNamespace = csharpNamespace;
            this.swiftPrefix = swiftPrefix;
            this.phpClassPrefix = phpClassPrefix;
            this.phpNamespace = phpNamespace;
            this.phpMetadataNamespace = phpMetadataNamespace;
            this.rubyPackage = rubyPackage;
            this.features = features;
            this.uninterpretedOption = uninterpretedOption;
        }

        public String javaPackage() {
            return javaPackage;
        }

        public String javaOuterClassname() {
            return javaOuterClassname;
        }

        public boolean javaMultipleFiles() {
            return javaMultipleFiles;
        }

        @Deprecated
        public boolean javaGenerateEqualsAndHash() {
            return javaGenerateEqualsAndHash;
        }

        public boolean javaStringCheckUtf8() {
            return javaStringCheckUtf8;
        }

        public DescriptorProtos.FileOptions.OptimizeMode optimizeFor() {
            return optimizeFor;
        }

        public String goPackage() {
            return goPackage;
        }

        public boolean ccGenericServices() {
            return ccGenericServices;
        }

        public boolean javaGenericServices() {
            return javaGenericServices;
        }

        public boolean pyGenericServices() {
            return pyGenericServices;
        }

        public boolean deprecated() {
            return deprecated;
        }

        public boolean ccEnableArenas() {
            return ccEnableArenas;
        }

        public String objcClassPrefix() {
            return objcClassPrefix;
        }

        public String csharpNamespace() {
            return csharpNamespace;
        }

        public String swiftPrefix() {
            return swiftPrefix;
        }

        public String phpClassPrefix() {
            return phpClassPrefix;
        }

        public String phpNamespace() {
            return phpNamespace;
        }

        public String phpMetadataNamespace() {
            return phpMetadataNamespace;
        }

        public String rubyPackage() {
            return rubyPackage;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.MessageOptions}
     */
    @ProtobufMessage(reservedIndexes = {4, 5, 6, 8, 9})
    public static final class MessageOptions {
        // proto: extensions 1000 to max

        /**
         * Set true to use the old proto1 MessageSet wire format for extensions.
         * This is provided for backwards-compatibility with the MessageSet wire
         * format.  You should not use this for any other reason:  It's less
         * efficient, has fewer features, and is more complicated.
         *
         * <p>The message must be defined exactly as follows:
         *
         * <pre>
         * message Foo {
         *   option message_set_wire_format = true;
         *   extensions 4 to max;
         * }
         * </pre>
         *
         * <p>Note that the message cannot have any defined fields; MessageSets only
         * have extensions.
         *
         * <p>All extensions of your type must be singular messages; e.g. they cannot
         * be int32s, enums, or repeated messages.
         *
         * <p>Because this is an option, the above two restrictions are not enforced by
         * the protocol compiler.
         *
         * <p><code>bool message_set_wire_format = 1;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 1)
        boolean messageSetWireFormat;

        /**
         * Disables the generation of the standard "descriptor()" accessor, which can
         * conflict with a field of the same name.  This is meant to make migration
         * from proto1 easier; new code should avoid fields named "descriptor".
         *
         * <p><code>bool no_standard_descriptor_accessor = 2;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 2)
        boolean noStandardDescriptorAccessor;

        /**
         * Is this message deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for the message, or it will be completely ignored; in the very least,
         * this is a formalization for deprecating messages.
         *
         * <p><code>bool deprecated = 3;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 3)
        boolean deprecated;

        /**
         * Whether the message is an automatically generated map entry type for the
         * maps field.
         *
         * <p>For maps fields:
         *
         * <pre>
         *   map&lt;KeyType, ValueType&gt; map_field = 1;
         * </pre>
         *
         * <p>The parsed descriptor looks like:
         *
         * <pre>
         *   message MapFieldEntry {
         *       option map_entry = true;
         *       optional KeyType key = 1;
         *       optional ValueType value = 2;
         *   }
         *   repeated MapFieldEntry map_field = 1;
         * </pre>
         *
         * <p>Implementations may choose not to generate the map_entry=true message, but
         * use a native map in the target language to hold the keys and values.
         * The reflection APIs in such implementations still need to work as
         * if the field is a repeated message field.
         *
         * <p>NOTE: Do not set the option in .proto files. Always use the maps syntax
         * instead. The option should only be implicitly set by the proto compiler
         * parser.
         *
         * <p><code>bool map_entry = 7;</code>
         */
        @ProtobufMessage.BoolField(index = 7)
        boolean mapEntry;

        /**
         * Enable the legacy handling of JSON field name conflicts.  This lowercases
         * and strips underscored from the fields before comparison in proto3 only.
         * The new behavior takes {@code json_name} into account and applies to proto2 as
         * well.
         *
         * <p>This should only be used as a temporary measure against broken builds due
         * to the change in behavior for JSON field name conflicts.
         *
         * <p>TODO This is legacy behavior we plan to remove once downstream
         * teams have had time to migrate.
         *
         * <p><code>bool deprecated_legacy_json_field_conflicts = 11;</code>
         */
        @Deprecated
        @ProtobufMessage.BoolField(index = 11)
        boolean deprecatedLegacyJsonFieldConflicts;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 12;</code>
         */
        @ProtobufMessage.MessageField(index = 12)
        DescriptorProtos.FeatureSet features;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        MessageOptions(
                boolean messageSetWireFormat,
                boolean noStandardDescriptorAccessor,
                boolean deprecated,
                boolean mapEntry,
                boolean deprecatedLegacyJsonFieldConflicts,
                DescriptorProtos.FeatureSet features,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.messageSetWireFormat = messageSetWireFormat;
            this.noStandardDescriptorAccessor = noStandardDescriptorAccessor;
            this.deprecated = deprecated;
            this.mapEntry = mapEntry;
            this.deprecatedLegacyJsonFieldConflicts = deprecatedLegacyJsonFieldConflicts;
            this.features = features;
            this.uninterpretedOption = uninterpretedOption;
        }

        public boolean messageSetWireFormat() {
            return messageSetWireFormat;
        }

        public boolean noStandardDescriptorAccessor() {
            return noStandardDescriptorAccessor;
        }

        public boolean deprecated() {
            return deprecated;
        }

        public boolean mapEntry() {
            return mapEntry;
        }

        @Deprecated
        public boolean deprecatedLegacyJsonFieldConflicts() {
            return deprecatedLegacyJsonFieldConflicts;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.FieldOptions}
     */
    @ProtobufMessage(reservedIndexes = {4, 18})
    public static final class FieldOptions {
        // proto: extensions 1000 to max

        /**
         * <p>Protobuf enum {@code google.protobuf.FieldOptions.CType}
         */
        @ProtobufEnum
        public enum CType {
            /**
             * Default mode.
             */
            @ProtobufEnum.Constant(index = 0)
            STRING,

            /**
             * The option [ctype=CORD] may be applied to a non-repeated field of type
             * "bytes". It indicates that in C++, the data should be stored in a Cord
             * instead of a string.  For very large strings, this may reduce memory
             * fragmentation. It may also allow better performance when parsing from a
             * Cord, or when parsing with aliasing enabled, as the parsed Cord may then
             * alias the original buffer.
             */
            @ProtobufEnum.Constant(index = 1)
            CORD,

            @ProtobufEnum.Constant(index = 2)
            STRING_PIECE;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FieldOptions.JSType}
         */
        @ProtobufEnum
        public enum JSType {
            /**
             * Use the default type.
             */
            @ProtobufEnum.Constant(index = 0)
            JS_NORMAL,

            /**
             * Use JavaScript strings.
             */
            @ProtobufEnum.Constant(index = 1)
            JS_STRING,

            /**
             * Use JavaScript numbers.
             */
            @ProtobufEnum.Constant(index = 2)
            JS_NUMBER;
        }

        /**
         * If set to RETENTION_SOURCE, the option will be omitted from the binary.
         *
         * <p>Protobuf enum {@code google.protobuf.FieldOptions.OptionRetention}
         */
        @ProtobufEnum
        public enum OptionRetention {
            @ProtobufEnum.Constant(index = 0)
            RETENTION_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            RETENTION_RUNTIME,

            @ProtobufEnum.Constant(index = 2)
            RETENTION_SOURCE;
        }

        /**
         * This indicates the types of entities that the field may apply to when used
         * as an option. If it is unset, then the field may be freely used as an
         * option on any kind of entity.
         *
         * <p>Protobuf enum {@code google.protobuf.FieldOptions.OptionTargetType}
         */
        @ProtobufEnum
        public enum OptionTargetType {
            @ProtobufEnum.Constant(index = 0)
            TARGET_TYPE_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            TARGET_TYPE_FILE,

            @ProtobufEnum.Constant(index = 2)
            TARGET_TYPE_EXTENSION_RANGE,

            @ProtobufEnum.Constant(index = 3)
            TARGET_TYPE_MESSAGE,

            @ProtobufEnum.Constant(index = 4)
            TARGET_TYPE_FIELD,

            @ProtobufEnum.Constant(index = 5)
            TARGET_TYPE_ONEOF,

            @ProtobufEnum.Constant(index = 6)
            TARGET_TYPE_ENUM,

            @ProtobufEnum.Constant(index = 7)
            TARGET_TYPE_ENUM_ENTRY,

            @ProtobufEnum.Constant(index = 8)
            TARGET_TYPE_SERVICE,

            @ProtobufEnum.Constant(index = 9)
            TARGET_TYPE_METHOD;
        }

        /**
         * <p>Protobuf type {@code google.protobuf.FieldOptions.EditionDefault}
         */
        @ProtobufMessage
        public static final class EditionDefault {

            /**
             * <p><code>Edition edition = 3;</code>
             */
            @ProtobufMessage.EnumField(index = 3)
            DescriptorProtos.Edition edition;

            /**
             * Textproto value.
             *
             * <p><code>string value = 2;</code>
             */
            @ProtobufMessage.StringField(index = 2)
            String value;

            EditionDefault(DescriptorProtos.Edition edition, String value) {
                this.edition = edition;
                this.value = value;
            }

            public DescriptorProtos.Edition edition() {
                return edition;
            }

            public String value() {
                return value;
            }
        }

        /**
         * Information about the support window of a feature.
         *
         * <p>Protobuf type {@code google.protobuf.FieldOptions.FeatureSupport}
         */
        @ProtobufMessage
        public static final class FeatureSupport {

            /**
             * The edition that this feature was first available in.  In editions
             * earlier than this one, the default assigned to EDITION_LEGACY will be
             * used, and proto files will not be able to override it.
             *
             * <p><code>Edition edition_introduced = 1;</code>
             */
            @ProtobufMessage.EnumField(index = 1)
            DescriptorProtos.Edition editionIntroduced;

            /**
             * The edition this feature becomes deprecated in.  Using this after this
             * edition may trigger warnings.
             *
             * <p><code>Edition edition_deprecated = 2;</code>
             */
            @ProtobufMessage.EnumField(index = 2)
            DescriptorProtos.Edition editionDeprecated;

            /**
             * The deprecation warning text if this feature is used after the edition it
             * was marked deprecated in.
             *
             * <p><code>string deprecation_warning = 3;</code>
             */
            @ProtobufMessage.StringField(index = 3)
            String deprecationWarning;

            /**
             * The edition this feature is no longer available in.  In editions after
             * this one, the last default assigned will be used, and proto files will
             * not be able to override it.
             *
             * <p><code>Edition edition_removed = 4;</code>
             */
            @ProtobufMessage.EnumField(index = 4)
            DescriptorProtos.Edition editionRemoved;

            /**
             * The removal error text if this feature is used after the edition it was
             * removed in.
             *
             * <p><code>string removal_error = 5;</code>
             */
            @ProtobufMessage.StringField(index = 5)
            String removalError;

            FeatureSupport(
                    DescriptorProtos.Edition editionIntroduced,
                    DescriptorProtos.Edition editionDeprecated,
                    String deprecationWarning,
                    DescriptorProtos.Edition editionRemoved,
                    String removalError
            ) {
                this.editionIntroduced = editionIntroduced;
                this.editionDeprecated = editionDeprecated;
                this.deprecationWarning = deprecationWarning;
                this.editionRemoved = editionRemoved;
                this.removalError = removalError;
            }

            public DescriptorProtos.Edition editionIntroduced() {
                return editionIntroduced;
            }

            public DescriptorProtos.Edition editionDeprecated() {
                return editionDeprecated;
            }

            public String deprecationWarning() {
                return deprecationWarning;
            }

            public DescriptorProtos.Edition editionRemoved() {
                return editionRemoved;
            }

            public String removalError() {
                return removalError;
            }
        }

        /**
         * NOTE: ctype is deprecated. Use {@code features.(pb.cpp).string_type} instead.
         * The ctype option instructs the C++ code generator to use a different
         * representation of the field than it normally would.  See the specific
         * options below.  This option is only implemented to support use of
         * [ctype=CORD] and [ctype=STRING] (the default) on non-repeated fields of
         * type "bytes" in the open source release.
         * TODO: make ctype actually deprecated.
         *
         * <p><code>CType ctype = 1;</code>
         */
        // proto default: STRING
        @ProtobufMessage.EnumField(index = 1)
        DescriptorProtos.FieldOptions.CType ctype;

        /**
         * The packed option can be enabled for repeated primitive fields to enable
         * a more efficient representation on the wire. Rather than repeatedly
         * writing the tag and type for each element, the entire array is encoded as
         * a single length-delimited blob. In proto3, only explicit setting it to
         * false will avoid using packed encoding.  This option is prohibited in
         * Editions, but the {@code repeated_field_encoding} feature can be used to control
         * the behavior.
         *
         * <p><code>bool packed = 2;</code>
         */
        @ProtobufMessage.BoolField(index = 2)
        boolean packed;

        /**
         * The jstype option determines the JavaScript type used for values of the
         * field.  The option is permitted only for 64 bit integral and fixed types
         * (int64, uint64, sint64, fixed64, sfixed64).  A field with jstype JS_STRING
         * is represented as JavaScript string, which avoids loss of precision that
         * can happen when a large value is converted to a floating point JavaScript.
         * Specifying JS_NUMBER for the jstype causes the generated JavaScript code to
         * use the JavaScript "number" type.  The behavior of the default option
         * JS_NORMAL is implementation dependent.
         *
         * <p>This option is an enum to permit additional types to be added, e.g.
         * goog.math.Integer.
         *
         * <p><code>JSType jstype = 6;</code>
         */
        // proto default: JS_NORMAL
        @ProtobufMessage.EnumField(index = 6)
        DescriptorProtos.FieldOptions.JSType jstype;

        /**
         * Should this field be parsed lazily?  Lazy applies only to message-type
         * fields.  It means that when the outer message is initially parsed, the
         * inner message's contents will not be parsed but instead stored in encoded
         * form.  The inner message will actually be parsed when it is first accessed.
         *
         * <p>This is only a hint.  Implementations are free to choose whether to use
         * eager or lazy parsing regardless of the value of this option.  However,
         * setting this option true suggests that the protocol author believes that
         * using lazy parsing on this field is worth the additional bookkeeping
         * overhead typically needed to implement it.
         *
         * <p>This option does not affect the public interface of any generated code;
         * all method signatures remain the same.  Furthermore, thread-safety of the
         * interface is not affected by this option; const methods remain safe to
         * call from multiple threads concurrently, while non-const methods continue
         * to require exclusive access.
         *
         * <p>Note that lazy message fields are still eagerly verified to check
         * ill-formed wireformat or missing required fields. Calling IsInitialized()
         * on the outer message would fail if the inner message has missing required
         * fields. Failed verification would result in parsing failure (except when
         * uninitialized messages are acceptable).
         *
         * <p><code>bool lazy = 5;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 5)
        boolean lazy;

        /**
         * unverified_lazy does no correctness checks on the byte stream. This should
         * only be used where lazy with verification is prohibitive for performance
         * reasons.
         *
         * <p><code>bool unverified_lazy = 15;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 15)
        boolean unverifiedLazy;

        /**
         * Is this field deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for accessors, or it will be completely ignored; in the very least, this
         * is a formalization for deprecating fields.
         *
         * <p><code>bool deprecated = 3;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 3)
        boolean deprecated;

        /**
         * DEPRECATED. DO NOT USE!
         * For Google-internal migration only. Do not use.
         *
         * <p><code>bool weak = 10;</code>
         */
        // proto default: false
        @Deprecated
        @ProtobufMessage.BoolField(index = 10)
        boolean weak;

        /**
         * Indicate that the field value should not be printed out when using debug
         * formats, e.g. when the field contains sensitive credentials.
         *
         * <p><code>bool debug_redact = 16;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 16)
        boolean debugRedact;

        /**
         * <p><code>OptionRetention retention = 17;</code>
         */
        @ProtobufMessage.EnumField(index = 17)
        DescriptorProtos.FieldOptions.OptionRetention retention;

        /**
         * <p><code>repeated OptionTargetType targets = 19;</code>
         */
        @ProtobufMessage.EnumField(index = 19)
        List<DescriptorProtos.FieldOptions.OptionTargetType> targets;

        /**
         * <p><code>repeated EditionDefault edition_defaults = 20;</code>
         */
        @ProtobufMessage.MessageField(index = 20)
        List<DescriptorProtos.FieldOptions.EditionDefault> editionDefaults;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 21;</code>
         */
        @ProtobufMessage.MessageField(index = 21)
        DescriptorProtos.FeatureSet features;

        /**
         * <p><code>FeatureSupport feature_support = 22;</code>
         */
        @ProtobufMessage.MessageField(index = 22)
        DescriptorProtos.FieldOptions.FeatureSupport featureSupport;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        FieldOptions(
                DescriptorProtos.FieldOptions.CType ctype,
                boolean packed,
                DescriptorProtos.FieldOptions.JSType jstype,
                boolean lazy,
                boolean unverifiedLazy,
                boolean deprecated,
                boolean weak,
                boolean debugRedact,
                DescriptorProtos.FieldOptions.OptionRetention retention,
                List<DescriptorProtos.FieldOptions.OptionTargetType> targets,
                List<DescriptorProtos.FieldOptions.EditionDefault> editionDefaults,
                DescriptorProtos.FeatureSet features,
                DescriptorProtos.FieldOptions.FeatureSupport featureSupport,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.ctype = ctype;
            this.packed = packed;
            this.jstype = jstype;
            this.lazy = lazy;
            this.unverifiedLazy = unverifiedLazy;
            this.deprecated = deprecated;
            this.weak = weak;
            this.debugRedact = debugRedact;
            this.retention = retention;
            this.targets = targets;
            this.editionDefaults = editionDefaults;
            this.features = features;
            this.featureSupport = featureSupport;
            this.uninterpretedOption = uninterpretedOption;
        }

        public DescriptorProtos.FieldOptions.CType ctype() {
            return ctype;
        }

        public boolean packed() {
            return packed;
        }

        public DescriptorProtos.FieldOptions.JSType jstype() {
            return jstype;
        }

        public boolean lazy() {
            return lazy;
        }

        public boolean unverifiedLazy() {
            return unverifiedLazy;
        }

        public boolean deprecated() {
            return deprecated;
        }

        @Deprecated
        public boolean weak() {
            return weak;
        }

        public boolean debugRedact() {
            return debugRedact;
        }

        public DescriptorProtos.FieldOptions.OptionRetention retention() {
            return retention;
        }

        public SequencedCollection<DescriptorProtos.FieldOptions.OptionTargetType> targets() {
            return Collections.unmodifiableSequencedCollection(targets);
        }

        public SequencedCollection<DescriptorProtos.FieldOptions.EditionDefault> editionDefaults() {
            return Collections.unmodifiableSequencedCollection(editionDefaults);
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public DescriptorProtos.FieldOptions.FeatureSupport featureSupport() {
            return featureSupport;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.OneofOptions}
     */
    @ProtobufMessage
    public static final class OneofOptions {
        // proto: extensions 1000 to max

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 1;</code>
         */
        @ProtobufMessage.MessageField(index = 1)
        DescriptorProtos.FeatureSet features;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        OneofOptions(
                DescriptorProtos.FeatureSet features,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.features = features;
            this.uninterpretedOption = uninterpretedOption;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.EnumOptions}
     */
    @ProtobufMessage(reservedIndexes = {5})
    public static final class EnumOptions {
        // proto: extensions 1000 to max

        /**
         * Set this option to true to allow mapping different tag names to the same
         * value.
         *
         * <p><code>bool allow_alias = 2;</code>
         */
        @ProtobufMessage.BoolField(index = 2)
        boolean allowAlias;

        /**
         * Is this enum deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for the enum, or it will be completely ignored; in the very least, this
         * is a formalization for deprecating enums.
         *
         * <p><code>bool deprecated = 3;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 3)
        boolean deprecated;

        /**
         * Enable the legacy handling of JSON field name conflicts.  This lowercases
         * and strips underscored from the fields before comparison in proto3 only.
         * The new behavior takes {@code json_name} into account and applies to proto2 as
         * well.
         * TODO Remove this legacy behavior once downstream teams have
         * had time to migrate.
         *
         * <p><code>bool deprecated_legacy_json_field_conflicts = 6;</code>
         */
        @Deprecated
        @ProtobufMessage.BoolField(index = 6)
        boolean deprecatedLegacyJsonFieldConflicts;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 7;</code>
         */
        @ProtobufMessage.MessageField(index = 7)
        DescriptorProtos.FeatureSet features;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        EnumOptions(
                boolean allowAlias,
                boolean deprecated,
                boolean deprecatedLegacyJsonFieldConflicts,
                DescriptorProtos.FeatureSet features,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.allowAlias = allowAlias;
            this.deprecated = deprecated;
            this.deprecatedLegacyJsonFieldConflicts = deprecatedLegacyJsonFieldConflicts;
            this.features = features;
            this.uninterpretedOption = uninterpretedOption;
        }

        public boolean allowAlias() {
            return allowAlias;
        }

        public boolean deprecated() {
            return deprecated;
        }

        @Deprecated
        public boolean deprecatedLegacyJsonFieldConflicts() {
            return deprecatedLegacyJsonFieldConflicts;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.EnumValueOptions}
     */
    @ProtobufMessage
    public static final class EnumValueOptions {
        // proto: extensions 1000 to max

        /**
         * Is this enum value deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for the enum value, or it will be completely ignored; in the very least,
         * this is a formalization for deprecating enum values.
         *
         * <p><code>bool deprecated = 1;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 1)
        boolean deprecated;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        DescriptorProtos.FeatureSet features;

        /**
         * Indicate that fields annotated with this enum value should not be printed
         * out when using debug formats, e.g. when the field contains sensitive
         * credentials.
         *
         * <p><code>bool debug_redact = 3;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 3)
        boolean debugRedact;

        /**
         * Information about the support window of a feature value.
         *
         * <p><code>FieldOptions.FeatureSupport feature_support = 4;</code>
         */
        @ProtobufMessage.MessageField(index = 4)
        DescriptorProtos.FieldOptions.FeatureSupport featureSupport;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        EnumValueOptions(
                boolean deprecated,
                DescriptorProtos.FeatureSet features,
                boolean debugRedact,
                DescriptorProtos.FieldOptions.FeatureSupport featureSupport,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.deprecated = deprecated;
            this.features = features;
            this.debugRedact = debugRedact;
            this.featureSupport = featureSupport;
            this.uninterpretedOption = uninterpretedOption;
        }

        public boolean deprecated() {
            return deprecated;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public boolean debugRedact() {
            return debugRedact;
        }

        public DescriptorProtos.FieldOptions.FeatureSupport featureSupport() {
            return featureSupport;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.ServiceOptions}
     */
    @ProtobufMessage
    public static final class ServiceOptions {
        // proto: extensions 1000 to max

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 34;</code>
         */
        @ProtobufMessage.MessageField(index = 34)
        DescriptorProtos.FeatureSet features;

        /**
         * Is this service deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for the service, or it will be completely ignored; in the very least,
         * this is a formalization for deprecating services.
         *
         * <p><code>bool deprecated = 33;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 33)
        boolean deprecated;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        ServiceOptions(
                DescriptorProtos.FeatureSet features,
                boolean deprecated,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.features = features;
            this.deprecated = deprecated;
            this.uninterpretedOption = uninterpretedOption;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public boolean deprecated() {
            return deprecated;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * <p>Protobuf type {@code google.protobuf.MethodOptions}
     */
    @ProtobufMessage
    public static final class MethodOptions {
        // proto: extensions 1000 to max

        /**
         * Is this method side-effect-free (or safe in HTTP parlance), or idempotent,
         * or neither? HTTP based RPC implementation may choose GET verb for safe
         * methods, and PUT verb for idempotent methods instead of the default POST.
         *
         * <p>Protobuf enum {@code google.protobuf.MethodOptions.IdempotencyLevel}
         */
        @ProtobufEnum
        public enum IdempotencyLevel {
            @ProtobufEnum.Constant(index = 0)
            IDEMPOTENCY_UNKNOWN,

            /**
             * implies idempotent
             */
            @ProtobufEnum.Constant(index = 1)
            NO_SIDE_EFFECTS,

            /**
             * idempotent, but may have side effects
             */
            @ProtobufEnum.Constant(index = 2)
            IDEMPOTENT;
        }

        /**
         * Is this method deprecated?
         * Depending on the target platform, this can emit Deprecated annotations
         * for the method, or it will be completely ignored; in the very least,
         * this is a formalization for deprecating methods.
         *
         * <p><code>bool deprecated = 33;</code>
         */
        // proto default: false
        @ProtobufMessage.BoolField(index = 33)
        boolean deprecated;

        /**
         * <p><code>IdempotencyLevel idempotency_level = 34;</code>
         */
        // proto default: IDEMPOTENCY_UNKNOWN
        @ProtobufMessage.EnumField(index = 34)
        DescriptorProtos.MethodOptions.IdempotencyLevel idempotencyLevel;

        /**
         * Any features defined in the specific edition.
         * WARNING: This field should only be used by protobuf plugins or special
         * cases like the proto compiler. Other uses are discouraged and
         * developers should rely on the protoreflect APIs for their client language.
         *
         * <p><code>FeatureSet features = 35;</code>
         */
        @ProtobufMessage.MessageField(index = 35)
        DescriptorProtos.FeatureSet features;

        /**
         * The parser stores options it doesn't recognize here. See above.
         *
         * <p><code>repeated UninterpretedOption uninterpreted_option = 999;</code>
         */
        @ProtobufMessage.MessageField(index = 999)
        List<DescriptorProtos.UninterpretedOption> uninterpretedOption;

        MethodOptions(
                boolean deprecated,
                DescriptorProtos.MethodOptions.IdempotencyLevel idempotencyLevel,
                DescriptorProtos.FeatureSet features,
                List<DescriptorProtos.UninterpretedOption> uninterpretedOption
        ) {
            this.deprecated = deprecated;
            this.idempotencyLevel = idempotencyLevel;
            this.features = features;
            this.uninterpretedOption = uninterpretedOption;
        }

        public boolean deprecated() {
            return deprecated;
        }

        public DescriptorProtos.MethodOptions.IdempotencyLevel idempotencyLevel() {
            return idempotencyLevel;
        }

        public DescriptorProtos.FeatureSet features() {
            return features;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption> uninterpretedOption() {
            return Collections.unmodifiableSequencedCollection(uninterpretedOption);
        }
    }

    /**
     * A message representing a option the parser does not recognize. This only
     * appears in options protos created by the compiler::Parser class.
     * DescriptorPool resolves these when building Descriptor objects. Therefore,
     * options protos in descriptor objects (e.g. returned by Descriptor::options(),
     * or produced by Descriptor::CopyTo()) will never have UninterpretedOptions
     * in them.
     *
     * <p>Protobuf type {@code google.protobuf.UninterpretedOption}
     */
    @ProtobufMessage
    public static final class UninterpretedOption {

        /**
         * The name of the uninterpreted option.  Each string represents a segment in
         * a dot-separated name.  is_extension is true iff a segment represents an
         * extension (denoted with parentheses in options specs in .proto files).
         * E.g.,{ ["foo", false], ["bar.baz", true], ["moo", false] } represents
         * "foo.(bar.baz).moo".
         *
         * <p>Protobuf type {@code google.protobuf.UninterpretedOption.NamePart}
         */
        @ProtobufMessage
        public static final class NamePart {

            /**
             * <p><code>required string name_part = 1;</code>
             */
            @ProtobufMessage.StringField(index = 1)
            String namePart;

            /**
             * <p><code>required bool is_extension = 2;</code>
             */
            @ProtobufMessage.BoolField(index = 2)
            boolean isExtension;

            NamePart(String namePart, boolean isExtension) {
                this.namePart = namePart;
                this.isExtension = isExtension;
            }

            public String namePart() {
                return namePart;
            }

            public boolean isExtension() {
                return isExtension;
            }
        }

        /**
         * <p><code>repeated NamePart name = 2;</code>
         */
        @ProtobufMessage.MessageField(index = 2)
        List<DescriptorProtos.UninterpretedOption.NamePart> name;

        /**
         * The value of the uninterpreted option, in whatever type the tokenizer
         * identified it as during parsing. Exactly one of these should be set.
         *
         * <p><code>string identifier_value = 3;</code>
         */
        @ProtobufMessage.StringField(index = 3)
        String identifierValue;

        /**
         * <p><code>uint64 positive_int_value = 4;</code>
         */
        @ProtobufMessage.Uint64Field(index = 4)
        long positiveIntValue;

        /**
         * <p><code>int64 negative_int_value = 5;</code>
         */
        @ProtobufMessage.Int64Field(index = 5)
        long negativeIntValue;

        /**
         * <p><code>double double_value = 6;</code>
         */
        @ProtobufMessage.DoubleField(index = 6)
        double doubleValue;

        /**
         * <p><code>bytes string_value = 7;</code>
         */
        @ProtobufMessage.BytesField(index = 7)
        byte[] stringValue;

        /**
         * <p><code>string aggregate_value = 8;</code>
         */
        @ProtobufMessage.StringField(index = 8)
        String aggregateValue;

        UninterpretedOption(
                List<DescriptorProtos.UninterpretedOption.NamePart> name,
                String identifierValue,
                long positiveIntValue,
                long negativeIntValue,
                double doubleValue,
                byte[] stringValue,
                String aggregateValue
        ) {
            this.name = name;
            this.identifierValue = identifierValue;
            this.positiveIntValue = positiveIntValue;
            this.negativeIntValue = negativeIntValue;
            this.doubleValue = doubleValue;
            this.stringValue = stringValue;
            this.aggregateValue = aggregateValue;
        }

        public SequencedCollection<DescriptorProtos.UninterpretedOption.NamePart> name() {
            return Collections.unmodifiableSequencedCollection(name);
        }

        public String identifierValue() {
            return identifierValue;
        }

        public long positiveIntValue() {
            return positiveIntValue;
        }

        public long negativeIntValue() {
            return negativeIntValue;
        }

        public double doubleValue() {
            return doubleValue;
        }

        public byte[] stringValue() {
            return stringValue;
        }

        public String aggregateValue() {
            return aggregateValue;
        }
    }

    /**
     * TODO Enums in C++ gencode (and potentially other languages) are
     * not well scoped.  This means that each of the feature enums below can clash
     * with each other.  The short names we've chosen maximize call-site
     * readability, but leave us very open to this scenario.  A future feature will
     * be designed and implemented to handle this, hopefully before we ever hit a
     * conflict here.
     *
     * <p>Protobuf type {@code google.protobuf.FeatureSet}
     */
    @ProtobufMessage(reservedIndexes = {999})
    public static final class FeatureSet {
        // proto: extensions 1000 to 9994 [ declaration = { number: 1000, full_name: ".pb.cpp", type: ".pb.CppFeatures" }, declaration = { number: 1001, full_name: ".pb.java", type: ".pb.JavaFeatures" }, declaration = { number: 1002, full_name: ".pb.go", type: ".pb.GoFeatures" }, declaration = { number: 1003, full_name: ".pb.python", type: ".pb.PythonFeatures" }, declaration = { number: 1004, full_name: ".pb.csharp", type: ".pb.CSharpFeatures" }, declaration = { number: 1100, full_name: ".imp.impress_feature_set", type: ".imp.ImpressFeatureSet" }, declaration = { number: 9989, full_name: ".pb.java_mutable", type: ".pb.JavaMutableFeatures" }, declaration = { number: 9990, full_name: ".pb.proto1", type: ".pb.Proto1Features" } ]
        // proto: extensions 9995 to 9999;  // For internal testing
        // proto: extensions 10000;         // for https://github.com/bufbuild/protobuf-es

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.FieldPresence}
         */
        @ProtobufEnum
        public enum FieldPresence {
            @ProtobufEnum.Constant(index = 0)
            FIELD_PRESENCE_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            EXPLICIT,

            @ProtobufEnum.Constant(index = 2)
            IMPLICIT,

            @ProtobufEnum.Constant(index = 3)
            LEGACY_REQUIRED;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.EnumType}
         */
        @ProtobufEnum
        public enum EnumType {
            @ProtobufEnum.Constant(index = 0)
            ENUM_TYPE_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            OPEN,

            @ProtobufEnum.Constant(index = 2)
            CLOSED;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.RepeatedFieldEncoding}
         */
        @ProtobufEnum
        public enum RepeatedFieldEncoding {
            @ProtobufEnum.Constant(index = 0)
            REPEATED_FIELD_ENCODING_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            PACKED,

            @ProtobufEnum.Constant(index = 2)
            EXPANDED;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.Utf8Validation}
         */
        @ProtobufEnum(reservedIndexes = {1})
        public enum Utf8Validation {
            @ProtobufEnum.Constant(index = 0)
            UTF8_VALIDATION_UNKNOWN,

            @ProtobufEnum.Constant(index = 2)
            VERIFY,

            @ProtobufEnum.Constant(index = 3)
            NONE;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.MessageEncoding}
         */
        @ProtobufEnum
        public enum MessageEncoding {
            @ProtobufEnum.Constant(index = 0)
            MESSAGE_ENCODING_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            LENGTH_PREFIXED,

            @ProtobufEnum.Constant(index = 2)
            DELIMITED;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.JsonFormat}
         */
        @ProtobufEnum
        public enum JsonFormat {
            @ProtobufEnum.Constant(index = 0)
            JSON_FORMAT_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            ALLOW,

            @ProtobufEnum.Constant(index = 2)
            LEGACY_BEST_EFFORT;
        }

        /**
         * <p>Protobuf enum {@code google.protobuf.FeatureSet.EnforceNamingStyle}
         */
        @ProtobufEnum
        public enum EnforceNamingStyle {
            @ProtobufEnum.Constant(index = 0)
            ENFORCE_NAMING_STYLE_UNKNOWN,

            @ProtobufEnum.Constant(index = 1)
            STYLE2024,

            @ProtobufEnum.Constant(index = 2)
            STYLE_LEGACY;
        }

        /**
         * <p>Protobuf type {@code google.protobuf.FeatureSet.VisibilityFeature}
         */
        @ProtobufMessage(reservedRanges = {@ProtobufReservedRange(min = 1, max = 536870911)})
        public static final class VisibilityFeature {

            /**
             * <p>Protobuf enum {@code google.protobuf.FeatureSet.VisibilityFeature.DefaultSymbolVisibility}
             */
            @ProtobufEnum
            public enum DefaultSymbolVisibility {
                @ProtobufEnum.Constant(index = 0)
                DEFAULT_SYMBOL_VISIBILITY_UNKNOWN,

                /**
                 * Default pre-EDITION_2024, all UNSET visibility are export.
                 */
                @ProtobufEnum.Constant(index = 1)
                EXPORT_ALL,

                /**
                 * All top-level symbols default to export, nested default to local.
                 */
                @ProtobufEnum.Constant(index = 2)
                EXPORT_TOP_LEVEL,

                /**
                 * All symbols default to local.
                 */
                @ProtobufEnum.Constant(index = 3)
                LOCAL_ALL,

                /**
                 * All symbols local by default. Nested types cannot be exported.
                 * With special case caveat for message { enum {} reserved 1 to max; }
                 * This is the recommended setting for new protos.
                 */
                @ProtobufEnum.Constant(index = 4)
                STRICT;
            }
        }

        /**
         * <p><code>FieldPresence field_presence = 1;</code>
         */
        @ProtobufMessage.EnumField(index = 1)
        DescriptorProtos.FeatureSet.FieldPresence fieldPresence;

        /**
         * <p><code>EnumType enum_type = 2;</code>
         */
        @ProtobufMessage.EnumField(index = 2)
        DescriptorProtos.FeatureSet.EnumType enumType;

        /**
         * <p><code>RepeatedFieldEncoding repeated_field_encoding = 3;</code>
         */
        @ProtobufMessage.EnumField(index = 3)
        DescriptorProtos.FeatureSet.RepeatedFieldEncoding repeatedFieldEncoding;

        /**
         * <p><code>Utf8Validation utf8_validation = 4;</code>
         */
        @ProtobufMessage.EnumField(index = 4)
        JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation;

        /**
         * <p><code>MessageEncoding message_encoding = 5;</code>
         */
        @ProtobufMessage.EnumField(index = 5)
        DescriptorProtos.FeatureSet.MessageEncoding messageEncoding;

        /**
         * <p><code>JsonFormat json_format = 6;</code>
         */
        @ProtobufMessage.EnumField(index = 6)
        DescriptorProtos.FeatureSet.JsonFormat jsonFormat;

        /**
         * <p><code>EnforceNamingStyle enforce_naming_style = 7;</code>
         */
        @ProtobufMessage.EnumField(index = 7)
        DescriptorProtos.FeatureSet.EnforceNamingStyle enforceNamingStyle;

        FeatureSet(
                DescriptorProtos.FeatureSet.FieldPresence fieldPresence,
                DescriptorProtos.FeatureSet.EnumType enumType,
                DescriptorProtos.FeatureSet.RepeatedFieldEncoding repeatedFieldEncoding,
                JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation,
                DescriptorProtos.FeatureSet.MessageEncoding messageEncoding,
                DescriptorProtos.FeatureSet.JsonFormat jsonFormat,
                DescriptorProtos.FeatureSet.EnforceNamingStyle enforceNamingStyle
        ) {
            this.fieldPresence = fieldPresence;
            this.enumType = enumType;
            this.repeatedFieldEncoding = repeatedFieldEncoding;
            this.utf8Validation = utf8Validation;
            this.messageEncoding = messageEncoding;
            this.jsonFormat = jsonFormat;
            this.enforceNamingStyle = enforceNamingStyle;
        }

        public DescriptorProtos.FeatureSet.FieldPresence fieldPresence() {
            return fieldPresence;
        }

        public DescriptorProtos.FeatureSet.EnumType enumType() {
            return enumType;
        }

        public DescriptorProtos.FeatureSet.RepeatedFieldEncoding repeatedFieldEncoding() {
            return repeatedFieldEncoding;
        }

        public JavaFeaturesProto.JavaFeatures.Utf8Validation utf8Validation() {
            return utf8Validation;
        }

        public DescriptorProtos.FeatureSet.MessageEncoding messageEncoding() {
            return messageEncoding;
        }

        public DescriptorProtos.FeatureSet.JsonFormat jsonFormat() {
            return jsonFormat;
        }

        public DescriptorProtos.FeatureSet.EnforceNamingStyle enforceNamingStyle() {
            return enforceNamingStyle;
        }
    }

    /**
     * A compiled specification for the defaults of a set of features.  These
     * messages are generated from FeatureSet extensions and can be used to seed
     * feature resolution. The resolution with this object becomes a simple search
     * for the closest matching edition, followed by proto merges.
     *
     * <p>Protobuf type {@code google.protobuf.FeatureSetDefaults}
     */
    @ProtobufMessage
    public static final class FeatureSetDefaults {

        /**
         * A map from every known edition with a unique set of defaults to its
         * defaults. Not all editions may be contained here.  For a given edition,
         * the defaults at the closest matching edition ordered at or before it should
         * be used.  This field must be in strict ascending order by edition.
         *
         * <p>Protobuf type {@code google.protobuf.FeatureSetDefaults.FeatureSetEditionDefault}
         */
        @ProtobufMessage(reservedIndexes = {1, 2}, reservedNames = {"features"})
        public static final class FeatureSetEditionDefault {

            /**
             * <p><code>Edition edition = 3;</code>
             */
            @ProtobufMessage.EnumField(index = 3)
            DescriptorProtos.Edition edition;

            /**
             * Defaults of features that can be overridden in this edition.
             *
             * <p><code>FeatureSet overridable_features = 4;</code>
             */
            @ProtobufMessage.MessageField(index = 4)
            DescriptorProtos.FeatureSet overridableFeatures;

            /**
             * Defaults of features that can't be overridden in this edition.
             *
             * <p><code>FeatureSet fixed_features = 5;</code>
             */
            @ProtobufMessage.MessageField(index = 5)
            DescriptorProtos.FeatureSet fixedFeatures;

            FeatureSetEditionDefault(
                    DescriptorProtos.Edition edition,
                    DescriptorProtos.FeatureSet overridableFeatures,
                    DescriptorProtos.FeatureSet fixedFeatures
            ) {
                this.edition = edition;
                this.overridableFeatures = overridableFeatures;
                this.fixedFeatures = fixedFeatures;
            }

            public DescriptorProtos.Edition edition() {
                return edition;
            }

            public DescriptorProtos.FeatureSet overridableFeatures() {
                return overridableFeatures;
            }

            public DescriptorProtos.FeatureSet fixedFeatures() {
                return fixedFeatures;
            }
        }

        /**
         * <p><code>repeated FeatureSetEditionDefault defaults = 1;</code>
         */
        @ProtobufMessage.MessageField(index = 1)
        List<DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> defaults;

        /**
         * The minimum supported edition (inclusive) when this was constructed.
         * Editions before this will not have defaults.
         *
         * <p><code>Edition minimum_edition = 4;</code>
         */
        @ProtobufMessage.EnumField(index = 4)
        DescriptorProtos.Edition minimumEdition;

        /**
         * The maximum known edition (inclusive) when this was constructed. Editions
         * after this will not have reliable defaults.
         *
         * <p><code>Edition maximum_edition = 5;</code>
         */
        @ProtobufMessage.EnumField(index = 5)
        DescriptorProtos.Edition maximumEdition;

        FeatureSetDefaults(
                List<DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> defaults,
                DescriptorProtos.Edition minimumEdition,
                DescriptorProtos.Edition maximumEdition
        ) {
            this.defaults = defaults;
            this.minimumEdition = minimumEdition;
            this.maximumEdition = maximumEdition;
        }

        public SequencedCollection<DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> defaults() {
            return Collections.unmodifiableSequencedCollection(defaults);
        }

        public DescriptorProtos.Edition minimumEdition() {
            return minimumEdition;
        }

        public DescriptorProtos.Edition maximumEdition() {
            return maximumEdition;
        }
    }

    /**
     * Encapsulates information about the original source file from which a
     * FileDescriptorProto was generated.
     *
     * <p>Protobuf type {@code google.protobuf.SourceCodeInfo}
     */
    @ProtobufMessage
    public static final class SourceCodeInfo {
        // proto: extensions 536000000 [declaration = { number: 536000000 type: ".buf.descriptor.v1.SourceCodeInfoExtension" full_name: ".buf.descriptor.v1.buf_source_code_info_extension" }]

        /**
         * <p>Protobuf type {@code google.protobuf.SourceCodeInfo.Location}
         */
        @ProtobufMessage
        public static final class Location {

            /**
             * Identifies which part of the FileDescriptorProto was defined at this
             * location.
             *
             * <p>Each element is a field number or an index.  They form a path from
             * the root FileDescriptorProto to the place where the definition appears.
             * For example, this path:
             *
             * <pre>
             * [ 4, 3, 2, 7, 1 ]
             * </pre>
             *
             * <p>refers to:
             *
             * <pre>
             * file.message_type(3)  // 4, 3
             *     .field(7)         // 2, 7
             *     .name()           // 1
             * </pre>
             *
             * <p>This is because FileDescriptorProto.message_type has field number 4:
             *
             * <pre>
             * repeated DescriptorProto message_type = 4;
             * </pre>
             *
             * <p>and DescriptorProto.field has field number 2:
             *
             * <pre>
             * repeated FieldDescriptorProto field = 2;
             * </pre>
             *
             * <p>and FieldDescriptorProto.name has field number 1:
             *
             * <pre>
             * optional string name = 1;
             * </pre>
             *
             * <p>Thus, the above path gives the location of a field name.  If we removed
             * the last element:
             *
             * <pre>
             * [ 4, 3, 2, 7 ]
             * </pre>
             *
             * <p>this path refers to the whole field declaration (from the beginning
             * of the label to the terminating semicolon).
             *
             * <p><code>repeated int32 path = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            List<Integer> path;

            /**
             * Always has exactly three or four elements: start line, start column,
             * end line (optional, otherwise assumed same as start line), end column.
             * These are packed into a single field for efficiency.  Note that line
             * and column numbers are zero-based -- typically you will want to add
             * 1 to each before displaying to a user.
             *
             * <p><code>repeated int32 span = 2;</code>
             */
            @ProtobufMessage.Int32Field(index = 2)
            List<Integer> span;

            /**
             * If this SourceCodeInfo represents a complete declaration, these are any
             * comments appearing before and after the declaration which appear to be
             * attached to the declaration.
             *
             * <p>A series of line comments appearing on consecutive lines, with no other
             * tokens appearing on those lines, will be treated as a single comment.
             *
             * <p>leading_detached_comments will keep paragraphs of comments that appear
             * before (but not connected to) the current element. Each paragraph,
             * separated by empty lines, will be one comment element in the repeated
             * field.
             *
             * <p>Only the comment content is provided; comment markers (e.g. //) are
             * stripped out.  For block comments, leading whitespace and an asterisk
             * will be stripped from the beginning of each line other than the first.
             * Newlines are included in the output.
             *
             * <p>Examples:
             *
             * <pre>
             * optional int32 foo = 1;  // Comment attached to foo.
             * // Comment attached to bar.
             * optional int32 bar = 2;
             *
             * optional string baz = 3;
             * // Comment attached to baz.
             * // Another line attached to baz.
             *
             * // Comment attached to moo.
             * //
             * // Another line attached to moo.
             * optional double moo = 4;
             *
             * // Detached comment for corge. This is not leading or trailing comments
             * // to moo or corge because there are blank lines separating it from
             * // both.
             *
             * // Detached comment for corge paragraph 2.
             *
             * optional string corge = 5;
             *
             *
             * optional int32 grault = 6;
             *
             * // ignored detached comments.
             * </pre>
             *
             * <p><code>string leading_comments = 3;</code>
             */
            @ProtobufMessage.StringField(index = 3)
            String leadingComments;

            /**
             * <p><code>string trailing_comments = 4;</code>
             */
            @ProtobufMessage.StringField(index = 4)
            String trailingComments;

            /**
             * <p><code>repeated string leading_detached_comments = 6;</code>
             */
            @ProtobufMessage.StringField(index = 6)
            List<String> leadingDetachedComments;

            Location(
                    List<Integer> path,
                    List<Integer> span,
                    String leadingComments,
                    String trailingComments,
                    List<String> leadingDetachedComments
            ) {
                this.path = path;
                this.span = span;
                this.leadingComments = leadingComments;
                this.trailingComments = trailingComments;
                this.leadingDetachedComments = leadingDetachedComments;
            }

            public SequencedCollection<Integer> path() {
                return Collections.unmodifiableSequencedCollection(path);
            }

            public SequencedCollection<Integer> span() {
                return Collections.unmodifiableSequencedCollection(span);
            }

            public String leadingComments() {
                return leadingComments;
            }

            public String trailingComments() {
                return trailingComments;
            }

            public SequencedCollection<String> leadingDetachedComments() {
                return Collections.unmodifiableSequencedCollection(leadingDetachedComments);
            }
        }

        /**
         * A Location identifies a piece of source code in a .proto file which
         * corresponds to a particular definition.  This information is intended
         * to be useful to IDEs, code indexers, documentation generators, and similar
         * tools.
         *
         * <p>For example, say we have a file like:
         *
         * <pre>
         * message Foo {
         *   optional string foo = 1;
         * }
         * </pre>
         *
         * <p>Let's look at just the field definition:
         *
         * <pre>
         * optional string foo = 1;
         * ^       ^^     ^^  ^  ^^^
         * a       bc     de  f  ghi
         * </pre>
         *
         * <p>We have the following locations:
         *
         * <pre>
         * span   path               represents
         * [a,i)  [ 4, 0, 2, 0 ]     The whole field definition.
         * [a,b)  [ 4, 0, 2, 0, 4 ]  The label (optional).
         * [c,d)  [ 4, 0, 2, 0, 5 ]  The type (string).
         * [e,f)  [ 4, 0, 2, 0, 1 ]  The name (foo).
         * [g,h)  [ 4, 0, 2, 0, 3 ]  The number (1).
         * </pre>
         *
         * <p>Notes:
         * - A location may refer to a repeated field itself (i.e. not to any
         *
         * <pre>
         * particular index within it).  This is used whenever a set of elements are
         * logically enclosed in a single code segment.  For example, an entire
         * extend block (possibly containing multiple extension definitions) will
         * have an outer location whose path refers to the "extensions" repeated
         * field without an index.
         * </pre>
         *
         * <p>- Multiple locations may have the same path.  This happens when a single
         *
         * <pre>
         * logical declaration is spread out across multiple places.  The most
         * obvious example is the "extend" block again -- there may be multiple
         * extend blocks in the same scope, each of which will have the same path.
         * </pre>
         *
         * <p>- A location's span is not always a subset of its parent's span.  For
         *
         * <pre>
         * example, the "extendee" of an extension declaration appears at the
         * beginning of the "extend" block and is shared by all extensions within
         * the block.
         * </pre>
         *
         * <p>- Just because a location's span is a subset of some other location's span
         *
         * <pre>
         * does not mean that it is a descendant.  For example, a "group" defines
         * both a type and a field in a single declaration.  Thus, the locations
         * corresponding to the type and field and their components will overlap.
         * </pre>
         *
         * <p>- Code which tries to interpret locations should probably be designed to
         *
         * <pre>
         * ignore those that it doesn't understand, as more types of locations could
         * be recorded in the future.
         * </pre>
         *
         * <p><code>repeated Location location = 1;</code>
         */
        @ProtobufMessage.MessageField(index = 1)
        List<DescriptorProtos.SourceCodeInfo.Location> location;

        SourceCodeInfo(List<DescriptorProtos.SourceCodeInfo.Location> location) {
            this.location = location;
        }

        public SequencedCollection<DescriptorProtos.SourceCodeInfo.Location> location() {
            return Collections.unmodifiableSequencedCollection(location);
        }
    }

    /**
     * Describes the relationship between generated code and its original source
     * file. A GeneratedCodeInfo message is associated with only one generated
     * source file, but may contain references to different source .proto files.
     *
     * <p>Protobuf type {@code google.protobuf.GeneratedCodeInfo}
     */
    @ProtobufMessage
    public static final class GeneratedCodeInfo {

        /**
         * <p>Protobuf type {@code google.protobuf.GeneratedCodeInfo.Annotation}
         */
        @ProtobufMessage
        public static final class Annotation {

            /**
             * Represents the identified object's effect on the element in the original
             * .proto file.
             *
             * <p>Protobuf enum {@code google.protobuf.GeneratedCodeInfo.Annotation.Semantic}
             */
            @ProtobufEnum
            public enum Semantic {
                /**
                 * There is no effect or the effect is indescribable.
                 */
                @ProtobufEnum.Constant(index = 0)
                NONE,

                /**
                 * The element is set or otherwise mutated.
                 */
                @ProtobufEnum.Constant(index = 1)
                SET,

                /**
                 * An alias to the element is returned.
                 */
                @ProtobufEnum.Constant(index = 2)
                ALIAS;
            }

            /**
             * Identifies the element in the original source .proto file. This field
             * is formatted the same as SourceCodeInfo.Location.path.
             *
             * <p><code>repeated int32 path = 1;</code>
             */
            @ProtobufMessage.Int32Field(index = 1)
            List<Integer> path;

            /**
             * Identifies the filesystem path to the original source .proto.
             *
             * <p><code>string source_file = 2;</code>
             */
            @ProtobufMessage.StringField(index = 2)
            String sourceFile;

            /**
             * Identifies the starting offset in bytes in the generated code
             * that relates to the identified object.
             *
             * <p><code>int32 begin = 3;</code>
             */
            @ProtobufMessage.Int32Field(index = 3)
            int begin;

            /**
             * Identifies the ending offset in bytes in the generated code that
             * relates to the identified object. The end offset should be one past
             * the last relevant byte (so the length of the text = end - begin).
             *
             * <p><code>int32 end = 4;</code>
             */
            @ProtobufMessage.Int32Field(index = 4)
            int end;

            /**
             * <p><code>Semantic semantic = 5;</code>
             */
            @ProtobufMessage.EnumField(index = 5)
            DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic semantic;

            Annotation(
                    List<Integer> path,
                    String sourceFile,
                    int begin,
                    int end,
                    DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic semantic
            ) {
                this.path = path;
                this.sourceFile = sourceFile;
                this.begin = begin;
                this.end = end;
                this.semantic = semantic;
            }

            public SequencedCollection<Integer> path() {
                return Collections.unmodifiableSequencedCollection(path);
            }

            public String sourceFile() {
                return sourceFile;
            }

            public int begin() {
                return begin;
            }

            public int end() {
                return end;
            }

            public DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic semantic() {
                return semantic;
            }
        }

        /**
         * An Annotation connects some span of text in generated code to an element
         * of its generating .proto file.
         *
         * <p><code>repeated Annotation annotation = 1;</code>
         */
        @ProtobufMessage.MessageField(index = 1)
        List<DescriptorProtos.GeneratedCodeInfo.Annotation> annotation;

        GeneratedCodeInfo(List<DescriptorProtos.GeneratedCodeInfo.Annotation> annotation) {
            this.annotation = annotation;
        }

        public SequencedCollection<DescriptorProtos.GeneratedCodeInfo.Annotation> annotation() {
            return Collections.unmodifiableSequencedCollection(annotation);
        }
    }
}

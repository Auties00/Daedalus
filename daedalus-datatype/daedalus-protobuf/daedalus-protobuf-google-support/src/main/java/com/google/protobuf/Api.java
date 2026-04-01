package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Api is a light-weight descriptor for an API Interface.
 *
 * <p>Interfaces are also described as "protocol buffer services" in some contexts,
 * such as by the "service" keyword in a .proto file, but they are different
 * from API Services, which represent a concrete implementation of an interface
 * as opposed to simply a description of methods and bindings. They are also
 * sometimes simply referred to as "APIs" in other contexts, such as the name of
 * this message itself. See <a href="https://cloud.google.com/apis/design/glossary">https://cloud.google.com/apis/design/glossary</a> for
 * detailed terminology.
 *
 * <p>New usages of this message as an alternative to ServiceDescriptorProto are
 * strongly discouraged. This message does not reliability preserve all
 * information necessary to model the schema and preserve semantics. Instead
 * make use of FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.Api}
 */
@ProtobufMessage
public final class Api {

    /**
     * The fully qualified name of this interface, including package name
     * followed by the interface's simple name.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * The methods of this interface, in unspecified order.
     *
     * <p><code>repeated Method methods = 2;</code>
     */
    @ProtobufMessage.MessageField(index = 2)
    List<Method> methods;

    /**
     * Any metadata attached to the interface.
     *
     * <p><code>repeated Option options = 3;</code>
     */
    @ProtobufMessage.MessageField(index = 3)
    List<Option> options;

    /**
     * A version string for this interface. If specified, must have the form
     * {@code major-version.minor-version}, as in {@code 1.10}. If the minor version is
     * omitted, it defaults to zero. If the entire version field is empty, the
     * major version is derived from the package name, as outlined below. If the
     * field is not empty, the version in the package name will be verified to be
     * consistent with what is provided here.
     *
     * <p>The versioning schema uses [semantic
     * versioning](<a href="http://semver.org">http://semver.org</a>) where the major version number
     * indicates a breaking change and the minor version an additive,
     * non-breaking change. Both version numbers are signals to users
     * what to expect from different versions, and should be carefully
     * chosen based on the product plan.
     *
     * <p>The major version is also reflected in the package name of the
     * interface, which must end in {@code v<major-version>}, as in
     * {@code google.feature.v1}. For major versions 0 and 1, the suffix can
     * be omitted. Zero major versions must only be used for
     * experimental, non-GA interfaces.
     *
     * <p><code>string version = 4;</code>
     */
    @ProtobufMessage.StringField(index = 4)
    String version;

    /**
     * Source context for the protocol buffer service represented by this
     * message.
     *
     * <p><code>SourceContext source_context = 5;</code>
     */
    @ProtobufMessage.MessageField(index = 5)
    SourceContext sourceContext;

    /**
     * Included interfaces. See [Mixin][].
     *
     * <p><code>repeated Mixin mixins = 6;</code>
     */
    @ProtobufMessage.MessageField(index = 6)
    List<Mixin> mixins;

    /**
     * The source syntax of the service.
     *
     * <p><code>Syntax syntax = 7;</code>
     */
    @ProtobufMessage.EnumField(index = 7)
    Syntax syntax;

    /**
     * The source edition string, only valid when syntax is SYNTAX_EDITIONS.
     *
     * <p><code>string edition = 8;</code>
     */
    @ProtobufMessage.StringField(index = 8)
    String edition;

    Api(
            String name,
            List<Method> methods,
            List<Option> options,
            String version,
            SourceContext sourceContext,
            List<Mixin> mixins,
            Syntax syntax,
            String edition
    ) {
        this.name = name;
        this.methods = methods;
        this.options = options;
        this.version = version;
        this.sourceContext = sourceContext;
        this.mixins = mixins;
        this.syntax = syntax;
        this.edition = edition;
    }

    public String name() {
        return name;
    }

    public SequencedCollection<Method> methods() {
        return Collections.unmodifiableSequencedCollection(methods);
    }

    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }

    public String version() {
        return version;
    }

    public SourceContext sourceContext() {
        return sourceContext;
    }

    public SequencedCollection<Mixin> mixins() {
        return Collections.unmodifiableSequencedCollection(mixins);
    }

    public Syntax syntax() {
        return syntax;
    }

    public String edition() {
        return edition;
    }
}

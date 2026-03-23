package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Declares an API Interface to be included in this interface. The including
 * interface must redeclare all the methods from the included interface, but
 * documentation and options are inherited as follows:
 *
 * <p>- If after comment and whitespace stripping, the documentation
 *
 * <pre>
 * string of the redeclared method is empty, it will be inherited
 * from the original method.
 * </pre>
 *
 * <p>- Each annotation belonging to the service config (http,
 *
 * <pre>
 * visibility) which is not set in the redeclared method will be
 * inherited.
 * </pre>
 *
 * <p>- If an http annotation is inherited, the path pattern will be
 *
 * <pre>
 * modified as follows. Any version prefix will be replaced by the
 * version of the including interface plus the [root][] path if
 * specified.
 * </pre>
 *
 * <p>Example of a simple mixin:
 *
 * <pre>
 *   package google.acl.v1;
 *   service AccessControl {
 *     // Get the underlying ACL object.
 *     rpc GetAcl(GetAclRequest) returns (Acl) {
 *       option (google.api.http).get = "/v1/{resource=**}:getAcl";
 *     }
 *   }
 *
 *   package google.storage.v2;
 *   service Storage {
 *     rpc GetAcl(GetAclRequest) returns (Acl);
 *
 *     // Get a data record.
 *     rpc GetData(GetDataRequest) returns (Data) {
 *       option (google.api.http).get = "/v2/{resource=**}";
 *     }
 *   }
 * </pre>
 *
 * <p>Example of a mixin configuration:
 *
 * <pre>
 *   apis:
 *   - name: google.storage.v2.Storage
 *     mixins:
 *     - name: google.acl.v1.AccessControl
 * </pre>
 *
 * <p>The mixin construct implies that all methods in {@code AccessControl} are
 * also declared with same name and request/response types in
 * {@code Storage}. A documentation generator or annotation processor will
 * see the effective {@code Storage.GetAcl} method after inheriting
 * documentation and annotations as follows:
 *
 * <pre>
 *   service Storage {
 *     // Get the underlying ACL object.
 *     rpc GetAcl(GetAclRequest) returns (Acl) {
 *       option (google.api.http).get = "/v2/{resource=**}:getAcl";
 *     }
 *     ...
 *   }
 * </pre>
 *
 * <p>Note how the version in the path pattern changed from {@code v1} to {@code v2}.
 *
 * <p>If the {@code root} field in the mixin is specified, it should be a
 * relative path under which inherited HTTP paths are placed. Example:
 *
 * <pre>
 *   apis:
 *   - name: google.storage.v2.Storage
 *     mixins:
 *     - name: google.acl.v1.AccessControl
 *       root: acls
 * </pre>
 *
 * <p>This implies the following inherited HTTP annotation:
 *
 * <pre>
 *   service Storage {
 *     // Get the underlying ACL object.
 *     rpc GetAcl(GetAclRequest) returns (Acl) {
 *       option (google.api.http).get = "/v2/acls/{resource=**}:getAcl";
 *     }
 *     ...
 *   }
 * </pre>
 *
 * <p>Protobuf type {@code google.protobuf.Mixin}
 */
@ProtobufMessage
public final class Mixin {

    /**
     * The fully qualified name of the interface which is included.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * If non-empty specifies a path under which inherited HTTP paths
     * are rooted.
     *
     * <p><code>string root = 2;</code>
     */
    @ProtobufMessage.StringField(index = 2)
    String root;

    Mixin(String name, String root) {
        this.name = name;
        this.root = root;
    }

    public String name() {
        return name;
    }

    public String root() {
        return root;
    }
}

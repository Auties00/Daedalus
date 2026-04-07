package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * {@code FieldMask} represents a set of symbolic field paths, for example:
 *
 * <pre>
 *   paths: "f.a"
 *   paths: "f.b.d"
 * </pre>
 *
 * <p>Here {@code f} represents a field in some root message, {@code a} and {@code b}
 * fields in the message found in {@code f}, and {@code d} a field found in the
 * message in {@code f.b}.
 *
 * <p>Field masks are used to specify a subset of fields that should be
 * returned by a get operation or modified by an update operation.
 * Field masks also have a custom JSON encoding (see below).
 *
 * <h2>Field Masks in Projections</h2>
 *
 *
 * <p>When used in the context of a projection, a response message or
 * sub-message is filtered by the API to only contain those fields as
 * specified in the mask. For example, if the mask in the previous
 * example is applied to a response message as follows:
 *
 * <pre>
 *   f {
 *     a : 22
 *     b {
 *       d : 1
 *       x : 2
 *     }
 *     y : 13
 *   }
 *   z: 8
 * </pre>
 *
 * <p>The result will not contain specific values for fields x,y and z
 * (their value will be set to the default, and omitted in proto text
 * output):
 *
 * <pre>
 *   f {
 *     a : 22
 *     b {
 *       d : 1
 *     }
 *   }
 * </pre>
 *
 * <p>A repeated field is not allowed except at the last position of a
 * paths string.
 *
 * <p>If a FieldMask object is not present in a get operation, the
 * operation applies to all fields (as if a FieldMask of all fields
 * had been specified).
 *
 * <p>Note that a field mask does not necessarily apply to the
 * top-level response message. In case of a REST get operation, the
 * field mask applies directly to the response, but in case of a REST
 * list operation, the mask instead applies to each individual message
 * in the returned resource list. In case of a REST custom method,
 * other definitions may be used. Where the mask applies will be
 * clearly documented together with its declaration in the API.  In
 * any case, the effect on the returned resource/resources is required
 * behavior for APIs.
 *
 * <h2>Field Masks in Update Operations</h2>
 *
 *
 * <p>A field mask in update operations specifies which fields of the
 * targeted resource are going to be updated. The API is required
 * to only change the values of the fields as specified in the mask
 * and leave the others untouched. If a resource is passed in to
 * describe the updated values, the API ignores the values of all
 * fields not covered by the mask.
 *
 * <p>If a repeated field is specified for an update operation, new values will
 * be appended to the existing repeated field in the target resource. Note that
 * a repeated field is only allowed in the last position of a {@code paths} string.
 *
 * <p>If a sub-message is specified in the last position of the field mask for an
 * update operation, then new value will be merged into the existing sub-message
 * in the target resource.
 *
 * <p>For example, given the target message:
 *
 * <pre>
 *   f {
 *     b {
 *       d: 1
 *       x: 2
 *     }
 *     c: [1]
 *   }
 * </pre>
 *
 * <p>And an update message:
 *
 * <pre>
 *   f {
 *     b {
 *       d: 10
 *     }
 *     c: [2]
 *   }
 * </pre>
 *
 * <p>then if the field mask is:
 *
 * <p>paths: ["f.b", "f.c"]
 *
 * <p>then the result will be:
 *
 * <pre>
 *   f {
 *     b {
 *       d: 10
 *       x: 2
 *     }
 *     c: [1, 2]
 *   }
 * </pre>
 *
 * <p>An implementation may provide options to override this default behavior for
 * repeated and message fields.
 *
 * <p>Note that libraries which implement FieldMask resolution have various
 * different behaviors in the face of empty masks or the special "*" mask.
 * When implementing a service you should confirm these cases have the
 * appropriate behavior in the underlying FieldMask library that you desire,
 * and you may need to special case those cases in your application code if
 * the underlying field mask library behavior differs from your intended
 * service semantics.
 *
 * <p>Update methods implementing <a href="https://google.aip.dev/134">https://google.aip.dev/134</a>
 * - MUST support the special value * meaning "full replace"
 * - MUST treat an omitted field mask as "replace fields which are present".
 *
 * <p>Other methods implementing <a href="https://google.aip.dev/157">https://google.aip.dev/157</a>
 * - SHOULD support the special value "*" to mean "get all".
 * - MUST treat an omitted field mask to mean "get all", unless otherwise
 * documented.
 *
 * <h2>Considerations for HTTP REST</h2>
 *
 *
 * <p>The HTTP kind of an update operation which uses a field mask must
 * be set to PATCH instead of PUT in order to satisfy HTTP semantics
 * (PUT must only be used for full updates).
 *
 * <h2>JSON Encoding of Field Masks</h2>
 *
 *
 * <p>In JSON, a field mask is encoded as a single string where paths are
 * separated by a comma. Fields name in each path are converted
 * to/from lower-camel naming conventions.
 *
 * <p>As an example, consider the following message declarations:
 *
 * <pre>
 *   message Profile {
 *     User user = 1;
 *     Photo photo = 2;
 *   }
 *   message User {
 *     string display_name = 1;
 *     string address = 2;
 *   }
 * </pre>
 *
 * <p>In proto a field mask for {@code Profile} may look as such:
 *
 * <pre>
 *   mask {
 *     paths: "user.display_name"
 *     paths: "photo"
 *   }
 * </pre>
 *
 * <p>In JSON, the same mask is represented as below:
 *
 * <pre>
 *   {
 *     mask: "user.displayName,photo"
 *   }
 * </pre>
 *
 * <h2>Field Masks and Oneof Fields</h2>
 *
 *
 * <p>Field masks treat fields in oneofs just as regular fields. Consider the
 * following message:
 *
 * <pre>
 *   message SampleMessage {
 *     oneof test_oneof {
 *       string name = 4;
 *       SubMessage sub_message = 9;
 *     }
 *   }
 * </pre>
 *
 * <p>The field mask can be:
 *
 * <pre>
 *   mask {
 *     paths: "name"
 *   }
 * </pre>
 *
 * <p>Or:
 *
 * <pre>
 *   mask {
 *     paths: "sub_message"
 *   }
 * </pre>
 *
 * <p>Note that oneof type names ("test_oneof" in this case) cannot be used in
 * paths.
 *
 * <h2>Field Mask Verification</h2>
 *
 *
 * <p>The implementation of any API method which has a FieldMask type field in the
 * request should verify the included field paths, and return an
 * {@code INVALID_ARGUMENT} error if any path is unmappable.
 *
 * <p>Protobuf type {@code com.google.protobuf.FieldMask}
 */
@ProtobufMessage
public final class FieldMask {

    /**
     * The set of field mask paths.
     *
     * <p><code>repeated string paths = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    List<String> paths;

    FieldMask(List<String> paths) {
        this.paths = paths;
    }

    public SequencedCollection<String> paths() {
        return Collections.unmodifiableSequencedCollection(paths);
    }
}

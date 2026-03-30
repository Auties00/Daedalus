package it.auties.proto.ci;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.ProtobufString;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.io.ProtobufInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.auties00.daedalus.protobuf.model.ProtobufType.STRING;

public class EmbeddedMessageTest {
    @Test
        public void testModifiers() {
        var anotherMessage = new WrapperMessage.EmbeddedMessage(ProtobufString.wrap("Hello"));
        var someMessage = new WrapperMessage(anotherMessage);
        var encoded = EmbeddedMessageTestWrapperMessageSpec.encode(someMessage);
        var decoded = EmbeddedMessageTestWrapperMessageSpec.decode(ProtobufInputStream.fromBytes(encoded));
        Assertions.assertNotNull(decoded.content());
        Assertions.assertEquals(anotherMessage.content(), decoded.content().content());
    }

    @ProtobufMessage
    public record WrapperMessage(
            @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
            EmbeddedMessage content
    ) {

            @ProtobufMessage
            public record EmbeddedMessage(
                    @ProtobufProperty(index = 3, type = STRING)
                    ProtobufString content
            ) {

            }
    }
}

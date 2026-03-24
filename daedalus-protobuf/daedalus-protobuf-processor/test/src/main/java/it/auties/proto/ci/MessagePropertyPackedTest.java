package it.auties.proto.ci;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.auties00.daedalus.protobuf.model.ProtobufType.UINT32;

public class MessagePropertyPackedTest {
    @Test
        public void testModifiers() {
        var someMessage = new PackedMessage(new ArrayList<>(List.of(1, 2, 3)));
        var encoded = MessagePropertyPackedTestPackedMessageSpec.encode(someMessage);
        var decoded = MessagePropertyPackedTestPackedMessageSpec.decode(encoded);
        Assertions.assertEquals(someMessage.content(), decoded.content());
    }

    @ProtobufMessage
    public record PackedMessage(
            @ProtobufProperty(index = 1, type = UINT32, packed = true)
            ArrayList<Integer> content
    ) {

    }
}

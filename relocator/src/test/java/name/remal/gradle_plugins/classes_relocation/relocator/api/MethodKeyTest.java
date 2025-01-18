package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.deserializeFrom;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.serializeToBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.val;
import org.junit.jupiter.api.Test;

class MethodKeyTest {

    @Test
    void serialization() {
        val methodKey = methodKeyOf("<init>", "()V");
        val bytes = serializeToBytes(methodKey);
        val deserializedMethodKey = deserializeFrom(bytes, MethodKey.class);
        assertEquals(methodKey, deserializedMethodKey);
    }

}

package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.deserializeFrom;
import static name.remal.gradle_plugins.toolkit.JavaSerializationUtils.serializeToBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClassReachabilityConfigTest {

    @Test
    void serialization() {
        var config = ClassReachabilityConfig.builder()
            .classInternalName("pkg/Logic")
            .onReachedClassInternalName("pkg/LogicCaller")
            .field("field")
            .methodsKey(methodKeyOf("<init>", "()V"))
            .allDeclaredConstructors(true)
            .allDeclaredMethods(true)
            .allDeclaredFields(true)
            .allPermittedSubclasses(true)
            .build();
        var bytes = serializeToBytes(config);
        var deserializedConfig = deserializeFrom(bytes, ClassReachabilityConfig.class);
        assertEquals(config, deserializedConfig);
    }

}

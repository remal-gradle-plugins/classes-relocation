package name.remal.gradle_plugins.classes_relocation.intern.guava_class_descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.objectweb.asm.Type.getType;

import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocatorTestLogic;

public class GuavaClassDescriptorTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        val classDescriptor = "Lcom/google/common/base/Preconditions;";
        val internalClassName = getType(classDescriptor).getInternalName();
        val className = internalClassName.replace('/', '.');
        assertDoesNotThrow(() -> Class.forName(className));
    }

}

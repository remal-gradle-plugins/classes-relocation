package name.remal.gradle_plugins.classes_relocation.intern.guava_class_descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Type.getType;

import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocatorTestLogic;
import org.objectweb.asm.Type;

public class GuavaClassDescriptorTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        {
            val classDescriptor = "Lcom/google/common/base/Functions;";
            val type = getType(classDescriptor);
            assertEquals(Type.OBJECT, type.getSort());
            val internalClassName = type.getInternalName();
            val className = internalClassName.replace('/', '.');
            assertDoesNotThrow(() -> Class.forName(className));
        }

        {
            val classDescriptor = "[[Lcom/google/common/base/Optional;";
            Type type = getType(classDescriptor);
            assertEquals(Type.ARRAY, type.getSort());
            assertEquals(2, type.getDimensions());
            type = type.getElementType();
            assertEquals(Type.OBJECT, type.getSort());
            val internalClassName = type.getInternalName();
            val className = internalClassName.replace('/', '.');
            assertDoesNotThrow(() -> Class.forName(className));
        }
    }

}

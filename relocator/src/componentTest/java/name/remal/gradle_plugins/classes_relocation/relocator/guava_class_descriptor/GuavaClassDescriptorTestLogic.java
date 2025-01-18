package name.remal.gradle_plugins.classes_relocation.relocator.guava_class_descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Type.getType;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import org.objectweb.asm.Type;

public class GuavaClassDescriptorTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        {
            var classDescriptor = "Lcom/google/common/base/Functions;";
            var type = getType(classDescriptor);
            assertEquals(Type.OBJECT, type.getSort());
            var internalClassName = type.getInternalName();
            var className = internalClassName.replace('/', '.');
            assertDoesNotThrow(() -> Class.forName(className));
        }

        {
            var classDescriptor = "[[Lcom/google/common/base/Optional;";
            Type type = getType(classDescriptor);
            assertEquals(Type.ARRAY, type.getSort());
            assertEquals(2, type.getDimensions());
            type = type.getElementType();
            assertEquals(Type.OBJECT, type.getSort());
            var internalClassName = type.getInternalName();
            var className = internalClassName.replace('/', '.');
            assertDoesNotThrow(() -> Class.forName(className));
        }
    }

}

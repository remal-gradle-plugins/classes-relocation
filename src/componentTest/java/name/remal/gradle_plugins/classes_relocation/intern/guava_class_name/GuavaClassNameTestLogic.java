package name.remal.gradle_plugins.classes_relocation.intern.guava_class_name;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocatorTestLogic;

public class GuavaClassNameTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        val className = "com.google.common.base.Preconditions";
        assertDoesNotThrow(() -> Class.forName(className));
    }

}

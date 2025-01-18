package name.remal.gradle_plugins.classes_relocation.relocator.guava_internal_class_name;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

public class GuavaInternalClassNameTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        var internalClassName = "com/google/common/base/Preconditions";
        var className = internalClassName.replace('/', '.');
        assertDoesNotThrow(() -> Class.forName(className));
    }

}

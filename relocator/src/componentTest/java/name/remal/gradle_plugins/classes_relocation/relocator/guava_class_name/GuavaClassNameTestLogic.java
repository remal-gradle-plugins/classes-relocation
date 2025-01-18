package name.remal.gradle_plugins.classes_relocation.relocator.guava_class_name;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

public class GuavaClassNameTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        var className = "com.google.common.base.Preconditions";
        assertDoesNotThrow(() -> Class.forName(className));
    }

}

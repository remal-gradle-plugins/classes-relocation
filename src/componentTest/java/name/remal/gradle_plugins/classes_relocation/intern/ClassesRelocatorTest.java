package name.remal.gradle_plugins.classes_relocation.intern;

import name.remal.gradle_plugins.classes_relocation.intern.guava_class_descriptor.GuavaClassDescriptorTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_class_name.GuavaClassNameTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_immutable_list.GuavaImmutableListTestLogic;
import name.remal.gradle_plugins.classes_relocation.intern.guava_internal_class_name.GuavaInternalClassNameTestLogic;
import org.junit.jupiter.api.Test;

class ClassesRelocatorTest extends ClassesRelocatorTestBase {

    @Test
    void guavaImmutableList() {
        assertTestLogic(GuavaImmutableListTestLogic.class, "guava");
    }

    @Test
    void guavaClassName() {
        assertTestLogic(GuavaClassNameTestLogic.class, "guava");
    }

    @Test
    void guavaInternalClassName() {
        assertTestLogic(GuavaInternalClassNameTestLogic.class, "guava");
    }

    @Test
    void guavaClassDescriptor() {
        assertTestLogic(GuavaClassDescriptorTestLogic.class, "guava");
    }

}

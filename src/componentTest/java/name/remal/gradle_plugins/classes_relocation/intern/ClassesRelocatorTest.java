package name.remal.gradle_plugins.classes_relocation.intern;

import name.remal.gradle_plugins.classes_relocation.intern.guava_immutable_list.GuavaImmutableListTestLogic;
import org.junit.jupiter.api.Test;

class ClassesRelocatorTest extends ClassesRelocatorTestBase {

    @Test
    void guava() {
        assertTestLogic(GuavaImmutableListTestLogic.class, "guava");
    }

}

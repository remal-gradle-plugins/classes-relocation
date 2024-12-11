package name.remal.gradle_plugins.classes_relocation.intern;

import name.remal.gradle_plugins.classes_relocation.intern.guava.GuavaTestLogic;
import org.junit.jupiter.api.Test;

class ClassesRelocatorTest extends ClassesRelocatorTestBase {

    @Test
    void guava() {
        assertTestLogic(GuavaTestLogic.class, "guava");
    }

}

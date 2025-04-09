package name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent;

import static org.junit.jupiter.api.Assertions.assertThrows;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent.to_relocate.ExpectedException;

public class RelocatedParentMethodTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        var child = new Child();
        assertThrows(ExpectedException.class, child::childMethod);
    }

}

package name.remal.gradle_plugins.classes_relocation.relocator.relocated_constructors;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getClassName;
import static org.junit.jupiter.api.Assertions.assertTrue;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.relocated_constructors.to_relocate.RelocatedConstructorsComponent;

public class RelocatedConstructorsTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        @SuppressWarnings("StringOperationCanBeSimplified")
        var nonConstantClassName = new String(getClassName(RelocatedConstructorsComponent.class));
        var clazz = Class.forName(nonConstantClassName);
        var ctor = clazz.getConstructor();
        var instance = (RelocatedConstructorsComponent) ctor.newInstance();
        assertTrue(instance.initialized.get());
    }

}

package name.remal.gradle_plugins.classes_relocation.relocator.accessible_field_from_parent_class;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getClassName;
import static org.junit.jupiter.api.Assertions.assertTrue;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.accessible_field_from_parent_class.to_relocate.AccessibleFieldFromParentClassComponent;

public class AccessibleFieldFromParentClassTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        @SuppressWarnings("StringOperationCanBeSimplified")
        var nonConstantClassName = new String(getClassName(AccessibleFieldFromParentClassComponent.class));
        var clazz = Class.forName(nonConstantClassName);
        var ctor = clazz.getConstructor();
        var instance = ctor.newInstance();
        var field = clazz.getField("initialized");
        assertTrue((Boolean) field.get(instance));
    }

}

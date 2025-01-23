package name.remal.gradle_plugins.classes_relocation.relocator.annotated_with_inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import name.remal.gradle_plugins.classes_relocation.relocator.annotated_with_inject.to_relocate.AnnotatedWithInjectComponent;

public class AnnotatedWithInjectTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        @SuppressWarnings("StringOperationCanBeSimplified")
        var nonConstantMethodName = new String("get");
        var method = AnnotatedWithInjectComponent.class.getMethod(nonConstantMethodName);
        assertTrue((Boolean) method.invoke(null));
    }

}

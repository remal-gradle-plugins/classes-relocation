package name.remal.gradle_plugins.classes_relocation.relocator.annotated_with_inject.to_relocate;

import javax.inject.Inject;

public class AnnotatedWithInjectComponent {

    @Inject
    public static boolean get() {
        return true;
    }

}

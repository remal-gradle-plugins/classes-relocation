package name.remal.gradle_plugins.classes_relocation.relocator.api;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

public interface ClassesRelocatorOrderedComponent
    extends ClassesRelocatorComponent, Comparable<ClassesRelocatorOrderedComponent> {

    default int getOrder() {
        return 0;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    default int compareTo(ClassesRelocatorOrderedComponent other) {
        return Integer.compare(getOrder(), other.getOrder());
    }

}

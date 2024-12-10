package name.remal.gradle_plugins.classes_relocation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;

public interface ClassesRelocationExclusions {

    @Input
    @org.gradle.api.tasks.Optional
    SetProperty<String> getExcludeClasses();

    default void excludeClasses(Iterable<String> classNamePatterns) {
        getExcludeClasses().addAll(classNamePatterns);
    }

    default void excludeClasses(String... classNamePatterns) {
        excludeClasses(asList(classNamePatterns));
    }

    default void excludeClass(String classNamePattern) {
        excludeClasses(singletonList(classNamePattern));
    }


    @Input
    @org.gradle.api.tasks.Optional
    SetProperty<String> getExcludeResources();

    default void excludeResources(Iterable<String> resourcePathPatterns) {
        getExcludeResources().addAll(resourcePathPatterns);
    }

    default void excludeResources(String... resourcePathPatterns) {
        excludeResources(asList(resourcePathPatterns));
    }

    default void excludeResource(String resourcePathPattern) {
        excludeResources(singletonList(resourcePathPattern));
    }

}

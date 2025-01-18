package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ResourcesFilter;
import org.jetbrains.annotations.Unmodifiable;

interface WithResources extends Closeable {

    @Unmodifiable
    List<Resource> getAllResources();

    @Unmodifiable
    default List<Resource> getAllResources(Consumer<ResourcesFilter> filterConfigurer) {
        var filter = new ResourcesFilter();
        filterConfigurer.accept(filter);
        if (filter.isEmpty()) {
            return getAllResources();
        } else {
            return getAllResources().stream()
                .filter(filter::matches)
                .collect(toUnmodifiableList());
        }
    }

    @Unmodifiable
    Map<String, @Unmodifiable List<Resource>> getResources();

    @Unmodifiable
    default List<Resource> getResources(String name) {
        return defaultValue(getResources().get(name), emptyList());
    }

    @Unmodifiable
    List<Resource> getClassResources(String classNameOrInternalName);


    @Unmodifiable
    Set<String> getClassInternalNames();

    @Unmodifiable
    Set<String> getClassNames();

    @Unmodifiable
    Set<String> getPackageNames();

}

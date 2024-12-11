package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

interface WithResources extends Closeable {

    @Unmodifiable
    Collection<Resource> getResources();

    @Unmodifiable
    default Collection<Resource> getResources(String name) {
        return getResources().stream()
            .filter(resource -> resource.getName().equals(name))
            .collect(toImmutableList());
    }


    @Unmodifiable
    default Stream<Resource> streamResourcesWithUniqueNames() {
        val foundResourcesNames = new LinkedHashSet<String>();
        return getResources().stream()
            .filter(resource -> foundResourcesNames.add(resource.getName()));
    }

    @Unmodifiable
    default Stream<Resource> streamResourcesWithUniqueNames(String name) {
        val foundResourcesNames = new LinkedHashSet<String>();
        return getResources(name).stream()
            .filter(resource -> foundResourcesNames.add(resource.getName()));
    }


    @Unmodifiable
    Set<String> getClassNames();

    @Unmodifiable
    Set<String> getPackageNames();

}

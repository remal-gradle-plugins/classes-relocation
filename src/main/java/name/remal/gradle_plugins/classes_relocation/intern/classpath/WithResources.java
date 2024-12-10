package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;
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
    Set<String> getClassNames();

    @Unmodifiable
    Set<String> getPackageNames();

}

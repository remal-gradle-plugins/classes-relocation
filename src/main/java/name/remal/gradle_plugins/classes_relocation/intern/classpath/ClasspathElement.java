package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.withoutMultiReleasePathPrefix;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collection;
import org.jetbrains.annotations.Unmodifiable;

public interface ClasspathElement extends WithResources, Closeable {

    Path getPath();

    String getModuleName();

    boolean isMultiRelease();

    @Unmodifiable
    default Collection<Resource> getResources(String name) {
        if (isMultiRelease()) {
            return getResources().stream()
                .filter(resource -> withoutMultiReleasePathPrefix(resource.getName()).equals(name))
                .collect(toImmutableList());
        }

        return WithResources.super.getResources(name);
    }

}

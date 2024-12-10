package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

public interface Classpath extends WithResources, Closeable {

    @SafeVarargs
    static Classpath newClasspathForPaths(Iterable<Path>... paths) {
        val combinedPaths = stream(paths)
            .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
            .collect(toImmutableList());
        return new ClasspathPaths(combinedPaths);
    }


    @Unmodifiable
    List<ClasspathElement> getElements();

    @Unmodifiable
    default Collection<Resource> getResources(String name) {
        return getElements().stream()
            .flatMap(element -> element.getResources(name).stream())
            .collect(toImmutableList());
    }


    default Classpath plus(Classpath classpath) {
        return new ClasspathComposite(ImmutableList.of(this, classpath));
    }

    default Classpath matching(Consumer<ResourcesFilter> filterConfigurer) {
        val filter = new ResourcesFilter();
        filterConfigurer.accept(filter);
        if (filter.isEmpty()) {
            return this;
        }

        return new ClasspathFiltered(this, filter);
    }

}

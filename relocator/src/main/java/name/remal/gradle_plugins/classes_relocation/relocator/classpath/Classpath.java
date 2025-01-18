package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.Unmodifiable;

public interface Classpath extends WithResources, Closeable {

    @SafeVarargs
    @SuppressWarnings("varargs")
    static Classpath newClasspathForPaths(Iterable<Path>... paths) {
        var combinedPaths = stream(paths)
            .filter(Objects::nonNull)
            .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
            .filter(Objects::nonNull)
            .collect(toUnmodifiableList());

        return new ClasspathPaths(combinedPaths);
    }


    @Unmodifiable
    List<ClasspathElement> getElements();


    default Classpath plus(Classpath classpath) {
        return new ClasspathComposite(List.of(this, classpath));
    }

}

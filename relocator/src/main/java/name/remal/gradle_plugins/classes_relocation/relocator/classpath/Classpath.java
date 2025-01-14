package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

public interface Classpath extends WithResources, Closeable {

    @SafeVarargs
    @SuppressWarnings("varargs")
    static Classpath newClasspathForPaths(Iterable<Path>... paths) {
        val combinedPaths = stream(paths)
            .filter(Objects::nonNull)
            .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
            .filter(Objects::nonNull)
            .collect(toImmutableList());

        return new ClasspathPaths(combinedPaths);
    }


    @Unmodifiable
    List<ClasspathElement> getElements();


    default Classpath plus(Classpath classpath) {
        return new ClasspathComposite(ImmutableList.of(this, classpath));
    }

}

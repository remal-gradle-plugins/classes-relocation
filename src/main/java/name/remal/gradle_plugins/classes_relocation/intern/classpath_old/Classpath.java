package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

public interface Classpath extends WithResources, Closeable {

    static Classpath newClasspathForPaths(Iterable<Path> paths) {
        return new ClasspathPaths(paths);
    }


    @Unmodifiable
    List<Path> getPaths();

    @Unmodifiable
    List<ClasspathElement> getElements();


    default Classpath plus(Classpath other) {
        return new ClasspathComposite(ImmutableList.of(this, other));
    }

    default Classpath withResources(Collection<String> inclusions, Collection<String> exclusions) {
        if (inclusions.isEmpty() && exclusions.isEmpty()) {
            return this;
        }

        return new ClasspathFiltered(this, () -> getResources(inclusions, exclusions));
    }

}

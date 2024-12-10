package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;

class ClasspathFiltered extends ClasspathBase {

    private final Classpath classpath;

    @Unmodifiable
    @Getter
    private final List<Resource> resources;

    public ClasspathFiltered(
        Classpath classpath,
        Supplier<List<Resource>> resourcesGetter
    ) {
        this.classpath = registerCloseable(classpath);
        this.resources = asLazyListProxy(LazyValue.of(resourcesGetter::get));
    }

    @Override
    @Unmodifiable
    public List<Path> getPaths() {
        return classpath.getPaths();
    }

    @Override
    @Unmodifiable
    public List<ClasspathElement> getElements() {
        return classpath.getElements();
    }

}

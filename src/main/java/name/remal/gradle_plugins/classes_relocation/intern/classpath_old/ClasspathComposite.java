package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.CustomLog;
import lombok.Getter;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;

@CustomLog
class ClasspathComposite extends ClasspathBase {

    @Unmodifiable
    private final List<Classpath> classpaths;

    @Unmodifiable
    @Getter
    private final List<Path> paths;

    @Unmodifiable
    @Getter
    private final List<ClasspathElement> elements;

    @Unmodifiable
    @Getter
    private final List<Resource> resources;

    public ClasspathComposite(Collection<Classpath> classpaths) {
        this.classpaths = classpaths.stream()
            .distinct()
            .collect(toImmutableList());
        registerCloseables(this.classpaths);
        this.paths = asLazyListProxy(LazyValue.of(() -> this.classpaths.stream()
            .map(Classpath::getPaths)
            .flatMap(Collection::stream)
            .distinct()
            .collect(toImmutableList())
        ));
        this.elements = asLazyListProxy(LazyValue.of(() -> this.classpaths.stream()
            .map(Classpath::getElements)
            .flatMap(Collection::stream)
            .distinct()
            .collect(toImmutableList())
        ));
        this.resources = asLazyListProxy(LazyValue.of(() -> this.classpaths.stream()
            .map(Classpath::getResources)
            .flatMap(Collection::stream)
            .distinct()
            .collect(toImmutableList())
        ));
    }

    @Override
    public boolean hasResource(Resource resource) {
        return classpaths.stream()
            .anyMatch(classpath -> classpath.hasResource(resource));
    }

    @Override
    public boolean hasResource(String resourcePath) {
        return classpaths.stream()
            .anyMatch(classpath -> classpath.hasResource(resourcePath));
    }

    @Override
    public boolean hasClass(String classNameOrInternalName) {
        return classpaths.stream()
            .anyMatch(classpath -> classpath.hasClass(classNameOrInternalName));
    }

}

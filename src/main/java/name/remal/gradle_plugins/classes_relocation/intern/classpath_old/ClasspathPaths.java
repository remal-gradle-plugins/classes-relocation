package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.file.Files.readAttributes;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrow;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

@Getter
@CustomLog
class ClasspathPaths extends ClasspathBase {

    @Unmodifiable
    private final List<Path> paths;

    public ClasspathPaths(Iterable<Path> paths) {
        this.paths = StreamSupport.stream(paths.spliterator(), false)
            .filter(Objects::nonNull)
            .distinct()
            .collect(toImmutableList());
    }


    @Unmodifiable
    private final List<ClasspathElement> elements = asLazyListProxy(() -> {
        val result = getPaths().stream()
            .map(path -> {
                try {
                    val attrs = readAttributes(path, BasicFileAttributes.class);
                    if (attrs.isDirectory()) {
                        return new ClasspathElementDir(path);
                    } else {
                        return new ClasspathElementJar(path);
                    }

                } catch (NoSuchFileException e) {
                    return null;

                } catch (IOException e) {
                    throw sneakyThrow(e);
                }
            })
            .filter(Objects::nonNull)
            .map(ClasspathElement.class::cast)
            .collect(toImmutableList());
        registerCloseables(result);
        return result;
    });


    @Unmodifiable
    private final List<Resource> resources = asLazyListProxy(() -> {
        return getElements().stream()
            .map(ClasspathElement::getResources)
            .flatMap(List::stream)
            .collect(toImmutableList());
    });


    @Override
    public boolean hasResource(Resource resource) {
        return getElements().stream()
            .anyMatch(element -> element.hasResource(resource));
    }

    @Override
    public boolean hasResource(String resourcePath) {
        return getElements().stream()
            .anyMatch(element -> element.hasResource(resourcePath));
    }

    @Override
    public boolean hasClass(String classNameOrInternalName) {
        return getElements().stream()
            .anyMatch(element -> element.hasClass(classNameOrInternalName));
    }

}

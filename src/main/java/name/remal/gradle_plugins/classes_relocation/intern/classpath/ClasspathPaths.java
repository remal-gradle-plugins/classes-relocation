package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.file.Files.readAttributes;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsFunction;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.jetbrains.annotations.Unmodifiable;

@Getter
class ClasspathPaths extends ClasspathBase {

    @Unmodifiable
    private final List<ClasspathElement> elements;

    public ClasspathPaths(List<Path> paths) {
        this.elements = asLazyListProxy(() -> closables.registerCloseables(
            paths.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(sneakyThrowsFunction(path -> {
                    final BasicFileAttributes attrs;
                    try {
                        attrs = readAttributes(path, BasicFileAttributes.class);
                    } catch (NoSuchFileException e) {
                        return null;
                    }

                    if (attrs.isDirectory()) {
                        return new ClasspathElementDir(path);
                    } else {
                        return new ClasspathElementJar(path);
                    }
                }))
                .filter(Objects::nonNull)
                .collect(toImmutableList())
        ));
    }


}

package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.nio.file.Files.readAttributes;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsFunction;

import com.google.common.collect.ImmutableList;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.StreamSupport;
import lombok.val;

public interface ResourceContainer extends WithResources {

    @SafeVarargs
    @SuppressWarnings("varargs")
    static ResourceContainer newResourceContainerPaths(Iterable<Path>... paths) {
        val combinedPaths = stream(paths)
            .filter(Objects::nonNull)
            .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
            .filter(Objects::nonNull)
            .collect(toImmutableSet());

        val containers = asLazyListProxy(() ->
            combinedPaths.stream()
                .map(sneakyThrowsFunction(path -> {
                    final BasicFileAttributes attrs;
                    try {
                        attrs = readAttributes(path, BasicFileAttributes.class);
                    } catch (NoSuchFileException e) {
                        return null;
                    }

                    if (attrs.isDirectory()) {
                        return new ResourceContainerDir(path);
                    } else {
                        return new ResourceContainerZip(path);
                    }
                }))
                .filter(Objects::nonNull)
                .collect(toList())
        );

        return new ResourceContainerComposite(containers);
    }


    default ResourceContainer plus(ResourceContainer container) {
        return new ResourceContainerComposite(ImmutableList.of(this, container));
    }

}

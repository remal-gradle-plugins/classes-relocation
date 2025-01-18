package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.Collection;
import lombok.Getter;

@Getter
abstract class ResourceContainerBase extends WithResourcesBase implements ResourceContainer {

    private final Path path;

    protected ResourceContainerBase(Path path) {
        this.path = path;
    }

    @Override
    protected final Collection<Resource> readResources() throws Exception {
        return readClasspathElementResources().stream()
            .sorted()
            .collect(toList());
    }

    protected abstract Collection<Resource> readClasspathElementResources() throws Exception;

}

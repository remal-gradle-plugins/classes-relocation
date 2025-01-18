package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Unmodifiable;

class ResourceContainerComposite extends WithResourcesBase implements ResourceContainer {

    @Unmodifiable
    private final List<ResourceContainer> containers;

    public ResourceContainerComposite(List<? extends ResourceContainer> containers) {
        this.containers = asLazyListProxy(() -> closables.registerCloseables(
            containers.stream()
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList())
        ));
    }

    @Override
    protected Collection<Resource> readResources() {
        return containers.stream()
            .map(ResourceContainer::getAllResources)
            .flatMap(Collection::stream)
            .collect(toList());
    }

}

package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyMapProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;

@Getter
abstract class WithResourcesBase extends ClosablesContainer implements WithResources {

    @Unmodifiable
    private final Set<String> resourcePaths =
        asLazySetProxy(LazyValue.of(WithResources.super::getResourcePaths));

    @Unmodifiable
    private final Map<String, List<Resource>> classes =
        asLazyMapProxy(LazyValue.of(WithResources.super::getClasses));

    @Unmodifiable
    private final Set<String> packageNames =
        asLazySetProxy(LazyValue.of(WithResources.super::getPackageNames));

}

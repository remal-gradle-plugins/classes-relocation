package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.util.List;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Unmodifiable;

@Getter
class ClasspathFiltered extends ClasspathBase {

    @Unmodifiable
    private final List<ClasspathElement> elements;

    public ClasspathFiltered(Classpath delegate, ResourcesFilter filter) {
        val clonedFilter = new ResourcesFilter(filter);
        this.elements = asLazyListProxy(() -> closables.registerCloseables(
            delegate.getElements().stream()
                .map(element -> new ClasspathElementFiltered(element, clonedFilter))
                .collect(toImmutableList())
        ));
    }

}

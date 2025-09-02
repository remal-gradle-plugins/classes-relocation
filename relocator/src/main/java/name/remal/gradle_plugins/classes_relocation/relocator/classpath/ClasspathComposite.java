package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.stream.Collectors.toUnmodifiableList;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.jetbrains.annotations.Unmodifiable;

@Getter
class ClasspathComposite extends ClasspathBase {

    @Unmodifiable
    private final List<ClasspathElement> elements;

    public ClasspathComposite(List<? extends Classpath> classpaths) {
        this.elements = asLazyListProxy(() -> closeables.registerCloseables(
            classpaths.stream()
                .filter(Objects::nonNull)
                .map(Classpath::getElements)
                .flatMap(Collection::stream)
                .distinct()
                .collect(toUnmodifiableList())
        ));
    }

}

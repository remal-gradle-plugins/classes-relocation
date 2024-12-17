package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import lombok.Getter;

@Getter
abstract class ClasspathBase extends WithResourcesBase implements Classpath {

    @Override
    protected Collection<Resource> readResources() {
        return getElements().stream()
            .map(WithResources::getAllResources)
            .flatMap(Collection::stream)
            .collect(toList());
    }

}

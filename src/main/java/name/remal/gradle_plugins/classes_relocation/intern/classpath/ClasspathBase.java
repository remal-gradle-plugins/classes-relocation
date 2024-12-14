package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Map;

abstract class ClasspathBase extends WithResourcesBase implements Classpath {

    @Override
    protected Collection<Resource> readResources() {
        return getElements().stream()
            .map(WithResources::getResources)
            .map(Map::values)
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .collect(toList());
    }

}

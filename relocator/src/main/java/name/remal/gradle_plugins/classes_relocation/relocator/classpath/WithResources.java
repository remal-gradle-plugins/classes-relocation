package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.Collections.emptyList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Unmodifiable;

interface WithResources extends Closeable {

    @Unmodifiable
    List<Resource> getAllResources();

    @Unmodifiable
    Map<String, @Unmodifiable List<Resource>> getResources();

    @Unmodifiable
    default List<Resource> getResources(String name) {
        return defaultValue(getResources().get(name), emptyList());
    }

    @Unmodifiable
    List<Resource> getClassResources(String classNameOrInternalName);


    @Unmodifiable
    Set<String> getClassInternalNames();

    @Unmodifiable
    Set<String> getClassNames();

    @Unmodifiable
    Set<String> getPackageNames();

}

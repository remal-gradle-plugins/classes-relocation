package name.remal.gradle_plugins.classes_relocation.intern.relocator;

import static java.util.Collections.emptyList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import java.util.Collection;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;

public interface ResourceRelocator {

    Collection<String> getInclusions();

    default Collection<String> getExclusions() {
        return emptyList();
    }

    void relocateResource(Resource resource, RelocationContext context);


    int getPriority();

    int MANIFEST_PRIORITY = doNotInline(0);

}

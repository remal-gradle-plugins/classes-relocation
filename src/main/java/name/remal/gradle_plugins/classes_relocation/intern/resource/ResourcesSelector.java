package name.remal.gradle_plugins.classes_relocation.intern.resource;

import java.util.List;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.intern.Ordered;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import org.jetbrains.annotations.Unmodifiable;

public interface ResourcesSelector extends Ordered {

    @Unmodifiable
    List<Resource> select(
        String resourceName,
        @Nullable Resource loadingResource,
        RelocationContext context
    ) throws Throwable;

}

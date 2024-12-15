package name.remal.gradle_plugins.classes_relocation.intern.resource;

import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.intern.Ordered;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

public interface ResourceProcessor extends Ordered {

    Optional<byte[]> processResource(Resource resource, RelocationContext context) throws Throwable;

}

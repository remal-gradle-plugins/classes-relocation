package name.remal.gradle_plugins.classes_relocation.intern;

import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;

class MergedResource extends GeneratedResource {

    public static Resource newMergedResource(String name, @Nullable Long lastModifiedMillis, byte[] content) {
        return new MergedResource(name, lastModifiedMillis, content);
    }


    public MergedResource(String name, @Nullable Long lastModifiedMillis, byte[] content) {
        super(name, lastModifiedMillis, content);
    }

}

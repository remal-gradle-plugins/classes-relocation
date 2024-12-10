package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
abstract class ResourceBase extends WithIdentityEqualsHashCode implements Resource {

    private final String name;


    @Override
    public String toString() {
        return getName();
    }

}

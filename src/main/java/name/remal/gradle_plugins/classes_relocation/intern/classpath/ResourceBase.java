package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Getter
@RequiredArgsConstructor
abstract class ResourceBase extends WithIdentityEqualsHashCode implements Resource {

    private final String name;


    @Override
    public String toString() {
        val classpathElement = getClasspathElement();
        if (classpathElement != null) {
            return classpathElement.toString() + '[' + getName() + ']';
        } else {
            return getName();
        }
    }

}

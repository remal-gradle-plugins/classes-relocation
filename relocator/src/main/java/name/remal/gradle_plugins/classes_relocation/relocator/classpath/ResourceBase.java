package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.parseMultiReleaseResourceName;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Getter
@RequiredArgsConstructor
abstract class ResourceBase extends WithIdentityEqualsHashCode implements Resource {

    private final String name;

    @Nullable
    private final Integer multiReleaseVersion;

    protected ResourceBase(String name) {
        val multiReleaseResourceName = parseMultiReleaseResourceName(name);
        this.name = multiReleaseResourceName.getName();
        this.multiReleaseVersion = multiReleaseResourceName.getMultiReleaseVersion();
    }


    @Override
    public String toString() {
        val sb = new StringBuilder();

        val classpathElement = getClasspathElement();
        if (classpathElement != null) {
            sb.append(classpathElement).append('[').append(this.getName()).append(']');
        } else {
            sb.append(getClass().getSimpleName()).append('[').append(this.getName()).append(']');
        }

        val multiReleaseVersion = getMultiReleaseVersion();
        if (multiReleaseVersion != null) {
            sb.append("[multiReleaseVersion=").append(multiReleaseVersion).append(']');
        }

        return sb.toString();
    }

}

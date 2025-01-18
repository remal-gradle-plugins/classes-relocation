package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.parseMultiReleaseResourceName;

import com.google.errorprone.annotations.ForOverride;
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


    @ForOverride
    protected String getToStringNamePrefix() {
        return "";
    }

    @Override
    public String toString() {
        val sb = new StringBuilder();

        val classpathElement = getClasspathElement();
        if (classpathElement != null) {
            sb
                .append(classpathElement)
                .append('[')
                .append(getToStringNamePrefix())
                .append(getName()).append(']');
        } else {
            sb
                .append(getClass().getSimpleName())
                .append('[')
                .append(getToStringNamePrefix())
                .append(getName())
                .append(']');
        }

        val multiReleaseVersion = getMultiReleaseVersion();
        if (multiReleaseVersion != null) {
            sb
                .append("[multiReleaseVersion=")
                .append(multiReleaseVersion)
                .append(']');
        }

        return sb.toString();
    }

}

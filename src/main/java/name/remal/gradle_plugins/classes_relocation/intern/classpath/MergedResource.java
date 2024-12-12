package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.Getter;

public class MergedResource extends ResourceBase {

    public static Resource newMergedResource(String name, byte[] content) {
        return new MergedResource(name, content);
    }


    @Getter
    private final long lastModifiedMillis = currentTimeMillis();

    private final byte[] content;

    private MergedResource(String name, byte[] content) {
        super(name);
        this.content = content.clone();
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(content);
    }

    @Nullable
    @Override
    public ClasspathElement getClasspathElement() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + getName() + ']';
    }

}

package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.Getter;

public abstract class GeneratedResource extends ResourceBase {

    private final byte[] content;

    @Getter
    private final long lastModifiedMillis;

    protected GeneratedResource(String name, @Nullable Long lastModifiedMillis, byte[] content) {
        super(name);
        this.content = content.clone();
        this.lastModifiedMillis = lastModifiedMillis != null && lastModifiedMillis >= 0
            ? lastModifiedMillis
            : currentTimeMillis();
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

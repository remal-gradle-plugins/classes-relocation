package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.Getter;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.content.Content;

public class GeneratedResource extends ResourceBase {

    @Getter
    private final long lastModifiedMillis = currentTimeMillis();

    @Getter
    private final ClasspathElement classpathElement = new GeneratedClasspathElement(singletonList(this));

    @Getter
    private final String path;

    private final Content content;

    public GeneratedResource(String path, Content content) {
        this.path = path;
        this.content = content;
    }

    @Override
    public InputStream open() {
        val bytes = content.toByteArray();
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public String toString() {
        return GeneratedResource.class.getSimpleName() + '[' + getPath() + ']';
    }

}

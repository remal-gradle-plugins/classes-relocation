package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GeneratedClasspathElement implements ClasspathElement {

    private final List<Resource> resources;


    @Nullable
    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getModuleName() {
        return GeneratedClasspathElement.class.getName();
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public String toString() {
        return GeneratedClasspathElement.class.getSimpleName();
    }

}

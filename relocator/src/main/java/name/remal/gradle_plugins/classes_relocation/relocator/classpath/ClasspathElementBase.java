package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrows;
import static name.remal.gradle_plugins.toolkit.reflection.ModuleNameParser.parseModuleName;

import java.nio.file.Path;
import java.util.Collection;
import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;

@Getter
@CustomLog
abstract class ClasspathElementBase extends WithResourcesBase implements ClasspathElement {

    private static final String MODULE_INFO_NAME = "module-info.class";


    private final Path path;

    protected ClasspathElementBase(Path path) {
        this.path = path;
    }


    @Override
    protected final Collection<Resource> readResources() throws Exception {
        return readClasspathElementResources().stream()
            .sorted()
            .collect(toList());
    }

    protected abstract Collection<Resource> readClasspathElementResources() throws Exception;


    private final LazyValue<String> moduleName = lazyValue(() -> {
        val moduleName = parseModuleName(
            () -> getResources(MODULE_INFO_NAME).stream()
                .findFirst()
                .map(sneakyThrows(Resource::open))
                .orElse(null),
            () -> getResources(MANIFEST_NAME).stream()
                .findFirst()
                .map(sneakyThrows(Resource::open))
                .orElse(null),
            getPath()
        );
        if (moduleName != null) {
            return moduleName;
        }

        throw new IllegalStateException("Can't get module name for " + this);
    });

    @Override
    public String getModuleName() {
        return moduleName.get();
    }


    @Override
    public final String toString() {
        return getPath().toString();
    }

}

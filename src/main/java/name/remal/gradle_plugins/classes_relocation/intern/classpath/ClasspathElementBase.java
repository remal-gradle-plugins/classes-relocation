package name.remal.gradle_plugins.classes_relocation.intern.classpath;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrows;
import static name.remal.gradle_plugins.toolkit.reflection.ModuleNameParser.parseModuleName;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;

@Getter
@CustomLog
abstract class ClasspathElementBase extends WithResourcesBase implements ClasspathElement {

    private static final Pattern MODULE_INFO_RESOURCE_NAME = Pattern.compile(
        "^(META-INF/versions/\\d+/)?module-info\\.class$"
    );


    private final Path path;

    protected ClasspathElementBase(Path path) {
        this.path = path;
    }

    @Override
    protected final Collection<Resource> readResources() throws Exception {
        val map = new LinkedHashMap<String, Resource>();
        for (val resource : readClasspathElementResources()) {
            val name = resource.getName();
            val previousResource = map.putIfAbsent(name, resource);
            if (previousResource != null) {
                logger.warn(
                    "{} has multiple resources with name `{}`, only first resource will be processed",
                    this,
                    name
                );
            }
        }
        return map.values();
    }

    protected abstract Collection<Resource> readClasspathElementResources() throws Exception;


    private final LazyValue<String> moduleName = lazyValue(() -> {
        val moduleName = parseModuleName(
            () -> getResources().stream()
                .filter(resource -> MODULE_INFO_RESOURCE_NAME.matcher(resource.getName()).matches())
                .findFirst()
                .map(sneakyThrows(Resource::open))
                .orElse(null),
            () -> getResources().stream()
                .filter(resource -> MANIFEST_NAME.equals(resource.getName()))
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

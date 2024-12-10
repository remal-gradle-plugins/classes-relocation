package name.remal.gradle_plugins.classes_relocation.intern.classpath_old;

import static java.lang.System.identityHashCode;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.LazyValue.lazyValue;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrows;
import static name.remal.gradle_plugins.toolkit.reflection.ModuleNameParser.parseModuleName;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;

abstract class ClasspathElementBase extends WithResourcesBase implements ClasspathElement {

    private static final Pattern MODULE_INFO_RESOURCE_PATH = Pattern.compile(
        "^(META-INF/versions/\\d+/)?module-info\\.class$"
    );

    @Override
    public String getModuleName() {
        return moduleName.get();
    }

    private final LazyValue<String> moduleName = lazyValue(() -> {
        val resources = asLazyListProxy(this::getResources);
        val moduleName = parseModuleName(
            () -> resources.stream()
                .filter(resource -> MODULE_INFO_RESOURCE_PATH.matcher(resource.getPath()).matches())
                .findFirst()
                .map(sneakyThrows(Resource::open))
                .orElse(null),
            () -> resources.stream()
                .filter(resource -> MANIFEST_NAME.equals(resource.getPath()))
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
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }


}

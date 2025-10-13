package name.remal.gradle_plugins.classes_relocation.relocator.minimization;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static name.remal.gradle_plugins.classes_relocation.relocator.minimization.ClassReachabilityConfigUtils.convertClassReachabilityConfigToMap;
import static name.remal.gradle_plugins.classes_relocation.relocator.minimization.ClassReachabilityConfigUtils.convertMapToClassReachabilityConfig;
import static name.remal.gradle_plugins.classes_relocation.relocator.minimization.ClassReachabilityConfigUtils.groupClassReachabilityConfigs;
import static name.remal.gradle_plugins.classes_relocation.relocator.minimization.ReachabilityConfigDefaults.DEFAULT_CLASS_REACHABILITY_CONFIGS;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.parseJsonArray;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.JsonUtils.parseJsonObject;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.metadata.AbstractClassesRelocationJsonSetMetadata;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

public class ClassReachabilityConfigs
    extends AbstractClassesRelocationJsonSetMetadata<ClassReachabilityConfig> {

    @Unmodifiable
    public List<ClassReachabilityConfig> getClassReachabilityConfigs(String classInternalName) {
        return getCurrent().stream()
            .filter(config -> config.getClassInternalName().equals(classInternalName))
            .collect(toUnmodifiableList());
    }

    @Unmodifiable
    public List<ClassReachabilityConfig> getClassReachabilityConfigsEnabledByClass(String enablingClassInternalName) {
        return getCurrent().stream()
            .filter(config -> enablingClassInternalName.equals(config.getOnReachedClassInternalName()))
            .collect(toUnmodifiableList());
    }


    @Override
    protected String getResourceName() {
        return "META-INF!name.remal.classes-relocation!reachability-metadata-reflection.json".replace('!', '/');
    }

    @Nullable
    @Override
    protected ClassReachabilityConfig readStorageElement(Object element) {
        if (!(element instanceof Map)) {
            return null;
        }

        var map = (Map<?, ?>) element;
        return convertMapToClassReachabilityConfig(map);
    }

    @Nullable
    @Override
    protected Object createStorageElement(ClassReachabilityConfig element) {
        return convertClassReachabilityConfigToMap(element);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void prepareRelocation(RelocationContext context) {
        super.prepareRelocation(context);

        loadGeneralMetadataResources(context);
        loadReflectMetadataResources(context);
        addClassReachabilityConfigsFromMinimizationExclusions(context);
        getCurrent().addAll(context.getConfig().getMinimization().getClassReachabilityConfigs());
        getCurrent().addAll(DEFAULT_CLASS_REACHABILITY_CONFIGS);

        List<ClassReachabilityConfig> current = new ArrayList<>(getCurrent());
        getCurrent().clear();
        current = groupClassReachabilityConfigs(current);
        getCurrent().addAll(current);
    }

    private void loadGeneralMetadataResources(RelocationContext context) {
        var generalMetadataResources = Stream.concat(
            context.getReachabilityMetadataResourceContainer()
                .getAllResources(filter -> filter.include(
                    "**/reachability-metadata.json"
                )).stream(),
            context.getSourceAndRelocationClasspath()
                .plus(context.getCompileAndRuntimeClasspath())
                .getAllResources(filter -> filter.include(
                    "META-INF/native-image/*/*/reachability-metadata.json"
                )).stream()
        ).collect(toList());

        for (var resource : generalMetadataResources) {
            var generalMetadata = parseJsonObject(resource);
            if (generalMetadata == null) {
                continue;
            }

            var reflectMetadata = castOrWarn(
                generalMetadata.get("reflection"),
                collectionClass(),
                "reflection",
                resource
            );
            loadReflectMetadata(reflectMetadata, resource);
        }
    }

    private void loadReflectMetadataResources(RelocationContext context) {
        var reflectMetadataResources = context.getReachabilityMetadataResourceContainer()
            .getAllResources(filter -> filter.include(
                "**/reflect-config.json"
            ));

        for (var resource : reflectMetadataResources) {
            var reflectMetadata = parseJsonArray(resource);
            loadReflectMetadata(reflectMetadata, resource);
        }
    }

    private void loadReflectMetadata(@Nullable Collection<?> reflectMetadata, Resource resource) {
        if (reflectMetadata == null) {
            return;
        }

        for (var reflection : reflectMetadata) {
            var reflectionMap = castOrWarn(
                reflection,
                mapClass(),
                resource
            );
            var config = convertMapToClassReachabilityConfig(reflectionMap);
            if (config != null) {
                getCurrent().add(config);
            }
        }
    }


    private void addClassReachabilityConfigsFromMinimizationExclusions(RelocationContext context) {
        var keepFilter = context.getConfig().getMinimization().getKeepResourcesFilter();
        if (keepFilter.isEmpty()) {
            return;
        }

        var keptResources = context.getRelocationClasspath()
            .getAllResources(keepFilter);

        var keptClassInternalNames = keptResources.stream()
            .map(Resource::getName)
            .filter(resourceName -> resourceName.endsWith(".class"))
            .map(resourceName -> resourceName.substring(0, resourceName.length() - ".class".length()))
            .collect(toImmutableSet());

        keptClassInternalNames.stream()
            .map(classInternalName -> ClassReachabilityConfig.builder()
                .classInternalName(classInternalName)
                .allDeclaredConstructors(true)
                .allDeclaredMethods(true)
                .allDeclaredFields(true)
                .allPermittedSubclasses(true)
                .build()
            )
            .forEach(getCurrent()::add);
    }

}

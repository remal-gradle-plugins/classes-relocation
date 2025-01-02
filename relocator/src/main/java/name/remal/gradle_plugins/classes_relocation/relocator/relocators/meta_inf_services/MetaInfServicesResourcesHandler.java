package name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.resourceNameWithFileNamePrefix;
import static name.remal.gradle_plugins.toolkit.FunctionUtils.toSubstringedBefore;
import static name.remal.gradle_plugins.toolkit.InputOutputStreamUtils.readStringFromStream;

import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClass;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.BaseResourcesHandler;
import name.remal.gradle_plugins.toolkit.ObjectUtils;

public class MetaInfServicesResourcesHandler extends BaseResourcesHandler {

    private static final Charset CHARSET = UTF_8;
    private static final Pattern NEW_LINES = Pattern.compile("[\\n\\r]+");

    public MetaInfServicesResourcesHandler() {
        super(
            ImmutableList.of(
                "META-INF/services/*"
            ),
            ImmutableList.of(
                "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule"
            )
        );
    }

    @Override
    protected Optional<Resource> selectImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        List<Resource> candidateResources,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) {
        return Optional.of(newGeneratedResource(builder -> builder
            .withSourceResources(candidateResources)
            .withContent(() -> {
                val mergedServices = parseServices(candidateResources);
                return join("\n", mergedServices).getBytes(CHARSET);
            })
        ));
    }

    @Override
    protected Optional<Resource> processResourceImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) {
        val updatedResourceName = resourceNameWithFileNamePrefix(
            resource,
            context.getRelocatedClassNamePrefix()
        );

        val services = parseServices(ImmutableList.of(resource));
        val relocatedServices = services.stream()
            .map(serviceImpl -> relocateServiceImplementation(serviceImpl, context))
            .collect(toList());
        val content = join("\n", relocatedServices).getBytes(CHARSET);

        return Optional.of(newGeneratedResource(builder -> builder
            .withSourceResource(resource)
            .withName(updatedResourceName)
            .withMultiReleaseVersion(multiReleaseVersion)
            .withContent(content)
        ));
    }

    @SneakyThrows
    private static Set<String> parseServices(Collection<? extends Resource> resources) {
        val services = new LinkedHashSet<String>();
        for (val resource : resources) {
            final String content;
            try (val inputStream = resource.open()) {
                content = readStringFromStream(inputStream, CHARSET);
            }

            NEW_LINES.splitAsStream(content)
                .map(toSubstringedBefore("#"))
                .map(String::trim)
                .filter(ObjectUtils::isNotEmpty)
                .forEach(services::add);
        }
        return services;
    }

    private static String relocateServiceImplementation(String serviceImplClassName, RelocationContext context) {
        val serviceImplClassInternalName = toClassInternalName(serviceImplClassName);
        if (context.isRelocationClassInternalName(serviceImplClassInternalName)) {
            context.queue(new RelocateClass(serviceImplClassInternalName));
            return context.getRelocatedClassNamePrefix() + serviceImplClassName;
        }

        return serviceImplClassName;
    }

}

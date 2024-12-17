package name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.FunctionUtils.toSubstringedBefore;
import static name.remal.gradle_plugins.toolkit.InputOutputStreamUtils.readStringFromStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClass;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.ResourceProcessor;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.ResourcesMerger;
import name.remal.gradle_plugins.toolkit.GlobPattern;
import name.remal.gradle_plugins.toolkit.ObjectUtils;

public class MetaInfServicesHandler implements ResourcesMerger, ResourceProcessor {

    private static final GlobPattern INCLUDE = GlobPattern.compile("META-INF/services/*");

    private static final Set<String> EXCLUDES = ImmutableSet.of(
        "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule"
    );

    private static final Charset CHARSET = UTF_8;
    private static final Pattern NEW_LINES = Pattern.compile("[\\n\\r]+");

    @Override
    public Optional<byte[]> merge(String resourceName, Collection<? extends Resource> resources) {
        if (!INCLUDE.matches(resourceName)
            || EXCLUDES.contains(resourceName)
        ) {
            return Optional.empty();
        }

        val mergedServices = parseServices(resources);
        val content = join("\n", mergedServices).getBytes(CHARSET);
        return Optional.of(content);
    }

    @Override
    public Optional<byte[]> processResource(Resource resource, RelocationContext context) {
        val resourceName = resource.getName();
        if (!INCLUDE.matches(resourceName)
            || EXCLUDES.contains(resourceName)
        ) {
            return Optional.empty();
        }

        val services = parseServices(ImmutableList.of(resource));
        val relocatedServices = services.stream()
            .map(serviceImpl -> relocateServiceImplementation(serviceImpl, context))
            .collect(toList());
        val content = join("\n", relocatedServices).getBytes(CHARSET);
        return Optional.of(content);
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

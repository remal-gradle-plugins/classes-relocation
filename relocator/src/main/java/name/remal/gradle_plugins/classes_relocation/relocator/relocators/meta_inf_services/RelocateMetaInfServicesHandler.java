package name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_services;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_NOT_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.AsmUtils.toClassName;
import static name.remal.gradle_plugins.toolkit.FunctionUtils.toSubstringedBefore;
import static name.remal.gradle_plugins.toolkit.InputOutputStreamUtils.readStringFromStream;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsConsumer;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ResourceKey;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClass;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;
import name.remal.gradle_plugins.toolkit.ObjectUtils;

public class RelocateMetaInfServicesHandler implements QueuedTaskHandler<RelocateMetaInfServices> {

    private static final Charset CHARSET = UTF_8;
    private static final Pattern NEW_LINES = Pattern.compile("[\\n\\r]+");

    @Override
    public QueuedTaskHandlerResult handle(RelocateMetaInfServices task, RelocationContext context) {
        val serviceClassName = toClassName(task.getServiceClassInternalName());
        val resourceName = "META-INF/services/" + serviceClassName;
        if (resourceName.equals("org.codehaus.groovy.runtime.ExtensionModule")) {
            return TASK_NOT_HANDLED;
        }

        val serviceResources = context.getSourceAndRelocationClasspath()
            .getResources(resourceName);
        if (serviceResources.isEmpty()) {
            return TASK_HANDLED;
        }

        val relocatedResourceName = "META-INF/services/" + context.getRelocatedClassNamePrefix() + serviceClassName;

        val groupedServiceResources = serviceResources.stream()
            .collect(groupingBy(ResourceKey::resourceKeyFor));

        groupedServiceResources.values().forEach(sneakyThrowsConsumer(resources -> {
            val services = parseServices(resources);
            val relocatedServices = services.stream()
                .map(serviceImpl -> relocateServiceImplementation(serviceImpl, context))
                .collect(toList());
            val content = join("\n", relocatedServices).getBytes(CHARSET);
            val generatedResource = newGeneratedResource(resources, relocatedResourceName, content);
            context.writeToOutput(generatedResource, relocatedResourceName);
        }));

        return TASK_HANDLED;
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

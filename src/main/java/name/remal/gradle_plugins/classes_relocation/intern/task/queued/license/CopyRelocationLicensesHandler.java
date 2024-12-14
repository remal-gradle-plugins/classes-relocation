package name.remal.gradle_plugins.classes_relocation.intern.task.queued.license;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.ResourceNameUtils.resourceNameWithRelocationSource;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;

public class CopyRelocationLicensesHandler implements QueuedTaskHandler<CopyRelocationLicenses> {

    private static final Pattern LICENSE_RESOURCE_NAME_PATTERN = Pattern.compile(
        "^(.*/)?[^/]*\\b((license)|(notice))\\b[^/]*$",
        CASE_INSENSITIVE
    );

    @Override
    public QueuedTaskHandlerResult handle(CopyRelocationLicenses task, RelocationContext context) {
        val licenseResources = context.getRelocationClasspath().getResources().values().stream()
            .flatMap(Collection::stream)
            .filter(context::isResourceProcessed)
            .map(Resource::getClasspathElement)
            .filter(Objects::nonNull)
            .distinct()
            .flatMap(classpathElement ->
                classpathElement.getResources().values().stream()
                    .flatMap(Collection::stream)
                    .filter(resource -> LICENSE_RESOURCE_NAME_PATTERN.matcher(resource.getName()).matches())
            )
            .collect(toList());
        for (val resource : licenseResources) {
            val relocationSource = context.getRelocationSource(resource);
            val updatedResourceName = resourceNameWithRelocationSource(resource, relocationSource);
            context.writeToOutput(resource, updatedResourceName);
        }

        return TASK_HANDLED;
    }

}

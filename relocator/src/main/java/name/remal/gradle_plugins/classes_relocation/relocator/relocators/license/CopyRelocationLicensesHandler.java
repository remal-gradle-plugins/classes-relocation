package name.remal.gradle_plugins.classes_relocation.relocator.relocators.license;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.resourceNameWithRelocationSource;
import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

public class CopyRelocationLicensesHandler implements QueuedTaskHandler<CopyRelocationLicenses> {

    private static final Pattern LICENSE_RESOURCE_NAME_PATTERN = Pattern.compile(
        "^(.*/)?[^/]*\\b((license)|(notice))\\b[^/]*$",
        CASE_INSENSITIVE
    );

    private final Set<String> usedNames = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    @Override
    public QueuedTaskHandlerResult handle(CopyRelocationLicenses task, RelocationContext context) {
        var licenseResources = context.getRelocationClasspath().getAllResources().stream()
            .filter(context::isResourceProcessed)
            .map(Resource::getClasspathElement)
            .filter(Objects::nonNull)
            .distinct()
            .flatMap(classpathElement ->
                classpathElement.getAllResources().stream()
                    .filter(resource -> LICENSE_RESOURCE_NAME_PATTERN.matcher(resource.getName()).matches())
            )
            .collect(toList());
        for (var resource : licenseResources) {
            var originalResourceName = context.getOriginalResourceName(resource);
            var relocationSource = context.getRelocationSource(resource);
            var updatedResourceName = resourceNameWithRelocationSource(originalResourceName, relocationSource);
            var newResource = GeneratedResource.newGeneratedResource(builder -> builder
                .withSourceResource(resource)
                .withName(updatedResourceName)
            );
            if (usedNames.add(newResource.getName())) {
                context.writeToOutput(newResource);
            }
        }

        return TASK_HANDLED;
    }

}

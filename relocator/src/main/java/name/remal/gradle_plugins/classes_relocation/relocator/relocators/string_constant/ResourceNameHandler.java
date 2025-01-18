package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.canBePartOfResourceName;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.getNamePrefixOfResourceName;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;

import java.util.Optional;
import java.util.function.Predicate;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ResourceNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    @SuppressWarnings("java:S3776")
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        var string = task.getString();

        if (string.startsWith("classpath:")) {
            var newTask = task.withString(string.substring("classpath:".length()));
            var result = handle(newTask, context);
            if (result.isPresent()) {
                return result;
            }
        }

        if (!canBePartOfResourceName(string)) {
            return Optional.empty();
        }

        int startPos = 0;
        boolean absolute = false;
        if (string.charAt(0) == '/') {
            startPos++;
            absolute = true;
        }


        {
            var resourceName = string.substring(startPos);
            var relocatedResourcesNames = context.getRelocationClasspath().getResources().keySet().stream()
                .filter(byResourceName(resourceName))
                .filter(not(ResourceNameHandler::isExcludedResource))
                .map(currentResourceName -> context.executeOptional(new RelocateResource(
                    currentResourceName,
                    task.getClassResource().getClasspathElement()
                )))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
            if (relocatedResourcesNames.isEmpty()) {
                // do nothing
            } else if (relocatedResourcesNames.size() == 1) {
                return Optional.of((absolute ? "/" : "") + relocatedResourcesNames.get(0));
            } else {
                var hasNamesStartedWithRelocatedResourceNamePrefix = relocatedResourcesNames.stream()
                    .anyMatch(name -> name.startsWith(context.getRelocatedResourceNamePrefix()));
                if (hasNamesStartedWithRelocatedResourceNamePrefix) {
                    return Optional.of(
                        (absolute ? "/" : "")
                            + context.getRelocatedResourceNamePrefix()
                            + resourceName
                    );
                }
            }
        }

        if (!absolute) {
            var classInternalName = task.getClassInternalName();
            var resourceNamePrefix = getNamePrefixOfResourceName(classInternalName);
            var resourceName = resourceNamePrefix + string;
            if (isExcludedResource(resourceName)) {
                return Optional.of(string);
            }
            if (context.isRelocationResourceName(resourceName)) {
                context.executeOptional(new RelocateResource(
                    resourceName,
                    task.getClassResource().getClasspathElement()
                ));
                return Optional.of(string);
            }
        }

        return Optional.empty();
    }

    private static Predicate<String> byResourceName(String resourceName) {
        if (resourceName.contains("/")) {
            return name -> name.startsWith(resourceName)
                && (name.length() == resourceName.length() || name.charAt(resourceName.length()) == '/');

        } else {
            return resourceName::equals;
        }
    }

    private static boolean isExcludedResource(String resourceName) {
        return resourceName.equals("module-info.class")
            || resourceName.equals(MANIFEST_NAME);
    }

    @Override
    public int getOrder() {
        return 100;
    }

}

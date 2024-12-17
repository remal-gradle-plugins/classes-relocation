package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.canBePartOfResourceName;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.getNamePrefixOfResourceName;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class ResourceNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        val string = task.getString();
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
            val resourceName = string.substring(startPos);
            val matches = new AtomicBoolean();
            context.getRelocationClasspath().getResources().keySet().stream()
                .filter(byResourceName(resourceName))
                .forEach(name -> {
                    context.queue(new RelocateResource(
                        name,
                        context.getRelocatedClassInternalNamePrefix() + name
                    ));
                    matches.set(true);
                });

            if (matches.get()) {
                return Optional.of(
                    (absolute ? "/" : "")
                        + context.getRelocatedClassInternalNamePrefix()
                        + resourceName
                );
            }
        }

        if (!absolute) {
            val classInternalName = task.getClassInternalName();
            val resourceNamePrefix = getNamePrefixOfResourceName(classInternalName);
            val resourceName = resourceNamePrefix + string;
            if (context.isRelocationResourceName(resourceName)) {
                context.queue(new RelocateResource(
                    resourceName,
                    context.getRelocatedClassInternalNamePrefix() + resourceName
                ));
                return Optional.of(string);
            }
        }

        return Optional.empty();
    }

    private static Predicate<String> byResourceName(String resourceName) {
        return name -> name.startsWith(resourceName)
            && (name.length() == resourceName.length() || name.charAt(resourceName.length()) == '/');
    }

    @Override
    public int getOrder() {
        return 100;
    }

}

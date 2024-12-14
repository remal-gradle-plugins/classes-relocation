package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.resource.RelocateResource;

public class ResourceNameHandler implements ImmediateTaskHandler<String, ProcessStringConstant> {

    @Override
    public Optional<String> handle(ProcessStringConstant task, RelocationContext context) {
        val string = task.getString();
        if (string.isEmpty()) {
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
            val prefixDelimPos = classInternalName.lastIndexOf('/');
            val resourceNamePrefix = prefixDelimPos >= 0 ? classInternalName.substring(0, prefixDelimPos + 1) : "";
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

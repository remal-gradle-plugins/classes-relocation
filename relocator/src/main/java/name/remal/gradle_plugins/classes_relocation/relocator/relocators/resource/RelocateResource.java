package name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import org.jspecify.annotations.Nullable;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateResource implements ImmediateTask<String> {

    String resourceName;

    @Nullable
    ClasspathElement currentClasspathElement;

}

package name.remal.gradle_plugins.classes_relocation.relocator.relocators.string_constant;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import lombok.With;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessStringConstant implements ImmediateTask<String> {

    @With
    String string;

    String classInternalName;

    Resource classResource;

}

package name.remal.gradle_plugins.classes_relocation.intern.task.immediate.string_constant;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessStringConstant implements ImmediateTask<String> {

    Resource classResource;

    String classInternalName;

    String string;

}

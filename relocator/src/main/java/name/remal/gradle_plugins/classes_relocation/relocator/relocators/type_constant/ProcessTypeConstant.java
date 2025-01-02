package name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import lombok.With;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import org.objectweb.asm.Type;

@Value
@With
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessTypeConstant implements ImmediateTask<Type> {

    Type type;

    Resource classResource;

}

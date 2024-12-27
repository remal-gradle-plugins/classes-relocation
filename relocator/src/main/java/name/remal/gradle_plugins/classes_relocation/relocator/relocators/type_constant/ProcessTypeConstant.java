package name.remal.gradle_plugins.classes_relocation.relocator.relocators.type_constant;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import org.objectweb.asm.Type;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class ProcessTypeConstant implements ImmediateTask<Type> {

    Type type;

}

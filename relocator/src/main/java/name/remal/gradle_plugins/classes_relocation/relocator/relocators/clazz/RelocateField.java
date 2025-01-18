package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateField implements RelocateClassTask {

    String classInternalName;

    String fieldName;

}

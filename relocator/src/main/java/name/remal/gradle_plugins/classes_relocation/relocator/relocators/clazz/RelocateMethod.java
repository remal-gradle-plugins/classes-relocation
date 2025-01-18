package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;

@Value
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class RelocateMethod implements RelocateClassTask {

    public static RelocateMethod relocateNoArgConstructor(String classInternalName) {
        return new RelocateMethod(classInternalName, "<init>", "()V");
    }


    String classInternalName;

    MethodKey methodKey;

    public RelocateMethod(String classInternalName, MethodKey methodKey) {
        this.classInternalName = classInternalName;
        this.methodKey = methodKey;
    }

    public RelocateMethod(String classInternalName, String methodName, String descriptor) {
        this(classInternalName, methodKeyOf(methodName, descriptor));
    }

}

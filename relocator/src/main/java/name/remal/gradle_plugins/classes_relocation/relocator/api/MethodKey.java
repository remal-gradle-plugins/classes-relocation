package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toMethodParamsDescriptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.gradle.api.tasks.Input;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Value
@Getter(onMethod_ = {@Input, @org.gradle.api.tasks.Optional})
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
@RequiredArgsConstructor(access = PRIVATE)
public class MethodKey implements Comparable<MethodKey>, Serializable {

    public static MethodKey methodKeyOf(String methodName, String methodDescriptorOrParamsDescriptor) {
        return new MethodKey(
            methodName,
            toMethodParamsDescriptor(methodDescriptorOrParamsDescriptor)
        );
    }

    public static MethodKey methodKeyOf(MethodNode methodNode) {
        return methodKeyOf(methodNode.name, methodNode.desc);
    }

    public static MethodKey methodKeyOf(MethodInsnNode methodInsnNode) {
        return methodKeyOf(methodInsnNode.name, methodInsnNode.desc);
    }


    String name;

    String paramsDescriptor;

    @Override
    public String toString() {
        return name + paramsDescriptor;
    }

    @Override
    public int compareTo(MethodKey other) {
        int result = name.compareTo(other.name);
        if (result == 0) {
            result = paramsDescriptor.compareTo(other.paramsDescriptor);
        }
        return result;
    }


    public boolean matches(@Nullable MethodNode methodNode) {
        if (methodNode == null) {
            return false;
        }

        return name.equals(methodNode.name)
            && paramsDescriptor.equals(toMethodParamsDescriptor(methodNode.desc));
    }

    public boolean matches(@Nullable MethodInsnNode methodInsnNode) {
        if (methodInsnNode == null) {
            return false;
        }

        return name.equals(methodInsnNode.name)
            && paramsDescriptor.equals(toMethodParamsDescriptor(methodInsnNode.desc));
    }


    public boolean isStaticInitializer() {
        return name.equals("<clinit>")
            && paramsDescriptor.equals("()");
    }

    public boolean isConstructor() {
        return name.equals("<init>");
    }


    //#region serialization

    private static final long serialVersionUID = 1;

    private Object writeReplace() throws ObjectStreamException {
        return new SerializableValueHolder(name, paramsDescriptor);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class SerializableValueHolder implements Serializable {

        private static final long serialVersionUID = 1;

        private String name;

        private String paramsDescriptor;

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(name);
            out.writeObject(paramsDescriptor);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            name = (String) in.readObject();
            paramsDescriptor = (String) in.readObject();
        }

        private Object readResolve() {
            return new MethodKey(
                requireNonNull(name),
                requireNonNull(paramsDescriptor)
            );
        }

    }

    //#endregion

}

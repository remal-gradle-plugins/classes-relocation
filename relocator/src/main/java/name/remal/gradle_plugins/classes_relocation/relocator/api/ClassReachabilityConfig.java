package name.remal.gradle_plugins.classes_relocation.relocator.api;

import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey.methodKeyOf;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.relocator.reachability.ClassReachabilityConfigUtils.convertClassReachabilityConfigToMap;
import static name.remal.gradle_plugins.classes_relocation.relocator.reachability.ClassReachabilityConfigUtils.convertMapToClassReachabilityConfig;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Tolerate;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.Unmodifiable;

@Value
@Getter(onMethod_ = {@Input, @org.gradle.api.tasks.Optional})
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
@Builder(toBuilder = true, builderClassName = "ClassReachabilityConfigBuilder")
@SuppressWarnings("java:S1948")
public class ClassReachabilityConfig implements Serializable {

    private static final String ON_REACHED_SELF = "<| on reached self |>";


    @NonNull
    String classInternalName;

    @Nullable
    @org.jetbrains.annotations.Nullable
    String onReachedClassInternalName;

    @Unmodifiable
    @Singular
    Set<String> fields;

    @Unmodifiable
    @Singular
    Set<MethodKey> methodsKeys;

    boolean allDeclaredConstructors;

    boolean allPublicConstructors;

    boolean allDeclaredMethods;

    boolean allPublicMethods;

    boolean allDeclaredFields;

    boolean allPublicFields;

    boolean allPermittedSubclasses;


    @Nullable
    public String getOnReachedClassInternalName() {
        if (ON_REACHED_SELF.equals(onReachedClassInternalName)) {
            return getClassInternalName();
        }
        return onReachedClassInternalName;
    }

    public boolean isAlwaysEnabled() {
        return isEmpty(getOnReachedClassInternalName());
    }


    @SuppressWarnings("unused")
    public static class ClassReachabilityConfigBuilder {

        @Tolerate
        public ClassReachabilityConfigBuilder className(String className) {
            return classInternalName(toClassInternalName(className));
        }

        @Tolerate
        public ClassReachabilityConfigBuilder onReachedClass(String className) {
            return onReachedClassInternalName(toClassInternalName(className));
        }

        @Tolerate
        public ClassReachabilityConfigBuilder onReached() {
            return onReachedClassInternalName(ON_REACHED_SELF);
        }

        @Tolerate
        public ClassReachabilityConfigBuilder method(String methodName, String methodParamsDescriptor) {
            return methodsKey(methodKeyOf(methodName, methodParamsDescriptor));
        }

    }


    //#region serialization

    private static final long serialVersionUID = 1;

    private Object writeReplace() throws ObjectStreamException {
        var map = convertClassReachabilityConfigToMap(this);
        return new SerializableValueHolder(map);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class SerializableValueHolder implements Serializable {

        private static final long serialVersionUID = 1;

        private Map<?, ?> map;

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(new LinkedHashMap<>(map));
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            map = (Map<?, ?>) in.readObject();
        }

        private Object readResolve() {
            var map = requireNonNull(this.map);
            var config = convertMapToClassReachabilityConfig(map);
            return requireNonNull(config);
        }

    }

    //#endregion

}

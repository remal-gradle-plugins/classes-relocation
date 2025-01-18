package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static name.remal.gradle_plugins.toolkit.DebugUtils.isDebugEnabled;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Value;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import org.objectweb.asm.tree.ClassNode;

@Value
public class RelocatedClassData {

    String inputClassInternalName;

    Set<String> relocatedFields = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    Set<String> notFoundFields = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    Set<MethodKey> relocatedNonOverrideableMethods = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    Set<MethodKey> relocatedOverrideableMethods = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    Set<MethodKey> notFoundMethods = isDebugEnabled() ? new TreeSet<>() : new LinkedHashSet<>();

    Resource resource;

    ClassNode inputClassNode;

    ClassNode outputClassNode;

    AtomicBoolean relocatedOverrideableMethodsFromNonRelocationClasses = new AtomicBoolean(false);


    public String getOutputClassInternalName() {
        return getOutputClassNode().name;
    }

    public boolean hasProcessedField(String fieldName) {
        return relocatedFields.contains(fieldName)
            || notFoundFields.contains(fieldName);
    }

    public boolean hasProcessedMethod(MethodKey methodKey) {
        return relocatedNonOverrideableMethods.contains(methodKey)
            || relocatedOverrideableMethods.contains(methodKey)
            || notFoundMethods.contains(methodKey);
    }

}

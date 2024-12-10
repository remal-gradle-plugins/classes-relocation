package name.remal.gradle_plugins.classes_relocation.intern.state;

import static java.util.Collections.unmodifiableSet;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.UnmodifiableView;

@RequiredArgsConstructor
public class RelocationTask {

    @Getter
    private final String internalClassName;


    private final Set<String> fieldNames = new LinkedHashSet<>();

    @UnmodifiableView
    public Set<String> getFieldNames() {
        return unmodifiableSet(fieldNames);
    }

    void addFieldName(String fieldName) {
        fieldNames.add(fieldName);
    }


    private final Set<ClassMethodInfo> methods = new LinkedHashSet<>();

    @UnmodifiableView
    public Set<ClassMethodInfo> getMethods() {
        return unmodifiableSet(methods);
    }

    void addMethod(ClassMethodInfo method) {
        methods.add(method);
    }

}

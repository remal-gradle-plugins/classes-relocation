package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Unmodifiable;

@NoArgsConstructor(access = PRIVATE)
final class ClasspathEmpty implements Classpath {

    public static final Classpath EMPTY_CLASSPATH = new ClasspathEmpty();


    @Override
    @Unmodifiable
    public List<ClasspathElement> getElements() {
        return List.of();
    }

    @Override
    @Unmodifiable
    public List<Resource> getAllResources() {
        return List.of();
    }

    @Override
    @Unmodifiable
    public Map<String, @Unmodifiable List<Resource>> getResources() {
        return Map.of();
    }

    @Override
    @Unmodifiable
    public List<Resource> getClassResources(String classNameOrInternalName) {
        return List.of();
    }

    @Override
    @Unmodifiable
    public Set<String> getClassInternalNames() {
        return Set.of();
    }

    @Override
    @Unmodifiable
    public Set<String> getClassNames() {
        return Set.of();
    }

    @Override
    @Unmodifiable
    public Set<String> getPackageNames() {
        return Set.of();
    }

    @Override
    public void close() {
        // do nothing
    }

}

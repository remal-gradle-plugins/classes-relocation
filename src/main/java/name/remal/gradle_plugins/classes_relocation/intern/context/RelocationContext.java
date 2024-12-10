package name.remal.gradle_plugins.classes_relocation.intern.context;


import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import org.jetbrains.annotations.Unmodifiable;

public interface RelocationContext {

    @Unmodifiable
    List<Resource> getSourceResources();

    @Unmodifiable
    List<Resource> getRelocationResources();

    boolean isSourceResource(Resource resource);


    boolean isResourceRelocated(Resource resource);

    void markResourceRelocated(Resource resource);


    @Nullable
    String getModuleIdentifier(ClasspathElement classpathElement);

    @Nullable
    default String getModuleIdentifier(Resource resource) {
        return getModuleIdentifier(resource.getClasspathElement());
    }


    String relocateClassLiteral(String classInternalName);

    String relocateResource(String resourcePath);

    void relocateField(String ownerInternalName, String fieldName, String fieldDescriptor);

    void relocateMethod(String ownerInternalName, String methodName, String methodDescriptor);


    @CheckReturnValue
    RelocationDestination targetEntryFor(Resource resource);

    @CheckReturnValue
    RelocationDestination targetEntryFor(Resource resource, String newPath);

    @Unmodifiable
    Set<String> getTargetEntries();

    boolean hasTargetEntry(String path);

}

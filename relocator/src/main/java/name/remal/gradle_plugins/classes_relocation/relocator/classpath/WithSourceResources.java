package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

public interface WithSourceResources {

    @Unmodifiable
    List<Resource> getSourceResources();

}

package name.remal.gradle_plugins.classes_relocation.relocator;

import java.util.Collection;
import lombok.val;

public class SourceResourcesInRelocationPackagesException extends ClassesRelocationException {

    private static String createMessage(Collection<String> sourcePackageNamesToRelocate) {
        val sb = new StringBuilder();
        sb.append("Source JAR contains packages for relocation:");
        sourcePackageNamesToRelocate.forEach(resource -> sb.append("\n  ").append(resource));
        sb.append("\n\nIf you want to redefine resources for relocation"
            + ", create a separate dependency and relocate it.\n");
        return sb.toString();
    }

    SourceResourcesInRelocationPackagesException(Collection<String> sourcePackageNamesToRelocate) {
        super(createMessage(sourcePackageNamesToRelocate));
    }

}

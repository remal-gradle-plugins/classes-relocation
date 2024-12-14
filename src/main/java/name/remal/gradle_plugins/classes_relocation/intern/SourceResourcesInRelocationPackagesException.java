package name.remal.gradle_plugins.classes_relocation.intern;

import static name.remal.gradle_plugins.classes_relocation.ClassesRelocationPlugin.CLASSES_RELOCATION_CONFIGURATION_NAME;

import java.util.Collection;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.ClassesRelocationException;

public class SourceResourcesInRelocationPackagesException extends ClassesRelocationException {

    private static String createMessage(Collection<String> sourcePackageNamesToRelocate) {
        val sb = new StringBuilder();
        sb.append("Source JAR contains packages for relocation:");
        sourcePackageNamesToRelocate.forEach(resource -> sb.append("\n  ").append(resource));
        sb.append("\n\nIf you want to redefine resources for relocation"
                + ", create a separate dependency and add it to `")
            .append(CLASSES_RELOCATION_CONFIGURATION_NAME)
            .append("` configuration.\n");
        return sb.toString();
    }

    SourceResourcesInRelocationPackagesException(Collection<String> sourcePackageNamesToRelocate) {
        super(createMessage(sourcePackageNamesToRelocate));
    }

}

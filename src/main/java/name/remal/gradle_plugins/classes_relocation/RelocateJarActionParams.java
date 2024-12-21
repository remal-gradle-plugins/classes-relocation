package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

interface RelocateJarActionParams extends WorkParameters {

    RegularFileProperty getJarFile();

    ConfigurableFileCollection getRelocationClasspath();

    MapProperty<String, String> getModuleIdentifiers();


    RegularFileProperty getTargetJarFile();

    Property<String> getBasePackageForRelocatedClasses();

    Property<String> getMetadataCharset();

    Property<Boolean> getPreserveFileTimestamps();

}

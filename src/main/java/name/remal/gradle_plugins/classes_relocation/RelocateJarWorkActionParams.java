package name.remal.gradle_plugins.classes_relocation;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

interface RelocateJarWorkActionParams extends WorkParameters {

    RegularFileProperty getJarFile();

    ConfigurableFileCollection getRelocationClasspath();

    ConfigurableFileCollection getCompileAndRuntimeClasspath();

    ConfigurableFileCollection getSystemClasspath();

    ConfigurableFileCollection getReachabilityMetadataClasspath();

    MapProperty<String, String> getModuleIdentifiers();


    RegularFileProperty getTargetJarFile();

    Property<String> getMetadataCharset();

    Property<Boolean> getPreserveFileTimestamps();


    Property<ClassesRelocationSettings> getSettings();

}

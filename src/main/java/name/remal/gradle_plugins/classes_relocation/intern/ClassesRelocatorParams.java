package name.remal.gradle_plugins.classes_relocation.intern;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceProcessor;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourcesMerger;

@SuperBuilder
@SuppressWarnings({"cast", "java:S1170"})
public abstract class ClassesRelocatorParams {

    public static final Charset DEFAULT_METADATA_CHARSET = UTF_8;


    @NonNull
    protected final Path sourceJarPath;

    @Singular
    protected final List<Path> relocationClasspathPaths;

    @Singular
    protected final List<ResourcesMerger> resourcesMergers;

    @Singular
    protected final List<ResourceProcessor> resourceProcessors;

    @Singular
    protected final List<Path> runtimeClasspathPaths;

    @Singular
    protected final List<Path> compileClasspathPaths;

    @Singular
    protected final Map<URI, String> moduleIdentifiers;


    @NonNull
    protected final Path targetJarPath;

    @NonNull
    protected final String basePackageForRelocatedClasses;

    @Default
    protected final Charset metadataCharset = DEFAULT_METADATA_CHARSET;

    protected final boolean preserveFileTimestamps;

    protected final boolean relocateResources;

}

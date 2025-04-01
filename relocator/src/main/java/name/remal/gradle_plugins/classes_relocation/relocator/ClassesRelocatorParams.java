package name.remal.gradle_plugins.classes_relocation.relocator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorObjectFactoryDefault.DEFAULT_OBJECT_FACTORY;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorConfig;
import org.jetbrains.annotations.Unmodifiable;

@SuperBuilder(toBuilder = true)
@Getter
@SuppressWarnings({"cast", "java:S1170"})
public abstract class ClassesRelocatorParams {

    public static final Charset DEFAULT_METADATA_CHARSET = UTF_8;


    @NonNull
    protected final Path sourceJarPath;

    @Unmodifiable
    @Singular
    protected final List<Path> relocationClasspathPaths;

    @Unmodifiable
    @Singular
    protected final List<Path> compileAndRuntimeClasspathPaths;

    @Unmodifiable
    @Singular
    protected final List<Path> systemClasspathPaths;

    @Unmodifiable
    @Singular
    protected final List<Path> reachabilityMetadataClasspathPaths;

    @Unmodifiable
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


    @Default
    protected final ClassesRelocatorConfig config = ClassesRelocatorConfig.builder().build();


    @Default
    protected final ClassesRelocatorObjectFactory objectFactory = DEFAULT_OBJECT_FACTORY;

}

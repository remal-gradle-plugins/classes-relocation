package name.remal.gradle_plugins.classes_relocation.intern;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@SuppressWarnings({"cast", "java:S1170"})
public abstract class ClassesRelocatorParams {

    public static final Charset DEFAULT_METADATA_CHARSET = UTF_8;


    @NonNull
    protected final Path sourceJarPath;

    @Singular
    protected final Set<Path> relocationClasspathPaths;

    @Singular
    protected final Set<Path> runtimeClasspathPaths;

    @Singular
    protected final Set<Path> compileClasspathPaths;

    @Singular
    protected final Map<URI, String> moduleIdentifiers;


    @NonNull
    protected final Path targetJarPath;

    @NonNull
    protected final String basePackageForRelocatedClasses;

    @Default
    protected final Charset metadataCharset = DEFAULT_METADATA_CHARSET;

    protected final boolean preserveFileTimestamps;

}

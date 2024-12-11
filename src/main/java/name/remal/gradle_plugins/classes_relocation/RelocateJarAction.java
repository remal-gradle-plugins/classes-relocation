package name.remal.gradle_plugins.classes_relocation;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocatorParams.DEFAULT_METADATA_CHARSET;
import static name.remal.gradle_plugins.toolkit.UriUtils.parseUri;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocator;
import org.gradle.workers.WorkAction;

@NoArgsConstructor(onConstructor_ = {@Inject})
@CustomLog
abstract class RelocateJarAction implements WorkAction<RelocateJarActionParams> {

    @Override
    public void execute() {
        val params = getParameters();
        try (
            val relocator = ClassesRelocator.builder()
                .sourceJarPath(params.getJarFile().get().getAsFile().toPath())
                .relocationClasspathPaths(params.getRelocationClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList())
                )
                .runtimeClasspathPaths(params.getRuntimeClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList())
                )
                .compileClasspathPaths(params.getCompileClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList())
                )
                .moduleIdentifiers(params.getModuleIdentifiers().get().entrySet().stream()
                    .collect(toImmutableMap(entry -> parseUri(entry.getKey()), Entry::getValue))
                )

                .targetJarPath(params.getTargetJarFile().get().getAsFile().toPath())
                .basePackageForRelocatedClasses(params.getBasePackageForRelocatedClasses().get())
                .metadataCharset(params.getMetadataCharset()
                    .map(Charset::forName)
                    .getOrElse(DEFAULT_METADATA_CHARSET)
                )
                .preserveFileTimestamps(params.getPreserveFileTimestamps().getOrElse(true))

                .build()
        ) {
            relocator.relocate();
        }
    }

}

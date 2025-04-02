package name.remal.gradle_plugins.classes_relocation;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.GradleClassesRelocatorObjectFactoryHolder.GradleClassesRelocatorObjectFactory;
import static name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorParams.DEFAULT_METADATA_CHARSET;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.UriUtils.parseUri;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocator;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassFilter;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MinimizationConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ResourcesFilter;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.workers.WorkAction;

@NoArgsConstructor(onConstructor_ = {@Inject})
@CustomLog
abstract class RelocateJarWorkAction implements WorkAction<RelocateJarWorkActionParams> {

    @Override
    @SneakyThrows
    public void execute() {
        var params = getParameters();
        var settings = params.getSettings().get();
        var minimizeSettings = settings.getMinimize();
        try (
            var relocator = ClassesRelocator.builder()
                .sourceJarPath(params.getJarFile().get().getAsFile().toPath())
                .relocationClasspathPaths(params.getRelocationClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList())
                )
                .systemClasspathPaths(params.getSystemClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList()))
                .reachabilityMetadataClasspathPaths(params.getReachabilityMetadataClasspath().getFiles().stream()
                    .map(File::toPath)
                    .collect(toList()))
                .moduleIdentifiers(params.getModuleIdentifiers().get().entrySet().stream()
                    .collect(toImmutableMap(entry -> parseUri(entry.getKey()), Entry::getValue))
                )

                .targetJarPath(params.getTargetJarFile().get().getAsFile().toPath())
                .basePackageForRelocatedClasses(settings.getBasePackageForRelocatedClasses().get())
                .metadataCharset(params.getMetadataCharset()
                    .map(Charset::forName)
                    .getOrElse(DEFAULT_METADATA_CHARSET)
                )
                .preserveFileTimestamps(params.getPreserveFileTimestamps().getOrElse(true))

                .config(ClassesRelocatorConfig.builder()
                    .logDynamicReflectionUsage(settings.getLogDynamicReflectionUsage().getOrElse(false))
                    .minimization(MinimizationConfig.builder()
                        .keepResourcesFilter(new ResourcesFilter()
                            .includeClasses(minimizeSettings.getKeepClasses().get())
                        )
                        .keepAnnotationsFilter(new ClassFilter()
                            .include(minimizeSettings.getKeepMembersAnnotatedWith().get())
                        )
                        .classReachabilityConfigs(minimizeSettings.getClassReachabilityConfigs().get())
                        .build()
                    )
                    .build()
                )

                .objectFactory(getObjects().newInstance(GradleClassesRelocatorObjectFactory.class))

                .build()
        ) {
            relocator.relocate();

            var reachabilityReportPath = getParameters().getReachabilityReportFile()
                .map(RegularFile::getAsFile)
                .map(File::toPath)
                .getOrNull();
            if (reachabilityReportPath != null) {
                var reachabilityReport = relocator.getReachabilityReport();
                createParentDirectories(reachabilityReportPath);
                try (var writer = newBufferedWriter(reachabilityReportPath, UTF_8)) {
                    reachabilityReport.renderTo(writer);
                }
            }
        }
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}

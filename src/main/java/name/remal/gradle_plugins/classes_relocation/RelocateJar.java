package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static name.remal.gradle_plugins.classes_relocation.ClassRelocationForkOptions.IS_FORK_ENABLED_DEFAULT;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.getModuleVersionIdentifiersForFilesIn;
import static name.remal.gradle_plugins.toolkit.JavaLauncherUtils.getJavaLauncherProviderFor;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

@CacheableTask
public abstract class RelocateJar extends DefaultTask implements ClassesRelocationSettings {

    @InputFile
    @PathSensitive(RELATIVE)
    public abstract RegularFileProperty getJarFile();

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getCompileClasspath();

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getRelocationClasspath();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Boolean> getPreserveFileTimestamps();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<String> getMetadataCharset();


    @OutputFile
    public abstract RegularFileProperty getTargetJarFile();

    {
        getTargetJarFile().convention(getLayout().getBuildDirectory()
            .dir(getName()).flatMap(dir ->
                dir.file(getJarFile()
                    .map(RegularFile::getAsFile)
                    .map(File::getName)
                )
            )
        );
    }


    @Getter
    @Nested
    private final ClassRelocationForkOptions forkOptions = getObjects().newInstance(ClassRelocationForkOptions.class);

    @org.gradle.api.tasks.Optional
    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();

    {
        getJavaLauncher().convention(getJavaLauncherProviderFor(getProject()));
    }


    @TaskAction
    public void relocateClasses() {
        if (getRelocationClasspath().isEmpty()) {
            getFiles().copy(spec -> {
                spec.from(getJarFile());
                spec.into(getTargetJarFile()
                    .map(RegularFile::getAsFile)
                    .map(File::getAbsoluteFile)
                    .map(File::getParentFile)
                );
            });
            return;
        }


        final WorkQueue workQueue;
        {
            val forkParams = Optional.of(getForkOptions());
            val isForkEnabled = Optional.of(getForkOptions())
                .map(ClassRelocationForkOptions::getEnabled)
                .map(Provider::getOrNull)
                .orElse(IS_FORK_ENABLED_DEFAULT);
            if (isForkEnabled) {
                workQueue = getWorkerExecutor().processIsolation(spec -> {
                    spec.getForkOptions().setExecutable(getJavaLauncher().get()
                        .getExecutablePath()
                        .getAsFile()
                        .getAbsolutePath()
                    );
                    spec.getForkOptions().setMaxHeapSize(forkParams
                        .map(ClassRelocationForkOptions::getMaxHeapSize)
                        .map(Provider::getOrNull)
                        .orElse(null)
                    );
                });

            } else {
                workQueue = getWorkerExecutor().classLoaderIsolation(spec -> {
                });
            }
        }

        workQueue.submit(RelocateJarAction.class, params -> {
            params.getJarFile().set(getJarFile());
            params.getRelocationClasspath().setFrom(getRelocationClasspath());
            params.getRuntimeClasspath().setFrom(getRuntimeClasspath());
            params.getCompileClasspath().setFrom(getCompileClasspath());
            params.getModuleIdentifiers().putAll(getProviders().provider(() -> {
                val moduleIdentifiers = new LinkedHashMap<String, String>();
                Stream.of(
                    getRelocationClasspath(),
                    getRuntimeClasspath(),
                    getCompileClasspath()
                ).forEach(fileCollection ->
                    getModuleVersionIdentifiersForFilesIn(fileCollection).forEach((file, id) ->
                        moduleIdentifiers.putIfAbsent(
                            file.toPath().toUri().toString(),
                            format(
                                "%s:%s",
                                id.getGroup(),
                                id.getModule()
                            )
                        )
                    )
                );
                return moduleIdentifiers;
            }));

            params.getTargetJarFile().set(getTargetJarFile());
            params.getBasePackageForRelocatedClasses().set(getBasePackageForRelocatedClasses());
            params.getMetadataCharset().set(getMetadataCharset());
            params.getPreserveFileTimestamps().set(getPreserveFileTimestamps());
        });
    }


    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProjectLayout getLayout();

    @Inject
    protected abstract FileSystemOperations getFiles();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Inject
    protected abstract ProviderFactory getProviders();

}

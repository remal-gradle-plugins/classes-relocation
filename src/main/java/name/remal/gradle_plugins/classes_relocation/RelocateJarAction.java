package name.remal.gradle_plugins.classes_relocation;

import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;

import java.io.File;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class RelocateJarAction implements Action<Task>, ClassesRelocationSettings {

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getRelocationClasspath();

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getCompileAndRuntimeClasspath();

    @InputFiles
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getReachabilityMetadataClasspath();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract MapProperty<String, String> getModuleIdentifiers();

    @org.gradle.api.tasks.Optional
    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();


    @org.gradle.api.tasks.Optional
    @OutputFile
    public abstract RegularFileProperty getReachabilityReportFile();


    public void execute(Task task) {
        execute((Jar) task);
    }

    @SneakyThrows
    private void execute(Jar task) {
        var reachabilityReportFile = getReachabilityReportFile().getAsFile().getOrNull();
        if (reachabilityReportFile != null) {
            deleteRecursively(reachabilityReportFile.toPath());
        }

        if (getRelocationClasspath().isEmpty()) {
            return;
        }

        var jarFile = task.getArchiveFile().get().getAsFile().getAbsoluteFile();
        var renamedJarFile = new File(
            jarFile.getParentFile(),
            "pre-relocation-" + jarFile.getName()
        );
        move(jarFile.toPath(), renamedJarFile.toPath(), REPLACE_EXISTING);

        final WorkQueue workQueue;
        {
            boolean isForkEnabled = getFork().getEnabled().get();
            if (isForkEnabled) {
                workQueue = getWorkerExecutor().processIsolation(spec -> {
                    spec.getForkOptions().setExecutable(getJavaLauncher().get()
                        .getExecutablePath()
                        .getAsFile()
                        .getAbsolutePath()
                    );
                    if (getJavaLauncher().get().getMetadata().getLanguageVersion().canCompileOrRun(9)) {
                        spec.getForkOptions().jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED");
                    }
                    spec.getForkOptions().setMaxHeapSize(getFork().getMaxHeapSize().getOrNull());
                });

            } else {
                workQueue = getWorkerExecutor().classLoaderIsolation(spec -> {
                });
            }
        }

        workQueue.submit(RelocateJarWorkAction.class, params -> {
            params.getJarFile().set(renamedJarFile);
            params.getRelocationClasspath().setFrom(getRelocationClasspath());
            params.getCompileAndRuntimeClasspath().setFrom(getCompileAndRuntimeClasspath());
            params.getJvmInstallationDir().set(getJavaLauncher()
                .map(JavaLauncher::getMetadata)
                .map(JavaInstallationMetadata::getInstallationPath)
            );
            params.getReachabilityMetadataClasspath().setFrom(getReachabilityMetadataClasspath());
            params.getModuleIdentifiers().set(getModuleIdentifiers());

            params.getTargetJarFile().set(jarFile);
            params.getMetadataCharset().set(task.getMetadataCharset());
            params.getPreserveFileTimestamps().set(task.isPreserveFileTimestamps());

            var settings = getObjects().newInstance(ClassesRelocationSettings.class);
            copyManagedProperties(ClassesRelocationSettings.class, this, settings);
            params.getSettings().set(settings);

            params.getReachabilityReportFile().set(reachabilityReportFile);
        });
    }


    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ObjectFactory getObjects();

}

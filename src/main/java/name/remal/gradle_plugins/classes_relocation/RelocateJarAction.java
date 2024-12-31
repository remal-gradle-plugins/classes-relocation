package name.remal.gradle_plugins.classes_relocation;

import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.classes_relocation.ClassRelocationForkOptions.IS_FORK_ENABLED_DEFAULT;

import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class RelocateJarAction implements Action<Task>, ClassesRelocationSettings {

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getRelocationClasspath();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract MapProperty<String, String> getModuleIdentifiers();

    @org.gradle.api.tasks.Optional
    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();

    public void execute(Task task) {
        execute((Jar) task);
    }

    @SneakyThrows
    private void execute(Jar task) {
        if (getRelocationClasspath().isEmpty()) {
            return;
        }

        val jarFile = task.getArchiveFile().get().getAsFile().getAbsoluteFile();
        val renamedJarFile = new File(
            jarFile.getParentFile(),
            "pre-relocation-" + jarFile.getName()
        );
        move(jarFile.toPath(), renamedJarFile.toPath(), REPLACE_EXISTING);

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
                    if (getJavaLauncher().get().getMetadata().getLanguageVersion().canCompileOrRun(9)) {
                        spec.getForkOptions().jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED");
                    }
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

        workQueue.submit(RelocateJarWorkAction.class, params -> {
            params.getJarFile().set(renamedJarFile);
            params.getRelocationClasspath().setFrom(getRelocationClasspath());
            params.getModuleIdentifiers().set(getModuleIdentifiers());

            params.getTargetJarFile().set(jarFile);
            params.getBasePackageForRelocatedClasses().set(getBasePackageForRelocatedClasses());
            params.getMetadataCharset().set(task.getMetadataCharset());
            params.getPreserveFileTimestamps().set(task.isPreserveFileTimestamps());
        });
    }


    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

}

package name.remal.gradle_plugins.classes_relocation;

import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

@CacheableTask
public abstract class RelocatedJar extends DefaultTask {

    @InputFile
    @PathSensitive(RELATIVE)
    public abstract RegularFileProperty getJarFile();

    @InputFiles
    @Classpath
    @org.gradle.api.tasks.Optional
    public abstract ConfigurableFileCollection getRelocationClasspath();


    @OutputFile
    public abstract RegularFileProperty getOutputJarFile();

    {
        getOutputJarFile().convention(getLayout().getBuildDirectory()
            .dir(getName()).flatMap(dir ->
                dir.file(getJarFile()
                    .map(RegularFile::getAsFile)
                    .map(File::getName)
                )
            )
        );
    }


    @TaskAction
    public void relocateClasses() {

    }


    @Inject
    protected abstract ProjectLayout getLayout();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Inject
    protected abstract ProviderFactory getProviders();

}

package name.remal.gradle_plugins.classes_relocation;

import static lombok.AccessLevel.PRIVATE;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.plugin.devel.tasks.ValidatePlugins;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;

@NoArgsConstructor(access = PRIVATE)
abstract class TaskClasspathConfigurers {

    public static final List<TaskClasspathConfigurer<?>> TASK_CLASSPATH_CONFIGURERS = ImmutableList.of(
        new TaskClasspathFileCollectionConfigurer<>(
            Test.class,
            Test::getClasspath,
            Test::setClasspath
        ),
        new TaskClasspathConfigurableFileCollectionConfigurer<>(
            PluginUnderTestMetadata.class,
            PluginUnderTestMetadata::getPluginClasspath
        ),
        new TaskClasspathConfigurableFileCollectionConfigurer<>(
            ValidatePlugins.class,
            ValidatePlugins::getClasspath
        ),
        new TaskClasspathConfigurableFileCollectionConfigurer<>(
            JacocoReportBase.class,
            JacocoReportBase::getClassDirectories
        ),
        new TaskClasspathConfigurableFileCollectionConfigurer<>(
            JacocoReportBase.class,
            JacocoReportBase::getAdditionalClassDirs
        ),
        new TaskClasspathFileCollectionConfigurer<>(
            CreateStartScripts.class,
            CreateStartScripts::getClasspath,
            CreateStartScripts::setClasspath
        ),
        new TaskClasspathFileCollectionConfigurer<>(
            JavaExec.class,
            task -> task.getName().equals(TASK_RUN_NAME)
                && task.getProject().getPluginManager().hasPlugin("application"),
            JavaExec::getClasspath,
            JavaExec::setClasspath
        )
    );

}

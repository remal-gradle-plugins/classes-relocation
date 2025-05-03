package name.remal.gradle_plugins.classes_relocation;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.SourceSetUtils.isCompiledBy;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.util.List;
import lombok.NoArgsConstructor;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CodeNarc;
import org.gradle.api.plugins.quality.Pmd;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.plugin.devel.tasks.ValidatePlugins;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;

@NoArgsConstructor(access = PRIVATE)
abstract class TaskClasspathConfigurers {

    public static final List<TaskClasspathConfigurer<?>> TASK_CLASSPATH_CONFIGURERS = List.of(
        new TaskClasspathFileCollectionConfigurer<>(
            AbstractCompile.class,
            task -> {
                var project = task.getProject();
                var sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
                if (sourceSets == null) {
                    return false;
                }
                var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
                var compilesMainSourceSet = isCompiledBy(mainSourceSet, task);
                return !compilesMainSourceSet;
            },
            AbstractCompile::getClasspath,
            AbstractCompile::setClasspath
        ),
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
            JavaExec::getClasspath,
            JavaExec::setClasspath
        ),

        new TaskClasspathFileCollectionConfigurer<>(
            Checkstyle.class,
            Checkstyle::getClasspath,
            Checkstyle::setClasspath
        ),
        new TaskClasspathFileCollectionConfigurer<>(
            CodeNarc.class,
            CodeNarc::getCompilationClasspath,
            CodeNarc::setCompilationClasspath
        ),
        new TaskClasspathFileCollectionConfigurer<>(
            Pmd.class,
            Pmd::getClasspath,
            Pmd::setClasspath
        )
    );

}

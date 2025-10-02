package name.remal.gradle_plugins.classes_relocation;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils.getJvmLanguagesCompileTaskProperties;
import static name.remal.gradle_plugins.toolkit.JvmLanguageCompilationUtils.isJvmLanguageCompileTask;
import static name.remal.gradle_plugins.toolkit.LazyNullableValue.lazyNullableValue;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsBiConsumer;
import static name.remal.gradle_plugins.toolkit.SourceSetUtils.isCompiledBy;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.lang.reflect.Field;
import java.util.List;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.toolkit.LazyNullableValue;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CodeNarc;
import org.gradle.api.plugins.quality.Pmd;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.plugin.devel.tasks.ValidatePlugins;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;

@NoArgsConstructor(access = PRIVATE)
abstract class TaskClasspathConfigurers {

    private static final LazyNullableValue<Field> TEST_STABLE_CLASSPATH_FIELD = lazyNullableValue(() -> {
        final Field field;
        try {
            field = makeAccessible(Test.class.getDeclaredField("stableClasspath"));
        } catch (NoSuchFieldException ignored) {
            return null;
        }

        if (!ConfigurableFileCollection.class.isAssignableFrom(field.getType())) {
            return null;
        }

        return field;
    });


    public static final List<TaskClasspathConfigurer<?>> TASK_CLASSPATH_CONFIGURERS = List.of(
        new TaskClasspathFileCollectionConfigurer<>(
            Task.class,
            task -> {
                if (!isJvmLanguageCompileTask(task)) {
                    return false;
                }

                var sourceSets = task.getProject().getExtensions().findByType(SourceSetContainer.class);
                if (sourceSets == null) {
                    return false;
                }
                var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
                var compilesMainSourceSet = isCompiledBy(mainSourceSet, task);
                return !compilesMainSourceSet;
            },
            task -> requireNonNull(getJvmLanguagesCompileTaskProperties(task)).getClasspath(),
            (task, classpath) -> requireNonNull(getJvmLanguagesCompileTaskProperties(task)).setClasspath(classpath)
        ),
        new TaskClasspathFileCollectionConfigurer<>(
            Test.class,
            Test::getClasspath,
            sneakyThrowsBiConsumer((task, classpath) -> {
                task.setClasspath(classpath);

                var stableClasspathField = TEST_STABLE_CLASSPATH_FIELD.get();
                if (stableClasspathField != null) {
                    var stableClasspath = (ConfigurableFileCollection) stableClasspathField.get(task);
                    stableClasspath.setFrom(classpath);
                }
            })
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

package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static name.remal.gradle_plugins.toolkit.testkit.TaskValidations.executeOnlyIfSpecs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.MinTestableGradleVersion;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class ClassesRelocationPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ClassesRelocationPlugin.class);
        project.getPluginManager().apply("java");
    }

    @Test
    @MinTestableGradleVersion("8.0")
    void jarTaskProperties() {
        var jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);
        var jarInputs = jar.getInputs();
        assertDoesNotThrow(jarInputs::getProperties);
    }

    @Test
    void compileTestJavaClasspath() {
        var compileTask = project.getTasks().named("compileTestJava", AbstractCompile.class).get();
        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        var jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);

        executeOnlyIfSpecs(compileTask);
        assertThat(compileTask.getClasspath().getFiles())
            .as("%s: %s", compileTask, "getClasspath")
            .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
            .contains(jar.getArchiveFile().get().getAsFile());
    }

    @Test
    void testClasspath() {
        var testTasks = new ArrayList<>(project.getTasks().withType(org.gradle.api.tasks.testing.Test.class));
        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        var jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);

        assertThat(testTasks).as("testTasks")
            .isNotEmpty()
            .allSatisfy(testTask -> {
                executeOnlyIfSpecs(testTask);
                assertThat(testTask.getClasspath().getFiles())
                    .as("%s: %s", testTask, "getClasspath")
                    .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
                    .contains(jar.getArchiveFile().get().getAsFile());
            });
    }

    @Test
    void pluginUnderTestMetadataClasspath() {
        project.getPluginManager().apply("java-gradle-plugin");

        var metadataTasks = new ArrayList<>(project.getTasks().withType(PluginUnderTestMetadata.class));
        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        var jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);

        assertThat(metadataTasks).as("metadataTasks")
            .isNotEmpty()
            .allSatisfy(metadataTask -> {
                executeOnlyIfSpecs(metadataTask);
                assertThat(metadataTask.getPluginClasspath().getFiles())
                    .as("%s: %s", metadataTask, "pluginClasspath")
                    .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
                    .contains(jar.getArchiveFile().get().getAsFile());
            });
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(ClassesRelocationPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                var taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}

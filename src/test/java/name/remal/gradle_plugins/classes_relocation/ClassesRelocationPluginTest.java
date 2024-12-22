package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.classes_relocation.ClassesRelocationPlugin.RELOCATED_JAR_TASK_NAME;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;

import java.util.ArrayList;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
@CustomLog
class ClassesRelocationPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ClassesRelocationPlugin.class);
        project.getPluginManager().apply("java");
    }

    @Test
    void mainSourceSetClasspaths() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        val relocateJar = project.getTasks().withType(RelocateJar.class).getByName(RELOCATED_JAR_TASK_NAME);
        assertThat(mainSourceSet.getCompileClasspath().getFiles())
            .as("%s: %s", mainSourceSet, "compileClasspath")
            .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
            .doesNotContain(relocateJar.getTargetJarFile().get().getAsFile());

        assertThat(mainSourceSet.getRuntimeClasspath().getFiles())
            .as("%s: %s", mainSourceSet, "runtimeClasspath")
            .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
            .contains(relocateJar.getTargetJarFile().get().getAsFile());
    }

    @Test
    void testSourceSetClasspaths() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val relocateJar = project.getTasks().withType(RelocateJar.class).getByName(RELOCATED_JAR_TASK_NAME);
        assertThat(testSourceSet.getCompileClasspath().getFiles())
            .as("%s: %s", testSourceSet, "compileClasspath")
            .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
            .contains(relocateJar.getTargetJarFile().get().getAsFile());

        assertThat(testSourceSet.getRuntimeClasspath().getFiles())
            .as("%s: %s", testSourceSet, "runtimeClasspath")
            .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
            .contains(relocateJar.getTargetJarFile().get().getAsFile());
    }

    @Test
    void pluginUnderTestMetadataClasspath() {
        project.getPluginManager().apply("java-gradle-plugin");

        val metadataTasks = new ArrayList<>(project.getTasks().withType(PluginUnderTestMetadata.class));
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        val relocateJar = project.getTasks().withType(RelocateJar.class).getByName(RELOCATED_JAR_TASK_NAME);
        assertThat(metadataTasks).as("metadataTasks")
            .isNotEmpty()
            .allSatisfy(metadataTask -> {
                assertThat(metadataTask.getPluginClasspath().getFiles())
                    .as("%s: %s", metadataTask, "pluginClasspath")
                    .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles())
                    .contains(relocateJar.getTargetJarFile().get().getAsFile());
            });
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        val taskClassNamePrefix = packageNameOf(ClassesRelocationPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                val taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}

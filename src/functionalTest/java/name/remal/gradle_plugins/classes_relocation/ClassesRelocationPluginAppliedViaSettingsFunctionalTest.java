package name.remal.gradle_plugins.classes_relocation;

import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

class ClassesRelocationPluginAppliedViaSettingsFunctionalTest {

    final GradleProject project;

    ClassesRelocationPluginAppliedViaSettingsFunctionalTest(GradleProject project) {
        this.project = project;
    }

    @Test
    void appliedViaSettingsIsAppliedToProject() {
        project.forSettingsFile(settings -> settings.applyPlugin("name.remal.classes-relocation"));

        // The plugin must NOT be applied via the project's build file: it should reach the project
        // solely through the Settings-level application propagating via GradleLifecycle.beforeProject.
        // `hasPlugin` is evaluated at configuration time and captured into a local variable, because
        // accessing `Task.project` at execution time is unsupported with the configuration cache.
        project.getBuildFile().line(
            "def isPluginApplied = pluginManager.hasPlugin('name.remal.classes-relocation')"
        );
        project.getBuildFile().line(
            "tasks.register('assertPluginApplied') { doLast { assert isPluginApplied } }"
        );

        project.assertBuildSuccessfully("assertPluginApplied");
    }

}

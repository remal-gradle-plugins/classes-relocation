package name.remal.gradle_plugins.classes_relocation;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.io.File.pathSeparator;
import static java.lang.String.format;
import static java.lang.String.join;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static name.remal.gradle_plugins.toolkit.StringUtils.escapeGroovy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Splitter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.val;
import name.remal.gradle_plugins.toolkit.generators.BaseGroovyFileContent;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import name.remal.gradle_plugins.toolkit.testkit.functional.SuppressedMessage;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassesRelocationPluginFunctionalTest {

    private static final AtomicInteger LAST_TEST_NUMBER = new AtomicInteger(0);

    final GradleProject project;
    final Path mavenRepoPath;

    ClassesRelocationPluginFunctionalTest(GradleProject project) {
        this.project = project;
        this.mavenRepoPath = project.getProjectDir().toPath().resolve(".m2");
    }


    final String groupId = "name.remal.classes-relocation.tests";
    final String artifactId = "functional";
    final String version = LAST_TEST_NUMBER.incrementAndGet() + "";

    private Path getPublishedJarPath() {
        return mavenRepoPath
            .resolve(groupId.replace('.', '/'))
            .resolve(artifactId)
            .resolve(version)
            .resolve(format("%s-%s.jar", artifactId, version));
    }


    @BeforeEach
    void beforeEach() {
        project.forSettingsFile(settings -> {
            settings.append("rootProject.name = '" + escapeGroovy(artifactId) + "'");
        });

        project.forBuildFile(build -> {
            build.append("group = '" + escapeGroovy(groupId) + "'");
            build.append("version = '" + escapeGroovy(version) + "'");

            build.applyPlugin("name.remal.classes-relocation");
            build.applyPlugin("java");

            build.appendBlock("classesRelocation", classesRelocation -> {
                classesRelocation.append("basePackageForRelocatedClasses = 'relocated'");
            });
        });

        project.addSuppressedDeprecationMessage(SuppressedMessage.builder()
            .startsWith(true)
            .message("The runtime configuration has been deprecated for artifact declaration")
            .minGradleVersion(GradleVersion.version("6.8"))
            .maxGradleVersion(GradleVersion.version("6.9.9999"))
            .build()
        );
    }


    @Test
    void testing() throws Throwable {
        addLibraryToDependencies(project.getBuildFile(), "guava", "classesRelocation");

        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return ImmutableList.of(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-api", "testImplementation");
        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-engine", "testRuntimeOnly");
        addLibraryToDependencies(project.getBuildFile(), "junit-platform-launcher", "testRuntimeOnly");

        project.getBuildFile().appendBlock("tasks.withType(Test).configureEach", task -> {
            task.append("useJUnitPlatform()");
            task.append("enableAssertions = true");
        });

        project.writeTextFile("src/test/java/pkg/LogicTest.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import static org.junit.jupiter.api.Assertions.*;",
            "import static java.util.Arrays.*;",
            "",
            "import org.junit.jupiter.api.Test;",
            "",
            "public class LogicTest {",
            "",
            "    @Test",
            "    void test() {",
            "        assertThrows(ClassNotFoundException.class, () ->",
            "            // this class should NOT be available here",
            "            Class.forName(\"com.google.common.collect.ImmutableList\")",
            "        );",
            "",
            "        assertEquals(asList(\"a\", \"b\", \"c\"), Logic.execute());",
            "    }",
            "",
            "}",
        }));

        project.getBuildFile().registerDefaultTask("test");

        project.assertBuildSuccessfully();
    }

    @Test
    void withoutRelocation() throws Throwable {
        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import static java.util.Arrays.*;",
            "",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return asList(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-api", "testImplementation");
        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-engine", "testRuntimeOnly");
        addLibraryToDependencies(project.getBuildFile(), "junit-platform-launcher", "testRuntimeOnly");

        project.getBuildFile().appendBlock("tasks.withType(Test).configureEach", task -> {
            task.append("useJUnitPlatform()");
            task.append("enableAssertions = true");
        });

        project.writeTextFile("src/test/java/pkg/LogicTest.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import static org.junit.jupiter.api.Assertions.*;",
            "import static java.util.Arrays.*;",
            "",
            "import org.junit.jupiter.api.Test;",
            "",
            "public class LogicTest {",
            "",
            "    @Test",
            "    void test() {",
            "        assertThrows(ClassNotFoundException.class, () ->",
            "            // this class should NOT be available here",
            "            Class.forName(\"com.google.common.collect.ImmutableList\")",
            "        );",
            "",
            "        assertEquals(asList(\"a\", \"b\", \"c\"), Logic.execute());",
            "    }",
            "",
            "}",
        }));

        project.getBuildFile().registerDefaultTask("test");

        project.assertBuildSuccessfully();
    }

    @Test
    void dependencyInAnotherProject() {
        addLibraryToDependencies(project.getBuildFile(), "guava", "classesRelocation");

        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return ImmutableList.of(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        project.newChildProject("executor", child -> {
            child.getBuildFile().applyPlugin("java");

            child.getBuildFile().appendBlock("dependencies", dependencies -> {
                dependencies.append("implementation project(':')");
            });

            child.writeTextFile("src/main/java/pkg/child/LogicExecutor.java", join("\n", new String[]{
                "package pkg.child;",
                "",
                "import java.util.List;",
                "import pkg.Logic;",
                "",
                "public class LogicExecutor {",
                "",
                "    public static List<String> execute() {",
                "        return Logic.execute();",
                "    }",
                "",
                "}",
            }));

            addLibraryToDependencies(child.getBuildFile(), "junit-jupiter-api", "testImplementation");
            addLibraryToDependencies(child.getBuildFile(), "junit-jupiter-engine", "testRuntimeOnly");
            addLibraryToDependencies(child.getBuildFile(), "junit-platform-launcher", "testRuntimeOnly");

            child.getBuildFile().appendBlock("tasks.withType(Test).configureEach", task -> {
                task.append("useJUnitPlatform()");
                task.append("enableAssertions = true");
            });

            child.writeTextFile("src/test/java/pkg/child/LogicExecutorTest.java", join("\n", new String[]{
                "package pkg.child;",
                "",
                "import static org.junit.jupiter.api.Assertions.*;",
                "import static java.util.Arrays.*;",
                "",
                "import org.junit.jupiter.api.Test;",
                "",
                "public class LogicExecutorTest {",
                "",
                "    @Test",
                "    void test() {",
                "        assertThrows(ClassNotFoundException.class, () ->",
                "            // this class should NOT be available here",
                "            Class.forName(\"com.google.common.collect.ImmutableList\")",
                "        );",
                "",
                "        assertEquals(asList(\"a\", \"b\", \"c\"), LogicExecutor.execute());",
                "    }",
                "",
                "}",
            }));
        });

        project.getBuildFile().registerDefaultTask("test");

        project.assertBuildSuccessfully();
    }

    @Test
    void sourceSetOutputDependencyInAnotherProject() {
        addLibraryToDependencies(project.getBuildFile(), "guava", "classesRelocation");

        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return ImmutableList.of(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        project.newChildProject("executor", child -> {
            child.getBuildFile().applyPlugin("java");

            child.getBuildFile().append("evaluationDependsOn(':')");
            child.getBuildFile().appendBlock("dependencies", dependencies -> {
                dependencies.append("implementation files(project(':').sourceSets.main.output)");
            });

            child.writeTextFile("src/main/java/pkg/child/LogicExecutor.java", join("\n", new String[]{
                "package pkg.child;",
                "",
                "import java.util.List;",
                "import pkg.Logic;",
                "",
                "public class LogicExecutor {",
                "",
                "    public static List<String> execute() {",
                "        return Logic.execute();",
                "    }",
                "",
                "}",
            }));

            addLibraryToDependencies(child.getBuildFile(), "junit-jupiter-api", "testImplementation");
            addLibraryToDependencies(child.getBuildFile(), "junit-jupiter-engine", "testRuntimeOnly");
            addLibraryToDependencies(child.getBuildFile(), "junit-platform-launcher", "testRuntimeOnly");

            child.getBuildFile().appendBlock("tasks.withType(Test).configureEach", task -> {
                task.append("useJUnitPlatform()");
                task.append("enableAssertions = true");
            });

            child.writeTextFile("src/test/java/pkg/child/LogicExecutorTest.java", join("\n", new String[]{
                "package pkg.child;",
                "",
                "import static org.junit.jupiter.api.Assertions.*;",
                "import static java.util.Arrays.*;",
                "",
                "import org.junit.jupiter.api.Test;",
                "",
                "public class LogicExecutorTest {",
                "",
                "    @Test",
                "    void test() {",
                "        assertThrows(ClassNotFoundException.class, () ->",
                "            // this class should NOT be available here",
                "            Class.forName(\"com.google.common.collect.ImmutableList\")",
                "        );",
                "",
                "        assertEquals(asList(\"a\", \"b\", \"c\"), LogicExecutor.execute());",
                "    }",
                "",
                "}",
            }));
        });

        project.getBuildFile().registerDefaultTask("test");

        project.assertBuildSuccessfully();
    }

    @Test
    void testSourceSets() throws Throwable {
        addLibraryToDependencies(project.getBuildFile(), "guava", "classesRelocation");

        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return ImmutableList.of(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-api", "testImplementation");
        addLibraryToDependencies(project.getBuildFile(), "junit-jupiter-engine", "testRuntimeOnly");
        addLibraryToDependencies(project.getBuildFile(), "junit-platform-launcher", "testRuntimeOnly");

        project.getBuildFile().appendBlock("tasks.withType(Test).configureEach", task -> {
            task.append("useJUnitPlatform()");
            task.append("enableAssertions = true");
        });

        project.getBuildFile().forBuildscript(buildscript -> {
            addLibraryToDependencies(buildscript, "test-source-sets", "classpath");
        });
        project.getBuildFile().append("apply plugin: 'name.remal.test-source-sets'");

        project.getBuildFile().append("testSourceSets.register('otherTest').get()");

        Stream.of(
            "otherTestCompileClasspath",
            "otherTestRuntimeClasspath"
        ).forEach(confName -> {
            project.getBuildFile().append("configurations." + confName + ".exclude(group: 'org.junit.jupiter')");
            project.getBuildFile().append("configurations." + confName + ".exclude(group: 'org.junit.platform')");
        });

        project.writeTextFile("src/otherTest/java/pkg/LogicTest.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import static org.junit.jupiter.api.Assertions.*;",
            "import static java.util.Arrays.*;",
            "",
            "import org.junit.jupiter.api.Test;",
            "",
            "public class LogicTest {",
            "",
            "    @Test",
            "    void test() {",
            "        assertThrows(ClassNotFoundException.class, () ->",
            "            // this class should NOT be available here",
            "            Class.forName(\"com.google.common.collect.ImmutableList\")",
            "        );",
            "",
            "        assertEquals(asList(\"a\", \"b\", \"c\"), Logic.execute());",
            "    }",
            "",
            "}",
        }));

        project.getBuildFile().registerDefaultTask("otherTest");

        project.assertBuildSuccessfully();
    }

    @Test
    void publishing() throws Throwable {
        addLibraryToDependencies(project.getBuildFile(), "guava", "classesRelocation");

        project.writeTextFile("src/main/java/pkg/Logic.java", join("\n", new String[]{
            "package pkg;",
            "",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "",
            "public class Logic {",
            "",
            "    public static List<String> execute() {",
            "        return ImmutableList.of(\"a\", \"b\", \"c\");",
            "    }",
            "",
            "}",
        }));

        project.getBuildFile().applyPlugin("maven-publish");

        project.getBuildFile().appendBlock("publishing", publishing -> {
            publishing.appendBlock("repositories", repositories -> {
                repositories.appendBlock("maven", maven -> {
                    maven.append("name = 'testRepo'");
                    maven.append("url = '" + escapeGroovy(mavenRepoPath.toUri().toString()) + "'");
                });
            });
            publishing.appendBlock("publications.create('mavenJava', MavenPublication)", publication -> {
                publication.append("from components.java");
            });
        });

        project.getBuildFile().registerDefaultTask("publishAllPublicationsToTestRepoRepository");

        project.assertBuildSuccessfully();

        Path publishedJarPath = getPublishedJarPath();
        assertThat(publishedJarPath).isRegularFile();

        try (val classLoader = new URLClassLoader(new URL[]{publishedJarPath.toUri().toURL()}, null)) {
            assertThrows(ClassNotFoundException.class, () ->
                // this class should NOT be available in the ClassLoader
                classLoader.loadClass("com.google.common.collect.ImmutableList")
            );

            val logic = classLoader.loadClass("pkg.Logic");
            val testMethod = logic.getMethod("execute");
            @SuppressWarnings("unchecked") val testList = (List<String>) testMethod.invoke(null);
            assertThat(testList).containsExactly("a", "b", "c");
        }
    }


    private static void addLibraryToDependencies(
        BaseGroovyFileContent<?> script,
        String libraryName,
        String configurationName
    ) {
        script.appendBlock("dependencies", deps -> {
            deps.append(configurationName + " files(");
            getLibraryFilePaths(libraryName).forEach(path ->
                deps.append("    '" + escapeGroovy(path.toString()) + "',")
            );
            deps.append(")");
        });
    }

    private static Collection<Path> getLibraryFilePaths(String libraryName) {
        val classpathString = System.getProperty(libraryName + "-classpath");
        if (classpathString == null) {
            throw new IllegalStateException("Unknown library: " + libraryName);
        }

        return Splitter.on(pathSeparator).splitToStream(classpathString)
            .filter(not(String::isEmpty))
            .map(Paths::get)
            .collect(toImmutableSet());
    }

}

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
import name.remal.gradle_plugins.generate_sources.generators.java_like.JavaLikeContent;
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
            settings.line("rootProject.name = '" + escapeGroovy(artifactId) + "'");
        });

        project.forBuildFile(build -> {
            build.line("group = '" + build.escapeString(groupId) + "'");
            build.line("version = '" + build.escapeString(version) + "'");

            build.applyPlugin("name.remal.classes-relocation");
            build.applyPlugin("java");

            build.block("classesRelocation", classesRelocation -> {
                classesRelocation.line("basePackageForRelocatedClasses = 'relocated'");
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

        project.getBuildFile().block("tasks.withType(Test).configureEach", task -> {
            task.line("useJUnitPlatform()");
            task.line("enableAssertions = true");
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

        project.assertBuildSuccessfully("test");
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

        project.getBuildFile().block("tasks.withType(Test).configureEach", task -> {
            task.line("useJUnitPlatform()");
            task.line("enableAssertions = true");
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

        project.assertBuildSuccessfully("test");
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

            child.getBuildFile().block("dependencies", dependencies -> {
                dependencies.line("implementation project(':')");
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

            child.getBuildFile().block("tasks.withType(Test).configureEach", task -> {
                task.line("useJUnitPlatform()");
                task.line("enableAssertions = true");
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

        project.assertBuildSuccessfully("test");
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

            child.getBuildFile().line("evaluationDependsOn(':')");
            child.getBuildFile().block("dependencies", dependencies -> {
                dependencies.line("implementation files(project(':').sourceSets.main.output)");
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

            child.getBuildFile().block("tasks.withType(Test).configureEach", task -> {
                task.line("useJUnitPlatform()");
                task.line("enableAssertions = true");
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

        project.assertBuildSuccessfully("test");
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

        project.getBuildFile().block("tasks.withType(Test).configureEach", task -> {
            task.line("useJUnitPlatform()");
            task.line("enableAssertions = true");
        });

        project.getBuildFile().forBuildscript(buildscript -> {
            addLibraryToDependencies(buildscript, "test-source-sets", "classpath");
        });
        project.getBuildFile().line("apply plugin: 'name.remal.test-source-sets'");

        project.getBuildFile().line("testSourceSets.register('otherTest').get()");

        Stream.of(
            "otherTestCompileClasspath",
            "otherTestRuntimeClasspath"
        ).forEach(confName -> {
            project.getBuildFile().line("configurations." + confName + ".exclude(group: 'org.junit.jupiter')");
            project.getBuildFile().line("configurations." + confName + ".exclude(group: 'org.junit.platform')");
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

        project.assertBuildSuccessfully("otherTest");
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

        project.getBuildFile().block("publishing", publishing -> {
            publishing.block("repositories", repositories -> {
                repositories.block("maven", maven -> {
                    maven.line("name = 'testRepo'");
                    maven.line("url = '" + escapeGroovy(mavenRepoPath.toUri().toString()) + "'");
                });
            });
            publishing.block("publications.create('mavenJava', MavenPublication)", publication -> {
                publication.line("from components.java");
            });
        });

        project.assertBuildSuccessfully("publishAllPublicationsToTestRepoRepository");

        Path publishedJarPath = getPublishedJarPath();
        assertThat(publishedJarPath).isRegularFile();

        try (var classLoader = new URLClassLoader(new URL[]{publishedJarPath.toUri().toURL()}, null)) {
            assertThrows(ClassNotFoundException.class, () ->
                // this class should NOT be available in the ClassLoader
                classLoader.loadClass("com.google.common.collect.ImmutableList")
            );

            var logic = classLoader.loadClass("pkg.Logic");
            var testMethod = logic.getMethod("execute");
            @SuppressWarnings("unchecked") var testList = (List<String>) testMethod.invoke(null);
            assertThat(testList).containsExactly("a", "b", "c");
        }
    }


    private static void addLibraryToDependencies(
        JavaLikeContent<?> script,
        String libraryName,
        String configurationName
    ) {
        script.block("dependencies", deps -> {
            deps.line(configurationName + " files(");
            deps.indent(indent ->
                getLibraryFilePaths(libraryName).forEach(path ->
                    indent.line("    '" + indent.escapeString(path.toString()) + "',")
                )
            );
            deps.line(")");
        });
    }

    private static Collection<Path> getLibraryFilePaths(String libraryName) {
        var classpathString = System.getProperty(libraryName + "-classpath");
        if (classpathString == null) {
            throw new IllegalStateException("Unknown library: " + libraryName);
        }

        return Splitter.on(pathSeparator).splitToStream(classpathString)
            .filter(not(String::isEmpty))
            .map(Paths::get)
            .collect(toImmutableSet());
    }

}

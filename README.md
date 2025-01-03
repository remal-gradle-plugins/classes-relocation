**Tested on Java LTS versions from <!--property:java-runtime.min-version-->8<!--/property--> to <!--property:java-runtime.max-version-->21<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->6.8<!--/property--> to <!--property:gradle-api.max-version-->8.12<!--/property-->.**

# `name.remal.classes-relocation` plugin

[![configuration cache: supported from v2](https://img.shields.io/static/v1?label=configuration%20cache&message=supported%20from%20v2&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.classes-relocation-->
```groovy
plugins {
    id 'name.remal.classes-relocation' version '2.0.0-rc-2'
}
```
<!--/plugin-usage-->

&nbsp;

This Gradle plugin facilitates the creation of a fat JAR by bundling your Java application with specific dependencies.
It relocates these dependencies to a new namespace within the JAR to prevent exposure to and conflicts with downstream projects.

This plugin is ideal for scenarios where your code relies on specific dependencies,
but you need to ensure that these dependencies do not interfere with those in projects that consume your JAR.

By using this plugin, you can better manage dependency versions,
leading to more reliable and predictable behavior of your Java applications across different environments.

Configuration:

```groovy
classesRelocation {
  basePackageForRelocatedClasses = 'you.base.project.relocated' // specify the base projects for relocated dependencies
}

dependencies {
  classesRelocation('com.google.guava:guava:XX.X.X-jre') { // relocate Guava with transitive dependencies
    exclude group: 'com.google.errorprone', module: 'error_prone_annotations' // but do NOT relocate Error Prone annotations
  }
}
```

## How the plugin works

This plugin adds a new action to the [`jar`](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html) task.
This action recreates the JAR file relocating dependencies from the `classesRelocation` configuration.

Only directly used classes will be relocated.

The result JAR file will be used by
[`Test`](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html),
[`PluginUnderTestMetadata`](https://docs.gradle.org/current/javadoc/org/gradle/plugin/devel/tasks/PluginUnderTestMetadata.html),
[`ValidatePlugins`](https://docs.gradle.org/current/javadoc/org/gradle/plugin/devel/tasks/ValidatePlugins.html),
[`JacocoReportBase`](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/JacocoReportBase.html) tasks (instead of `build/classes/main/*` directories).

Other projects in Multi-Project builds will consume the result JAR file
if the current project is declared as a dependency (the same way it happens by default).

The result JAR file will be published to Maven repositories (the same way it happens by default).

By default, the [`jar`](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html) task is not cacheable.
This plugin makes this task cacheable.

# Alternatives

## [`com.gradleup.shadow`](https://plugins.gradle.org/plugin/com.gradleup.shadow)

The classic Gradle plugin for fat-JAR creation.

**Benefits of `name.remal.classes-relocation`**

* Simpler configuration.
* Other projects in Multi-Project builds will consume the relocated JAR file automatically.
* Publication of the relocated JAR is configured automatically.

**Benefits of `com.gradleup.shadow`**

* Used in many projects, so it can be considered more stable and reliable.
* Can be used not only for classes relocation but for creating fat-JARs.
* Extendable (be implementing `com.github.jengelman.gradle.plugins.shadow.transformers.Transformer`)
* More configuration options. For example, a capability of excluding specific files and packages from relocation.
* Minimization capabilities

# Migration guide

## Version 1.* to 2.*

The `name.remal.gradle_plugins.classes_relocation.ClassesRelocationExtension` project extension should be used
instead of `name.remal.gradle_plugins.plugins.classes_relocation.ClassesRelocationExtension`.

The `relocateClasses` configuration is still supported but was deprecated. Use `classesRelocation` instead.

The `@name.remal.gradle_plugins.api.RelocateClasses` and `@name.remal.gradle_plugins.api.RelocatePackages` annotations are no longer supported.
If you need to relocate a dependency of a specific class only (but not of other classes), create a separate module and include it in your build.

The `excludeFromClassesRelocation` and `excludeFromForcedClassesRelocation` configurations are no longer supported.

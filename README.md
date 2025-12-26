**Tested on Java LTS versions from <!--property:java-runtime.min-version-->11<!--/property--> to <!--property:java-runtime.max-version-->25<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->7.5<!--/property--> to <!--property:gradle-api.max-version-->9.3.0-rc-2<!--/property-->.**

# `name.remal.classes-relocation` plugin

[![configuration cache: supported from v2.1](https://img.shields.io/static/v1?label=configuration%20cache&message=supported%20from%20v2.1&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.classes-relocation-->
```groovy
plugins {
    id 'name.remal.classes-relocation' version '2.1.5'
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

## Base configuration

```groovy
classesRelocation {
  basePackageForRelocatedClasses = 'your.project.relocated' // specify the base package for relocated dependencies
}

dependencies {
  classesRelocation('com.google.guava:guava:XX.X.X-jre') { // relocate Guava with transitive dependencies
    exclude group: 'com.google.errorprone', module: 'error_prone_annotations' // but do NOT relocate Error Prone annotations
  }
}
```

## Minimization

This plugin can automatically remove all classes of relocated dependencies that are not used by the project,
minimizing the result JAR file.
The minimization is implemented via extensive bytecode analysis.

These class members are **always** relocated:

* Static initializer
  * It means that all initialized static fields will always be kept
  * See issue [#37](https://github.com/remal-gradle-plugins/classes-relocation/issues/37)
* Methods of used annotation classes 

Serialization-related members are relocated if any instance member is relocated:

* `serialVersionUID` static field
* `writeObject(ObjectOutputStream)` method
* `readObject(ObjectInputStream)` method
* `readObjectNoData()` method
* `writeReplace()` method
* `readResolve()` method

If you relocate a dependency that doesn't use reflection (`Class.getMethod()`, `Class.getField()`, etc),
you likely don't need to configure minimization.

### Keep class members annotated with configured annotations

All class members annotated by these annotations will be kept:

<!--iterable-code-property:keepAnnotationInclusionsDefault-->
* `jakarta.inject.**`
* `javax.inject.**`
* `com.fasterxml.jackson.**`
* `com.google.gson.**`
* `jakarta.validation.**`
* `javax.validation.**`
* `org.hibernate.validator.**`
<!--/iterable-code-property-->

You can configure other annotation type patterns:

```groovy
classesRelocation {
  minimize {
    keepMembersAnnotatedWith('jakarta.inject.**') // all class members annotated with annotations matched to the provided patterns will be kept
  }
}
```

### GraalVM's Reachability Metadata

To avoid the removal of necessary code, the plugin uses [GraalVM's Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/):

* `META-INF/native-image/<groupId>/<artifactId>/reachability-metadata.json` files in the relocated dependencies are processed
  * only `$.reflection` field is supported
  * `$.resources` and `$.bundles` fields are NOT supported, so resource relocation can be broken is some cases
* `reflect-config.json` files from [oracle/graalvm-reachability-metadata](https://github.com/oracle/graalvm-reachability-metadata) are processed

### Full minimization configuration

```groovy
classesRelocation {
  minimize {
    keepClasses('com.google.common.*') // all classes in the `com.google.common` package will be fully relocated; subpackages (like `com.google.common.base`) will NOT be minimized
    keepClasses('com.google.common.**') // all classes in the `com.google.common` package in its subpackages (like `com.google.common.base`) will be fully relocated

    keepMembersAnnotatedWith('jakarta.inject.**') // all class members annotated with annotations matched to the provided patterns will be kept

    graalvmReachabilityMetadataVersion = 'x.x.x' // set release of https://github.com/oracle/graalvm-reachability-metadata

    addClassReachabilityConfig { // add GraalVM's Reachability Metadata programmatically
      className('package.Class') // for `$.reflection[*].type`
      onReachedClass('other.package.reached.Class') // for `$.reflection[*].condition.typeReached`
      onReached() // equals to `onReachedClass()` with the same class name as `className()`
      field('fieldName') // for `$.reflection[*].fields[*].name`
      fields(['fieldName1', 'fieldName2']) // for `$.reflection[*].fields[*].name`
      method('methodName', '(Ljava/lang/String)') // for `$.reflection[*].methods[*].name` and `$.reflection[*].methods[*].parameterTypes`; `(Ljava/lang/String)` is a method descriptor (see https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3), return type is OPTIONAL
      allDeclaredConstructors(true) // for `$.reflection[*].allDeclaredConstructors`
      allPublicConstructors(true) // for `$.reflection[*].allPublicConstructors`
      allDeclaredMethods(true) // for `$.reflection[*].allDeclaredMethods`
      allPublicMethods(true) // for `$.reflection[*].allPublicMethods`
      allDeclaredFields(true) // for `$.reflection[*].allDeclaredFields`
      allPublicFields(true) // for `$.reflection[*].allPublicFields`
      allPermittedSubclasses(true) // for `$.reflection[*].allPermittedSubclasses`
    }

    addClassReachabilityConfig { /* ... */ } // add more GraalVM's Reachability Metadata
  }
}
```

Used minimization configuration is stored in the result JAR file.
So, if the result JAR file is used as a relocated dependency in another project,
all kept classes/members will be relocated.

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
* Minimization is relatively simple and enabled by default

**Benefits of `com.gradleup.shadow`**

* Used in many projects, so it can be considered more stable and reliable.
* Can be used not only for classes relocation but for creating fat-JARs.
* Extendable (be implementing `com.github.jengelman.gradle.plugins.shadow.transformers.Transformer`)
* More configuration options. For example, a capability of excluding specific files and packages from relocation.
* Minimization requires tests to be written (by default, classes not used in **test** will be removed)
  * Code coverage should be very high to work properly
  * Build will fail if there is a dependency like this: `:prod - main source set` (with relocation) > `:test-utils - main source set` (depends on `:prod`) > `:prod > test source set` (prod's tests depend on `:test-utils`)

# Migration guide

## Version 1.* to 2.*

The minimum Java version is 11.

The `name.remal.gradle_plugins.classes_relocation.ClassesRelocationExtension` project extension should be used
instead of `name.remal.gradle_plugins.plugins.classes_relocation.ClassesRelocationExtension`.

The `relocateClasses` configuration is still supported but was deprecated. Use `classesRelocation` instead.

The `@name.remal.gradle_plugins.api.RelocateClasses` and `@name.remal.gradle_plugins.api.RelocatePackages` annotations are no longer supported.
If you need to relocate a dependency of a specific class only (but not of other classes), create a separate module and include it in your build.

The `excludeFromClassesRelocation` and `excludeFromForcedClassesRelocation` configurations are no longer supported.

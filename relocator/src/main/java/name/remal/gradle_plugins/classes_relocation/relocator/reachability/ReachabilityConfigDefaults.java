package name.remal.gradle_plugins.classes_relocation.relocator.reachability;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig;

@NoArgsConstructor(access = PRIVATE)
public abstract class ReachabilityConfigDefaults {

    public static final List<ClassReachabilityConfig> DEFAULT_CLASS_REACHABILITY_CONFIGS = List.of(
        // org.xmlresolver:xmlresolver
        ClassReachabilityConfig.builder()
            .className("org.xmlresolver.loaders.XmlLoader")
            .onReachedClass("org.xmlresolver.ResolverFeature")
            .allPublicConstructors(true)
            .build(),

        // com.google.guava:guava
        ClassReachabilityConfig.builder()
            .className("com.google.common.cache.Striped64")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.cache.Striped64$Cell")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.collect.ConcurrentHashMultiset")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.collect.ImmutableMultimap")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.collect.ImmutableSetMultimap")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.collect.AbstractSortedMultiset")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("com.google.common.collect.TreeMultiset")
            .onReached()
            .allDeclaredFields(true)
            .build(),

        // io.github.classgraph:classgraph
        ClassReachabilityConfig.builder()
            .className("io.github.classgraph.Resource")
            .onReached()
            .allPublicMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.scanspec.AcceptReject")
            .onReached()
            .allDeclaredFields(true)
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.scanspec.ScanSpec")
            .onReached()
            .allDeclaredFields(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.SpringBootRestartClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.QuarkusClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.TomcatWebappClassLoaderBaseHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.OSGiDefaultClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.PlexusClassWorldsClassRealmClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.EquinoxClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.FallbackClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.FelixClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.WeblogicClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.JPMSClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.ParentLastDelegationOrderTestClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.CxfContainerClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.AntClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.EquinoxContextFinderClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.ClassGraphClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.WebsphereTraditionalClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.URLClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.JBossClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.WebsphereLibertyClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build(),
        ClassReachabilityConfig.builder()
            .className("nonapi.io.github.classgraph.classloaderhandler.UnoOneJarClassLoaderHandler")
            .onReached()
            .allDeclaredMethods(true)
            .build()
    );

}

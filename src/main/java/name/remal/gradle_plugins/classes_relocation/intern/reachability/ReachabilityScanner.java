package name.remal.gradle_plugins.classes_relocation.intern.reachability;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.intern.reachability.ReachableComparator.REACHABLE_COMPARATOR;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;

import com.google.common.collect.ImmutableList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.content.ClassNodeContent;
import name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@Builder
@RequiredArgsConstructor(access = PRIVATE)
public class ReachabilityScanner {

    private final Classpath sourceClasspath;
    private final Classpath relocationClasspath;
    private final Classpath runtimeClasspath;
    private final Classpath compileClasspath;

    private final Classpath allRelocationClasspath = asLazyProxy(Classpath.class, LazyValue.of(() -> {
        val relocationPackageNames = relocationClasspath.getPackageNames();
        val sourceInclusions = relocationPackageNames.stream()
            .map(it -> it.replace('.', '/'))
            .flatMap(it -> Stream.of(
                it + "/*.class",
                "META-INF/versions/*/" + it + "/*.class"
            ))
            .collect(toList());
        return sourceClasspath.withResources(
            /* inclusions = */ sourceInclusions,
            /* exclusions = */ ImmutableList.of()
        ).plus(relocationClasspath);
    }));

    private final Classpath dependenciesClasspath = asLazyProxy(Classpath.class, LazyValue.of(() ->
        runtimeClasspath.plus(compileClasspath)
    ));

    public Reachability scan() {
        val reachability = new Reachability();

        Queue<Reachable> reachableQueue = new PriorityQueue<>(REACHABLE_COMPARATOR);
        ReachableConsumer reachableConsumer = reachables -> {
            if (reachables != null) {
                for (val reachable : reachables) {
                    if (reachable != null) {
                        reachableQueue.add(reachable);
                    }
                }
            }
        };

        sourceClasspath.getClasses().keySet().stream()
            .map(AsmUtils::toClassInternalName)
            .map(reachability::registerReachableClass)
            .forEach(reachableQueue::add);

        sourceClasspath.getResources().stream()
            .map(Resource::getPath)
            .filter(path -> !path.endsWith(".class"))
            .map(reachability::registerReachableResource)
            .forEach(reachableQueue::add);

        while (true) {
            val reachable = reachableQueue.poll();
            if (reachable == null) {
                break;
            }

            if (reachable instanceof ReachableClass) {
                handle(reachability, reachableConsumer, (ReachableClass) reachable);
            }
        }

        return reachability;
    }

    private void handle(
        Reachability reachability,
        ReachableConsumer reachableConsumer,
        ReachableClass reachableClass
    ) {
        val className = toClassName(reachableClass.getClassInternalName());

        reachableConsumer.accept(
            reachability.registerReachableMethod(
                reachableClass.getClassInternalName(),
                "<clinit>",
                "()V"
            ),
            reachability.registerReachableResource(
                "META-INF/services/" + className
            )
        );

        if (allRelocationClasspath.hasClass(className)) {
            return;
        }

        sourceClasspath.getClassResources(className).forEach(resource ->
            resource.getContent().with(ClassNodeContent.class, classNode -> {

            })
        );
    }


    private void registerAllMembers(
        Reachability reachability,
        ReachableConsumer reachableConsumer,
        ClassNode classNode
    ) {
        if (classNode.fields != null) {
            for (val field : classNode.fields) {
                reachability.registerReachableField(classNode.name, field.name);
            }
        }

        if (classNode.methods != null) {
            for (val method : classNode.methods) {
                reachability.registerReachableMethod(classNode.name, method.name, method.desc);
                processMethod(reachability, reachableConsumer, method);
            }
        }
    }

    private void processMethod(
        Reachability reachability,
        ReachableConsumer reachableConsumer,
        MethodNode methodNode
    ) {

    }


    @FunctionalInterface
    private interface ReachableConsumer {
        void accept(@Nullable Reachable... reachables);
    }

}

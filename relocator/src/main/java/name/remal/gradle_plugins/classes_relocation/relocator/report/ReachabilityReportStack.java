package name.remal.gradle_plugins.classes_relocation.relocator.report;

import static name.remal.gradle_plugins.toolkit.LateInit.lateInit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.api.WithRelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport.ReachabilityNode;
import name.remal.gradle_plugins.toolkit.LateInit;
import org.jspecify.annotations.Nullable;

@Deprecated
@SuppressWarnings({"java:S1123", "java:S1133", "java:S6355"})
class ReachabilityReportStack
    implements ClassesRelocatorComponent, WithRelocationContext {

    private final LateInit<ReachabilityReport> reachabilityReport = lateInit("reachabilityReport");

    @Override
    public void setRelocationContext(RelocationContext relocationContext) {
        reachabilityReport.set(relocationContext.getRelocationComponent(ReachabilityReport.class));
    }


    private final Deque<ReachabilityNode> nodes = new ArrayDeque<>();

    @Nullable
    public ReachabilityNode getTop() {
        return nodes.peekLast();
    }

    public void forTop(Consumer<ReachabilityNode> action) {
        var lastNode = getTop();
        if (lastNode != null) {
            action.accept(lastNode);
        }
    }


    public ReachabilityNode pushClass(String classNameOrInternalName) {
        var node = reachabilityReport.get().clazz(classNameOrInternalName);
        nodes.addLast(node);
        return node;
    }

    public ReachabilityNode pushField(String classNameOrInternalName, String fieldName) {
        var node = reachabilityReport.get().field(classNameOrInternalName, fieldName);
        nodes.addLast(node);
        return node;
    }

    public ReachabilityNode pushMethod(String classNameOrInternalName, MethodKey methodKey) {
        var node = reachabilityReport.get().method(classNameOrInternalName, methodKey);
        nodes.addLast(node);
        return node;
    }

    public ReachabilityNode pushResource(String resourceName) {
        var node = reachabilityReport.get().resource(resourceName);
        nodes.addLast(node);
        return node;
    }


    @Nullable
    public ReachabilityNode pop() {
        return nodes.pollLast();
    }

}

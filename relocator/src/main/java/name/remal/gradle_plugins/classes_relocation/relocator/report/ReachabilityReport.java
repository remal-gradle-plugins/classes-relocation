package name.remal.gradle_plugins.classes_relocation.relocator.report;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.toClassName;

import com.google.errorprone.annotations.CheckReturnValue;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorComponent;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;

public class ReachabilityReport
    extends ReachabilityReportNode
    implements ClassesRelocatorComponent {

    @RequiredArgsConstructor(access = PRIVATE)
    public static class ReachabilityNode {

        private final ReachabilityReportNode reportNode;


        public void reachesClass(String classNameOrInternalName) {
            var className = toClassName(classNameOrInternalName);
            reportNode.children.putIfAbsent(className, null);
        }

        public void reachesField(String classNameOrInternalName, String fieldName) {
            var className = toClassName(classNameOrInternalName);
            reportNode.children.putIfAbsent(className + "#" + fieldName, null);
        }

        public void reachesMethod(String classNameOrInternalName, MethodKey methodKey) {
            var className = toClassName(classNameOrInternalName);
            reportNode.children.putIfAbsent(className + "#" + methodKey, null);
        }

        public void reachesResource(String resourceName) {
            reportNode.children.putIfAbsent(resourceName, null);
        }


        @CheckReturnValue
        public RelocationContext wrapRelocationContext(RelocationContext delegate) {
            return new ReachabilityReportRelocationContext(delegate, this);
        }

    }


    @CheckReturnValue
    public ReachabilityNode clazz(String classNameOrInternalName) {
        var className = toClassName(classNameOrInternalName);
        var reportNode = children.computeIfAbsent(className, __ -> new ReachabilityReportNode());
        return new ReachabilityNode(reportNode);
    }

    @CheckReturnValue
    public ReachabilityNode field(String classNameOrInternalName, String fieldName) {
        var classNode = clazz(classNameOrInternalName);
        var reportNode = classNode.reportNode.children.computeIfAbsent(fieldName, __ -> new ReachabilityReportNode());
        return new ReachabilityNode(reportNode);
    }

    @CheckReturnValue
    public ReachabilityNode method(String classNameOrInternalName, MethodKey methodKey) {
        var classNode = clazz(classNameOrInternalName);
        var reportNode = classNode.reportNode.children.computeIfAbsent(methodKey, __ -> new ReachabilityReportNode());
        return new ReachabilityNode(reportNode);
    }

    @CheckReturnValue
    public ReachabilityNode resource(String resourceName) {
        var reportNode = children.computeIfAbsent(resourceName, __ -> new ReachabilityReportNode());
        return new ReachabilityNode(reportNode);
    }

}

package name.remal.gradle_plugins.classes_relocation.relocator.report;

import name.remal.gradle_plugins.classes_relocation.relocator.api.DelegateRelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateClass;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateField;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz.RelocateMethod;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport.ReachabilityNode;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

public class ReachabilityReportRelocationContext extends DelegateRelocationContext {

    private final ReachabilityNode reachabilityNode;

    public ReachabilityReportRelocationContext(
        RelocationContext delegate,
        ReachabilityNode reachabilityNode
    ) {
        super(delegate);
        this.reachabilityNode = reachabilityNode;
    }

    @Override
    public void queue(QueuedTask task) {
        if (task instanceof RelocateClass) {
            reachabilityNode.reachesClass(
                ((RelocateClass) task).getClassInternalName()
            );

        } else if (task instanceof RelocateField) {
            reachabilityNode.reachesField(
                ((RelocateField) task).getClassInternalName(),
                ((RelocateField) task).getFieldName()
            );

        } else if (task instanceof RelocateMethod) {
            reachabilityNode.reachesMethod(
                ((RelocateMethod) task).getClassInternalName(),
                ((RelocateMethod) task).getMethodKey()
            );
        }

        super.queue(task);
    }

}

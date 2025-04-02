package name.remal.gradle_plugins.classes_relocation.relocator.report;

public class ReachabilityUnmodifiableReport
    extends ReachabilityReportNode {

    public ReachabilityUnmodifiableReport(ReachabilityReport delegate) {
        children.putAll(delegate.children);
    }

}

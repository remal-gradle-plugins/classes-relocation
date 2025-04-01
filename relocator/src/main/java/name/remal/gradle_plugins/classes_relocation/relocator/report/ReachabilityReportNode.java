package name.remal.gradle_plugins.classes_relocation.relocator.report;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
class ReachabilityReportNode {

    protected final Map<Object, @org.jetbrains.annotations.Nullable ReachabilityReportNode> children
        = new LinkedHashMap<>();


    private static final String INDENT = " ".repeat(2);

    public String render() {
        var sb = new StringBuilder();
        renderTo(children, sb, 0);
        return sb.toString();
    }

    public void renderTo(Appendable appendable) {
        renderTo(children, appendable, 0);
    }

    @SneakyThrows
    private void renderTo(Map<Object, ReachabilityReportNode> map, Appendable appendable, int depth) {
        var indent = INDENT.repeat(depth);

        for (var entry : map.entrySet()) {
            var name = entry.getKey();
            var child = entry.getValue();

            appendable.append(indent).append(String.valueOf(name));

            if (child != null) {
                appendable.append(" {\n");
                renderTo(child.children, appendable, depth + 1);
                appendable.append(indent).append('}');
            }

            appendable.append('\n');
        }
    }


    @Override
    public String toString() {
        return render();
    }

}

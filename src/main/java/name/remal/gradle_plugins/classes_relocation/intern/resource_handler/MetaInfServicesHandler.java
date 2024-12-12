package name.remal.gradle_plugins.classes_relocation.intern.resource_handler;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.FunctionUtils.toSubstringedBefore;
import static name.remal.gradle_plugins.toolkit.InputOutputStreamUtils.readStringFromStream;

import com.google.auto.service.AutoService;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.toolkit.ObjectUtils;

@AutoService({ResourcesMerger.class, ResourceProcessor.class})
public class MetaInfServicesHandler implements ResourcesMerger, ResourceProcessor {

    @Override
    public Collection<String> getInclusions() {
        return singletonList("META-INF/services/*");
    }

    @Override
    public Collection<String> getExclusions() {
        return singletonList("META-INF/services/org.codehaus.groovy.runtime.ExtensionModule");
    }


    private static final Charset CHARSET = UTF_8;
    private static final Pattern NEW_LINES = Pattern.compile("[\\n\\r]+");

    @SneakyThrows
    private static Set<String> parseServices(Collection<? extends Resource> resources) {
        val services = new LinkedHashSet<String>();
        for (val resource : resources) {
            final String content;
            try (val inputStream = resource.open()) {
                content = readStringFromStream(inputStream, CHARSET);
            }

            NEW_LINES.splitAsStream(content)
                .map(toSubstringedBefore("#"))
                .map(String::trim)
                .filter(ObjectUtils::isNotEmpty)
                .forEach(services::add);
        }
        return services;
    }

    @Override
    public byte[] merge(String resourceName, Collection<? extends Resource> resources) {
        val services = parseServices(resources);
        return join("\n", services).getBytes(CHARSET);
    }

    @Override
    public byte[] processResource(Resource resource, ResourceProcessingContext context) {
        val services = parseServices(singletonList(resource));

        val relocatedServices = services.stream()
            .map(service -> {
                context.handleInternalClassName(toClassInternalName(service));
                return context.getRelocatedClassNamePrefix() + service;
            })
            .collect(toList());

        return join("\n", relocatedServices).getBytes(CHARSET);
    }

}

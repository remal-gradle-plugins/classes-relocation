package name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.report.ReachabilityReport;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.ResourceProcessor;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.ResourcesSelector;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTaskHandler;

public class RelocateResourceHandler implements ImmediateTaskHandler<String, RelocateResource> {

    private final Set<RelocateResource> processingTasks = new LinkedHashSet<>();
    private final Map<RelocateResource, Optional<String>> cache = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("java:S2789")
    public Optional<String> handle(RelocateResource task, RelocationContext context) {
        if (!processingTasks.add(task)) {
            return Optional.empty();
        }

        try {
            Optional<String> result = cache.get(task);
            if (result == null) {
                result = handleImpl(task, context);
                cache.put(task, result);
            }
            return result;

        } finally {
            processingTasks.remove(task);
        }
    }

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    private Optional<String> handleImpl(RelocateResource task, RelocationContext context) {
        var resourceName = task.getResourceName();
        Map<Integer, List<Resource>> allCandidateResources = context.getSourceAndRelocationClasspath()
            .getResources(resourceName)
            .stream()
            .collect(groupingBy(resource -> {
                var multiReleaseVersion = resource.getMultiReleaseVersion();
                return multiReleaseVersion != null ? multiReleaseVersion : -1;
            }, LinkedHashMap::new, toList()));
        if (allCandidateResources.isEmpty()) {
            return Optional.empty();
        }

        context = context.getRelocationComponent(ReachabilityReport.class)
            .resource(resourceName)
            .wrapRelocationContext(context);

        var selectors = context.getRelocationComponents(ResourcesSelector.class);
        var processors = context.getRelocationComponents(ResourceProcessor.class);

        var originalResourceName = context.getOriginalResourceName(resourceName);

        String relocatedName = null;
        for (Entry<Integer, List<Resource>> candidateResourcesEntry : allCandidateResources.entrySet()) {
            Integer multiReleaseVersion = candidateResourcesEntry.getKey();
            if (multiReleaseVersion <= 0) {
                multiReleaseVersion = null;
            }

            var candidateResources = candidateResourcesEntry.getValue();

            Resource selectedResource = null;
            for (var selector : selectors) {
                var result = selector.select(
                    resourceName,
                    originalResourceName,
                    multiReleaseVersion,
                    candidateResources,
                    task.getCurrentClasspathElement(),
                    context
                );
                if (result.isPresent()) {
                    selectedResource = result.get();
                    break;
                }
            }
            if (selectedResource == null) {
                selectedResource = candidateResources.get(0);
            }

            Resource processedResource = null;
            for (var processor : processors) {
                var result = processor.processResource(
                    resourceName,
                    originalResourceName,
                    multiReleaseVersion,
                    selectedResource,
                    context
                );
                if (result.isPresent()) {
                    processedResource = result.get();
                    break;
                }
            }
            if (processedResource == null) {
                processedResource = GeneratedResource.builder()
                    .withSourceResource(selectedResource)
                    .withName(context.getRelocatedResourceNamePrefix() + resourceName)
                    .withMultiReleaseVersion(multiReleaseVersion)
                    .build();
            }

            if (relocatedName == null) {
                relocatedName = processedResource.getName();
            }

            context.writeToOutput(processedResource);
            context.registerOriginalResourceName(processedResource, originalResourceName);
        }

        return Optional.ofNullable(relocatedName);
    }

}

package name.remal.gradle_plugins.classes_relocation.relocator.relocators.xml;

import static javax.xml.stream.XMLInputFactory.IS_COALESCING;
import static javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE;
import static javax.xml.stream.XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.getNamePrefixOfResourceName;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.relocators.resource.RelocateResource;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.BaseResourcesHandler;

public class SimpleXmlResourcesHandler extends BaseResourcesHandler {

    private static final XMLInputFactory XML_INPUT_FACTORY;

    static {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(IS_NAMESPACE_AWARE, false);
        factory.setProperty(IS_VALIDATING, false);
        factory.setProperty(IS_COALESCING, false);
        factory.setProperty(IS_REPLACING_ENTITY_REFERENCES, false);
        factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(SUPPORT_DTD, false);
        XML_INPUT_FACTORY = factory;
    }


    public SimpleXmlResourcesHandler() {
        super(
            List.of(
                "**/*.xml"
            ),
            List.of(
            )
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "java:S3776", "RedundantSuppression"})
    protected Optional<Resource> processResourceImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) throws Throwable {
        var resourceNamePrefix = getNamePrefixOfResourceName(resourceName);

        try (var inputStream = resource.open()) {
            var events = XML_INPUT_FACTORY.createXMLEventReader(
                resource.toString(),
                inputStream
            );
            try {
                while (events.hasNext()) {
                    var untypedEvent = events.nextEvent();
                    if (untypedEvent.isStartElement()) {
                        StartElement event = untypedEvent.asStartElement();
                        Iterator<Attribute> attrs = event.getAttributes();
                        while (attrs.hasNext()) {
                            Attribute attr = attrs.next();
                            var string = attr.getValue();
                            if (context.isRelocationResourceName(resourceNamePrefix + string)) {
                                context.executeOptional(new RelocateResource(
                                    resourceNamePrefix + string,
                                    resource.getClasspathElement()
                                ));
                            }
                        }
                    }

                    if (untypedEvent.isCharacters()) {
                        var event = untypedEvent.asCharacters();
                        if (event.isWhiteSpace()) {
                            continue;
                        }

                        var string = event.getData();
                        if (context.isRelocationResourceName(resourceNamePrefix + string)) {
                            context.executeOptional(new RelocateResource(
                                resourceNamePrefix + string,
                                resource.getClasspathElement()
                            ));
                        }
                    }
                }

            } finally {
                events.close();
            }
        }

        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

}

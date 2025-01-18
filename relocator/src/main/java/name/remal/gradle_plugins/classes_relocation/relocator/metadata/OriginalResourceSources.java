package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

public class OriginalResourceSources extends AbstractClassesRelocationJsonDictionaryMetadata {

    @Override
    protected String getResourceName() {
        return "META-INF!name.remal.classes-relocation!original-resource-sources.json".replace('!', '/');
    }

}

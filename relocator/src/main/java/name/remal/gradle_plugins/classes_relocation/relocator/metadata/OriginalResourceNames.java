package name.remal.gradle_plugins.classes_relocation.relocator.metadata;

public class OriginalResourceNames extends AbstractClassesRelocationJsonDictionaryMetadata {

    @Override
    protected String getResourceName() {
        return "META-INF!name.remal.classes-relocation!original-resource-names.json".replace('!', '/');
    }

}

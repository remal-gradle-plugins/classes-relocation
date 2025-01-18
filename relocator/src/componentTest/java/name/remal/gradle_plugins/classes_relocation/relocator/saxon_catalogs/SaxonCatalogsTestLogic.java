package name.remal.gradle_plugins.classes_relocation.relocator.saxon_catalogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xmlresolver.ResolverFeature.CATALOG_MANAGER;

import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;
import org.xmlresolver.XMLResolverConfiguration;

public class SaxonCatalogsTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        var config = new XMLResolverConfiguration("");
        var catalogManager = config.getFeature(CATALOG_MANAGER);
        assertNotNull(catalogManager.lookupSystem("https://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"));
    }

}

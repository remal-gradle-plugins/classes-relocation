package name.remal.gradle_plugins.classes_relocation.relocator.utils;

import java.util.Collection;

public interface CollectionAddMethods<E> {

    boolean add(E element);

    boolean addAll(Collection<? extends E> collection);

}

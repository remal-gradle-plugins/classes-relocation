package name.remal.gradle_plugins.classes_relocation.relocator.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;

@EqualsAndHashCode
public class UniqueArrayQueue<E> implements Queue<E> {

    private final Set<E> seen = new LinkedHashSet<>();

    @Delegate(excludes = QueueAddMethods.class)
    private final Queue<E> queue = new ArrayDeque<>();

    @Override
    public boolean add(E element) {
        if (!seen.add(element)) {
            return false;
        }

        return queue.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean result = false;
        for (var element : collection) {
            result |= add(element);
        }
        return result;
    }

    @Override
    public boolean offer(E element) {
        if (!seen.add(element)) {
            return false;
        }

        return queue.offer(element);
    }

    @Override
    public String toString() {
        return queue.toString();
    }

}

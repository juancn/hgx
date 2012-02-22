package codng.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A default Sequence implementation.
 * @param <T> element type
 */
public abstract class DefaultSequence<T> implements Sequence<T> {
	@Override
	public <Y> Sequence<Y> map(final Function<T, Y> mapper) {
		return Sequences.map(this, mapper);
	}

	@Override
	public <Y> Sequence<Y> flatMap(final Function<T, Iterator<Y>> mapper) {
		return Sequences.flatMap(this, mapper);
	}

	@Override
	public Sequence<T> filter(final Predicate<T> filter) {
		return Sequences.filter(this, filter);
	}

	@Override
	public List<T> toList() {
		return Sequences.toList(this);
	}

	@Override
	public Set<T> toSet() {
		return Sequences.asSet(this);
	}

	@Override
	public boolean isEmpty() {
		return Sequences.isEmpty(this);
	}
}

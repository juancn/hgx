package codng.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A default Sequence implementation.
 * @param <T> element type
 */
public abstract class DefaultSequence<T> implements Sequence<T> {
	protected static final int TO_STRING_LIMIT = 1000;

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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		int count = 0;
		for (T t : this) {
			if(count != 0) sb.append(", ");
			if(count > TO_STRING_LIMIT) { sb.append("..."); break; }
			sb.append(t);
			++count;
		}
		sb.append(']');
		return sb.toString();
	}
}

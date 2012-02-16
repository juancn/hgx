package codng.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility methods to implement Sequences.
 * {@link DefaultSequence} provides a default implementation of a {@link Sequence}.
 * Those methods are implemented by static utility methods defined in here.
 * This allows some flexibility to implementors that might not be able to subclass {@link DefaultSequence}
 */
public class Sequences {

	private Sequences() {}

	/**
	 * Creates a new sequence containing the specified elements.
	 * @param elements the array by which the sequence will be backed by
	 * @param <X> element type
	 * @return a sequence with the specified elements
	 */
	@SafeVarargs
	public static <X> Sequence<X> asSequence(final X... elements) {
		return asSequence(Arrays.asList(elements));
	}

	/**
	 * Wraps an iterable as a Sequence
	 * @param iterable an iterable
	 * @param <X> element type of the iterable
	 * @return a sequence backed by the specified iterable
	 */
	public static <X> Sequence<X> asSequence(final Iterable<X> iterable) {
		return new DefaultSequence<X>() {
			@Override
			public Iterator<X> iterator() {
				return iterable.iterator();
			}
		};
	}

	/**
	 * Returns a filtered sequence based on the specified iterable.
	 * This function applies the specified predicate to each element, returning it if the predicate returns true.
	 * @param iterable an iterable
	 * @param filter predicate that accepts or rejects elements
	 * @param <T> element type of the iterable
	 * @return the filtered sequence.
	 */
	public static <T> Sequence<T> filter(final Iterable<T> iterable, final Predicate<T> filter) {
		return new DefaultSequence<T>() {
			@Override
			public Iterator<T> iterator() {
				return new NoRemoveIterator<T>() {
					private Iterator<T> it = iterable.iterator();
					@Override
					protected T advance() {
						while (it.hasNext()) {
							final T next = it.next();
							if(filter.apply(next)) return next;
						}
						return finished();
					}
				};
			}
		};
	}

	/**
	 * Maps the specified iterable to a Sequence using the specified function.
	 * @param iterable an iterable
	 * @param mapper mapping function.
	 * @param <T> element type of the iterable
	 * @param <Y> element type of the resulting sequence
	 * @return the mapped sequence
	 */
	public static <T, Y> Sequence<Y> map(final Iterable<T> iterable, final Function<T, Y> mapper) {
		return new DefaultSequence<Y>() {
			@Override
			public Iterator<Y> iterator() {
				return new NoRemoveIterator<Y>() {
					private Iterator<T> it = iterable.iterator();
					@Override
					protected Y advance() {
						return it.hasNext() ? mapper.apply(it.next()) : finished();
					}
				};
			}
		};
	}

	/**
	 * Maps this sequence using the specified function and returns the concatenation of results.
	 * @param iterable an iterable
	 * @param mapper mapping function.
	 * @param <T> element type of the iterable
	 * @param <Y> element type of the resulting sequence
	 * @return the mapped sequence
	 */
	public static <T, Y> Sequence<Y> flatMap(final Iterable<T> iterable, final Function<T, Iterator<Y>> mapper) {
		return new DefaultSequence<Y>() {
			@Override
			public Iterator<Y> iterator() {
				return new NoRemoveIterator<Y>() {
					private Iterator<T> outer = iterable.iterator();
					private Iterator<Y> inner;
					@Override
					protected Y advance() {
						if( (inner == null || !inner.hasNext()) && outer.hasNext()) {
							inner = mapper.apply(outer.next());
						}
						return (inner != null && inner.hasNext()) ? inner.next() : finished();
					}
				};
			}
		};
	}

	/**
	 * Copies the iterable to a list
	 * @param ts an iterable
	 * @return a copy of the iterable as a list.
	 */
	public static <T> List<T> toList(final Iterable<T> ts) {
		final ArrayList<T> list = new ArrayList<>();
		for (T t : ts) list.add(t);
		return list;
	}

	/**
	 * Copies the iterable to a set
	 * @param ts an iterable
	 * @return a copy of the iterable as a set.
	 */
	public static <T> Set<T> asSet(final Iterable<T> ts) {
		final HashSet<T> list = new HashSet<>();
		for (T t : ts) list.add(t);
		return list;
	}
}
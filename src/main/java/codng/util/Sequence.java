package codng.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An ordered sequence of elements.
 * The default implementation generates a view of the source sequence
 * (i.e. a Stream in functional programming terminology).
 * Implementations are not required to do so, but it is strongly encouraged.
 * @param <T> element type.
 */
public interface Sequence<T>
		extends Iterable<T>
{
	/**
	 * Maps this sequence using the specified function.
	 * @param mapper mapping function.
	 * @param <Y> element type of the resulting sequence
	 * @return the mapped sequence
	 */
	<Y> Sequence<Y> map(Function<T,Y> mapper);

	/**
	 * Maps this sequence using the specified function and returns the concatenation of results.
	 * @param mapper mapping function.
	 * @param <Y> element type of the resulting sequence
	 * @return the mapped sequence
	 */
	<Y> Sequence<Y> flatMap(Function<T,Iterator<Y>> mapper);

	/**
	 * Returns a filtered sequence.
	 * This function applies the specified predicate to each element, returning it if the predicate returns true.
	 * @param filter predicate that accepts or rejects elements
	 * @return the filtered sequence.
	 */
	Sequence<T> filter(Predicate<T> filter);

	/**
	 * Returns a copy of this sequence as a list.
	 * @return a copy of this sequence as a set.
	 */
	List<T> asList();

	/**
	 * Returns a copy of this sequence as a set.
	 * @return a copy of this sequence as a set.
	 */
	Set<T> asSet();

	/**
	 * A default Sequence implementation.
	 * @param <T> element type
	 */
	public static abstract class Default<T> implements Sequence<T> {
		@Override
		public <Y> Sequence<Y> map(final Function<T, Y> mapper) {
			final Default<T> self = this;
			return new Default<Y>() {
				@Override
				public Iterator<Y> iterator() {
					return new NoRemoveIterator<Y>() {
						private Iterator<T> it = self.iterator();
						@Override
						protected Y advance() {
							return it.hasNext() ? mapper.apply(it.next()) : finished();
						}
					};
				}
			};
		}

		@Override
		public <Y> Sequence<Y> flatMap(final Function<T, Iterator<Y>> mapper) {
			final Default<T> self = this;
			return new Default<Y>() {
				@Override
				public Iterator<Y> iterator() {
					return new NoRemoveIterator<Y>() {
						private Iterator<T> outer = self.iterator();
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

		@Override
		public Sequence<T> filter(final Predicate<T> filter) {
			final Default<T> self = this;
			return new Default<T>() {
				@Override
				public Iterator<T> iterator() {
					return new NoRemoveIterator<T>() {
						private Iterator<T> it = self.iterator();
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

		@SafeVarargs
		public static <X> Sequence<X> asSequence(final X... elements) {
			return asSequence(Arrays.asList(elements));
		}

		public static <X> Sequence<X> asSequence(final Iterable<X> iterable) {
			return new Default<X>() {
				@Override
				public Iterator<X> iterator() {
					return iterable.iterator();
				}
			};
		}

		@Override
		public List<T> asList() {
			final ArrayList<T> list = new ArrayList<>();
			for (T t : this) list.add(t);
			return list;
		}

		@Override
		public Set<T> asSet() {
			final HashSet<T> list = new HashSet<>();
			for (T t : this) list.add(t);
			return list;
		}
	}
}

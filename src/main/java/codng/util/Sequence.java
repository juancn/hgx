package codng.util;

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
	 * @return a copy of this sequence as a list.
	 */
	List<T> toList();

	/**
	 * Returns a copy of this sequence as a set.
	 * @return a copy of this sequence as a set.
	 */
	Set<T> toSet();

}

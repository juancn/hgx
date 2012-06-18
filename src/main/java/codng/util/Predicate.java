package codng.util;

/**
 * A predicate (i.e. a function that returns boolean values)
 * @param <X> predicate domain
 */
public interface Predicate<X> extends Function<X, Boolean> {

	/**
	 * Returns true if this predicate accepts x
	 * @param x an object on which the predicate is asserted
	 * @return true if this predicate accepts x
	 */
	boolean accepts(X x);

	/**
	 * Negates this predicate
	 * @return this predicate negated
	 */
	Predicate<X> not();

	/**
	 * Returns 'this && other'
	 * @param other a predicate
	 * @return 'this && other'
	 */
	Predicate<X> and(Predicate<X> other);

	/**
	 * Returns 'this || other'
	 * @param other a predicate
	 * @return 'this || other'
	 */
	Predicate<X> or(Predicate<X> other);
}

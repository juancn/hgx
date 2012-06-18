package codng.util;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Set of utility functions to use and implement predicates.
 */
public class Predicates {
	private static final Object SENTINEL = new Object();

	private static final Predicate<Object> TRUE_CONST = new DefaultPredicate<Object>() {
		@Override
		public boolean accepts(Object x) {
			return true;
		}
	};
	private static final Predicate<Object> FALSE_CONST = TRUE_CONST.not();

	/**
	 * Negates the specified predicate
	 * @param predicate a predicate
	 * @param <X> predicate's domain
	 * @return the negated predicate
	 */
	public static <X> Predicate<X> not(final Predicate<X> predicate) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return !predicate.accepts(x);
			}
		};
	}

	/**
	 * Returns a predicate that accepts any value
	 * @param <X> predicate's domain
	 * @return a predicate that accepts any value
	 */
	public static <X> Predicate<X> alwaysTrue() {
		return Cast.force(TRUE_CONST);
	}

	/**
	 * Returns a predicate that rejects any value
	 * @param <X> predicate's domain
	 * @return a predicate that rejects any value
	 */
	public static <X> Predicate<X> alwaysFalse() {
		return Cast.force(FALSE_CONST);
	}

	/**
	 * Returns a predicate that accepts any non-null value
	 * @param <X> predicate's domain
	 * @return a predicate that accepts any non-null value
	 */
	public static <X> Predicate<X> notNull() {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return x != null;
			}
		};
	}

	/**
	 * Return 'a && b'
	 * @param a a predicate
	 * @param b a predicate
	 * @param <X> predicate's domain
	 * @return 'a && b'
	 */
	public static <X> Predicate<X> and(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return a.accepts(x) && b.accepts(x);
			}
		};
	}

	/**
	 * Return 'a || b'
	 * @param a a predicate
	 * @param b a predicate
	 * @param <X> predicate's domain
	 * @return 'a || b'
	 */
	public static <X> Predicate<X> or(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return a.accepts(x) || b.accepts(x);
			}
		};
	}

	/**
	 * Returns true if all of the elements of the iterable are accepted by the predicate
	 * @param iterable an iterable
	 * @param predicate a predicate
	 * @param <X> predicate's domain
	 * @return true if all of the elements of the iterable are accepted by the predicate
	 */
	public static <X> boolean forAll(final Iterable<X> iterable, final Predicate<X> predicate) {
		for (X x : iterable) {
			if(!predicate.accepts(x)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if any of the elements of the iterable are accepted by the predicate
	 * @param iterable an iterable
	 * @param predicate a predicate
	 * @param <X> predicate's domain
	 * @return true if any of the elements of the iterable are accepted by the predicate
	 */
	public static <X> boolean forAny(final Iterable<X> iterable, final Predicate<X> predicate) {
		for (X x : iterable) {
			if(predicate.accepts(x)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a predicate that accepts values only once.
	 * Proper implementation of equals() and hashCode() in values are required by this predicate.
	 * @param <X> predicate's domain
	 * @return  a predicate that accepts values only once
	 */
	public static <X> Predicate<X> onlyOnce() {
		final Map<X, Object> seen = new WeakHashMap<>();
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				if (seen.containsKey(x)) return false;
				else {
					seen.put(x, SENTINEL);
					return true;
				}
			}
		};
	}
}

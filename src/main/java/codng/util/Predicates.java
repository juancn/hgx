package codng.util;

import java.util.HashSet;
import java.util.Set;

public class Predicates {
	private static final Predicate<Object> TRUE_CONST = new DefaultPredicate<Object>() {
		@Override
		public boolean accepts(Object x) {
			return true;
		}
	};
	private static final Predicate<Object> FALSE_CONST = TRUE_CONST.not();

	public static <X> Predicate<X> not(final Predicate<X> predicate) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return !predicate.accepts(x);
			}
		};
	}

	public static <X> Predicate<X> alwaysTrue() {
		return Cast.force(TRUE_CONST);
	}

	public static <X> Predicate<X> alwaysFalse() {
		return Cast.force(FALSE_CONST);
	}

	public static <X> Predicate<X> notNull() {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return x != null;
			}
		};
	}

	public static <X> Predicate<X> and(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return a.accepts(x) && b.accepts(x);
			}
		};
	}

	public static <X> Predicate<X> or(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				return a.accepts(x) || b.accepts(x);
			}
		};
	}

	public static <X> boolean forAll(final Iterable<X> iterable, final Predicate<X> pred) {
		for (X x : iterable) {
			if(!pred.accepts(x)) {
				return false;
			}
		}
		return true;
	}

	public static <X> boolean forAny(final Iterable<X> iterable, final Predicate<X> pred) {
		for (X x : iterable) {
			if(pred.accepts(x)) {
				return true;
			}
		}
		return false;
	}

	public static <X> Predicate<X> onlyOnce() {
		final Set<X> seen = new HashSet<>();
		return new DefaultPredicate<X>() {
			@Override
			public boolean accepts(X x) {
				if (seen.contains(x)) return false;
				else {
					seen.add(x);
					return true;
				}
			}
		};
	}
}

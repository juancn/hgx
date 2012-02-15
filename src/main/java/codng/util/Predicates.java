package codng.util;

public class Predicates {
	private static final Predicate<Object> TRUE_CONST = new DefaultPredicate<Object>() {
		@Override
		public boolean apply(Object x) {
			return true;
		}
	};
	private static final Predicate<Object> FALSE_CONST = TRUE_CONST.not();

	public static <X> Predicate<X> not(final Predicate<X> predicate) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean apply(X x) {
				return !predicate.apply(x);
			}
		};
	}

	public static <X> Predicate<X> TRUE() {
		return Cast.force(TRUE_CONST);
	}

	public static <X> Predicate<X> FALSE() {
		return Cast.force(FALSE_CONST);
	}

	public static <X> Predicate<X> and(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean apply(X x) {
				return a.apply(x) && b.apply(x);
			}
		};
	}

	public static <X> Predicate<X> or(final Predicate<X> a, final Predicate<X> b) {
		return new DefaultPredicate<X>() {
			@Override
			public boolean apply(X x) {
				return a.apply(x) || b.apply(x);
			}
		};
	}
}

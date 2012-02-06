package codng.util;

public interface Predicate<X> {
	boolean apply(X x);
	Predicate<X> not();
	Predicate<X> and(Predicate<X> other);
	Predicate<X> or(Predicate<X> other);

	public static abstract class Default<X> 
			implements Predicate<X> {
		@Override
		public final Predicate<X> not() {
			final Default<X> self = this;
			return new Default<X>() {
				@Override
				public boolean apply(X x) {
					return !self.apply(x);
				}
			};
		}

		@Override
		public final Predicate<X> and(final Predicate<X> other) {
			final Default<X> self = this;
			return new Default<X>() {
				@Override
				public boolean apply(X x) {
					return self.apply(x) && other.apply(x);
				}
			};
		}

		@Override
		public final Predicate<X> or(final Predicate<X> other) {
			final Default<X> self = this;
			return new Default<X>() {
				@Override
				public boolean apply(X x) {
					return self.apply(x) || other.apply(x);
				}
			};
		}

		public static <X> Predicate<X> TRUE() {
			return Cast.force(TRUE_CONST);
		}

		public static <X> Predicate<X> FALSE() {
			return Cast.force(FALSE_CONST);
		}

		private static final Predicate<Object> TRUE_CONST = new Default<Object>() {
			@Override
			public boolean apply(Object x) {
				return true;
			}
		};

		private static final Predicate<Object> FALSE_CONST = TRUE_CONST.not();
	}
}

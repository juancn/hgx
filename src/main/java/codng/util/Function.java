package codng.util;

/**
 * A function from X to Y.
 * @param <X> the function's domain (i.e. argument type)
 * @param <Y> the function's image (i.e. result type)
 */
public interface Function<X,Y> {
	Y apply(X x);
	<Z> Function<X, Z> compose(Function<Y, Z> other);
	
	public static abstract class Default<X,Y> 
			implements Function<X, Y> {
		@Override
		public <Z> Function<X, Z> compose(final Function<Y, Z> other) {
			final Default<X, Y> self = this;
			return new Default<X,Z>() {
				@Override
				public Z apply(X x) {
					return other.apply(self.apply(x));
				}
			};
		}
		
		public static <T> Function<T,T> identity() {
			return Cast.force(IDENTITY_CONST);
		}
		
		private static final Function<Object, Object> IDENTITY_CONST = new Default<Object, Object>() {
			@Override
			public Object apply(Object o) {
				return o;
			}
		};
	}
}

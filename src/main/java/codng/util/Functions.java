package codng.util;

/**
 * Utility methods to implement {@link Function}
 */
public class Functions {
	private static final Function<Object, Object> IDENTITY_CONST = new DefaultFunction<Object, Object>() {
		@Override
		public Object apply(Object o) {
			return o;
		}
	};

	/**
	 * Given two functions: f(x) and g(x) computes g(f(x))
	 * @param f X->Y function
	 * @param g Y->Z function
	 * @param <X> domain of f
	 * @param <Y> image of f and domain of g
	 * @param <Z> image of g
	 * @return the composition: g(f(x))
	 */
	public static <X, Y, Z> Function<X, Z> compose(final Function<X, Y> f, final Function<Y, Z> g) {
		return new DefaultFunction<X,Z>() {
			@Override
			public Z apply(X x) {
				return g.apply(f.apply(x));
			}
		};
	}

	/**
	 * Returns the identity function
	 * @param <T> image and domain of the function
	 * @return the identity function
	 */
	public static <T> Function<T,T> identity() {
		return Cast.force(IDENTITY_CONST);
	}
}

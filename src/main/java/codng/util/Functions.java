package codng.util;

import codng.hgx.ChangeSet;
import codng.hgx.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * Creates a map with the elements of the specified iterable grouped by the results of mapper.
	 * @param elements elements to be processed
	 * @param mapper key extraction function
	 * @param <X> type of the elements
	 * @param <Y> type of the key
	 * @return a new map with the elements indexed by mapper(elements)
	 */
	public static <X,Y> Map<Y, X> toMap(Iterable<X> elements, Function<X, Y> mapper) {
		final HashMap<Y, X> map = new HashMap<>();
		for (X element : elements) {
			map.put(mapper.apply(element), element);
		}
		return map;
	}
}

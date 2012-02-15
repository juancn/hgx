package codng.util;

/**
 * A function from X to Y.
 * @param <X> the function's domain (i.e. argument type)
 * @param <Y> the function's image (i.e. result type)
 */
public interface Function<X,Y> {
	/**
	 * Applies this function to the parameter x
	 * @param x function parameter
	 * @return the result of applying this function to x
	 */
	Y apply(X x);

	/**
	 * Given two functions: f(x) and g(x) computes f(g(x)) (assuming this function is f)
	 * @param g Y->Z function
	 * @param <Z> image of g
	 * @return the composition: f(g(x))
	 */
	<Z> Function<X, Z> compose(Function<Y, Z> g);
}

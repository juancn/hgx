package codng.util;

/**
 * Default implementation of a {@link Function}
 * All methods delegate on {@link Functions}
 * @param <X> domain
 * @param <Y> image
 */
public abstract class DefaultFunction<X,Y>
		implements Function<X, Y> {
	@Override
	public <Z> Function<X, Z> compose(final Function<Y, Z> g) {
		return Functions.compose(this, g);
	}
}

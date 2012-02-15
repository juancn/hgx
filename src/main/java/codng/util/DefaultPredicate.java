package codng.util;

/**
 * Default implementation of a {@link Predicate}
 * All methods delegate on {@link Predicates}
 * @param <X> domain of the predicate
 */
public abstract class DefaultPredicate<X>
		implements Predicate<X> {
	@Override
	public final Predicate<X> not() {
		return Predicates.not(this);
	}

	@Override
	public final Predicate<X> and(final Predicate<X> other) {
		return Predicates.and(this, other);
	}

	@Override
	public final Predicate<X> or(final Predicate<X> other) {
		return Predicates.or(this, other);
	}
}

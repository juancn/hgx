package codng.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator that does not support remove.
 * Subclasses can override the 'advance()' method, calling 'finished()' to signal that no more elements are available.
 * @param <T> element type
 */
public abstract class NoRemoveIterator<T>
	implements Iterator<T>
{
	protected T element;
	protected boolean initialized;
	private boolean finished;

	/**
	 * Advance to the next element or return {@link #finished()}
	 * @return T the next element or {@link #finished()}
	 */
	protected abstract T advance();

	/**
	 * Marks this iterator as finished. Returns T for convenience.
	 * @return T this method always returns null.
	 */
	protected final T finished() {
		finished = true;
		return null;
	}

	/**
	 * Return <code>value</code> or {@link #finished()} if value is null
	 * @param value value to be checked
	 * @return <code>value</code> or {@link #finished()} if value is null
	 */
	protected final T finishIfNull(T value) {
		return value == null ? finished() : value;
	}
	
	/** Defer initialization until subclasses have been properly initialized */
	private void initialize() {
		if(!initialized) {
			initialized = true;
			element = advance();
		}
	}

	@Override
	public boolean hasNext() {
		initialize();
		return !finished;
	}

	@Override
	public T next() {
		if(!hasNext()) throw new NoSuchElementException();
		final T result = element;
		element = advance();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

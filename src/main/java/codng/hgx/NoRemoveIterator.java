package codng.hgx;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class NoRemoveIterator<T>
	implements Iterator<T> {
	protected T element;
	protected boolean initialized;

	protected abstract T advance();
	
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
		return element != null;
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

package codng.util;

import java.util.Iterator;

/**
 * A sequence of integers in the specified range
 */
public class Range
		extends DefaultSequence<Integer>
{
	private final int from;
	private final int to;

	private Range(final int from, final int to) {
		this.from = from;
		this.to = to;
	}

	public static Range closed(final int from, final int to) {
		return new Range(from, to);
	}

	@Override
	public Iterator<Integer> iterator() {
		if(from < to) {
			return new NoRemoveIterator<Integer>() {
				int current = from;
				@Override
				protected Integer advance() {
					//noinspection RedundantCast
					return current <= to ? (Integer) current++ : finished();
				}
			};
		} else {
			return new NoRemoveIterator<Integer>() {
				int current = from;
				@Override
				protected Integer advance() {
					//noinspection RedundantCast
					return current >= to ? (Integer) current-- : finished();
				}
			};
		}
	}
}

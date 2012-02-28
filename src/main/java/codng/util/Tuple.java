package codng.util;

public class Tuple<A, B> {
	public final A first;
	public final B second;

	protected Tuple(final A first, final B second) {
		this.first = first;
		this.second = second;
	}
	
	public <A,B> Tuple<A,B> make(A a, B b) {
		return new Tuple<>(a,b);
	}
}

package codng.util;

public class Tuple<A, B> {
	public final A first;
	public final B second;

	protected Tuple(final A first, final B second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Tuple tuple = (Tuple) o;

		if (first != null ? !first.equals(tuple.first) : tuple.first != null) return false;
		if (second != null ? !second.equals(tuple.second) : tuple.second != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = first != null ? first.hashCode() : 0;
		result = 31 * result + (second != null ? second.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return String.format("(%s, %s)", first, second);
	}

	public static <A,B> Tuple<A,B> make(final A a, final B b) {
		return new Tuple<>(a,b);
	}

	public static <A,B,C> Tuple3<A,B,C> make(final A a, final B b, final C c) {
		return new Tuple3<>(a,b,c);
	}

	public static <A,B,C,D> Tuple4<A,B,C,D> make(final A a, final B b, final C c, final D d) {
		return new Tuple4<>(a,b,c,d);
	}
}

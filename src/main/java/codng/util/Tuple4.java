package codng.util;

public class Tuple4<A, B, C, D> extends Tuple3<A, B, C>{
	public final D fourth;

	Tuple4(final A first, final B second, final C third, final D fourth) {
		super(first, second, third);
		this.fourth = fourth;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		Tuple4 tuple4 = (Tuple4) o;

		if (fourth != null ? !fourth.equals(tuple4.fourth) : tuple4.fourth != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (fourth != null ? fourth.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return String.format("(%s, %s, %s, %s)", first, second, third, fourth);
	}
}

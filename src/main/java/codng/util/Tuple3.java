package codng.util;

public class Tuple3<A, B, C> extends Tuple<A, B>{
	public final C third;

	Tuple3(A first, B second, C third) {
		super(first, second);
		this.third = third;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		Tuple3 tuple3 = (Tuple3) o;

		if (third != null ? !third.equals(tuple3.third) : tuple3.third != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (third != null ? third.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return String.format("(%s, %s, %s)", first, second, third);
	}
}

package codng.util;

public class Cast {
	private Cast() {}
	
	@SuppressWarnings("unchecked")
	public static <T> T force(Object obj) {
		return (T)obj;
	}
}

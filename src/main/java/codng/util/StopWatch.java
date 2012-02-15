package codng.util;

public class StopWatch {
	private long start;

	public long reset() {
		return start = System.currentTimeMillis();
	}

	public long elapsed() {
		return System.currentTimeMillis() - start;
	}
}

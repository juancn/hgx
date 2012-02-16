package codng.util;

public class StopWatch {
	private long start;

	public StopWatch() {
		reset();
	}

	public long reset() {
		return start = System.currentTimeMillis();
	}

	public long elapsed() {
		return System.currentTimeMillis() - start;
	}
}

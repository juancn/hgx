package codng.hgx;

import java.io.IOException;

public class Hg {
	public static String branch() throws IOException {
		return Command.executeSimple("hg", "branch").trim();
	}

	static String id() throws IOException {
		return Command.executeSimple("hg", "id", "-r", "0").trim();
	}
}

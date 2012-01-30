package codng.hgx;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;

public class Hg {
	public static String branch() throws IOException {
		return Command.executeSimple("hg", "branch").trim();
	}

	static String id() throws IOException {
		return Command.executeSimple("hg", "id", "-r", "0").trim();
	}

	static AsyncCommand log(int since) throws IOException, InterruptedException {
		return new AsyncCommand("hg", "log", "-r", since +":").invoke();
	}

	static AsyncCommand diff(String from, String to) throws IOException, InterruptedException {
		return new AsyncCommand("hg", "diff", "--git", "-r", from, "-r", to).invoke();
	}

	private static class QuietPipedInputStream extends PipedInputStream {
		@Override
		public int read() throws IOException {
			try {
				return super.read();
			} catch (IOException e) {
				if(e.getMessage().equals("Write end dead")) {
					return -1;
				}
				throw e;
			}
		}
	}

	public static class AsyncCommand {
		private String[] args;
		private PipedInputStream snk;
		private Callable<Integer> exitCode;

		private AsyncCommand(String... args) {
			this.args = args;
		}

		public InputStream getOutput() {
			return snk;
		}

		public Callable<Integer> getExitCode() {
			return exitCode;
		}

		private AsyncCommand invoke() throws IOException, InterruptedException {
			snk = new QuietPipedInputStream();
			exitCode = new Command(args)
					.redirectError(System.err)
					.redirectOutput(new PipedOutputStream(snk))
					.start();
			return this;
		}
	}
}

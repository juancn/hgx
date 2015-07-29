package codng.hgx;

import codng.util.Command;
import codng.util.DefaultSequence;
import codng.util.NoRemoveIterator;
import codng.util.Sequence;
import codng.util.Sequences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hg {
	public static Sequence<Branch> branches() {
		return new DefaultSequence<Branch>() {
			@Override
			public Iterator<Branch> iterator() {
				try {
					final AsyncCommand invoke = new AsyncCommand("hg", "branches").invoke();
					final BufferedReader br = new BufferedReader(new InputStreamReader(invoke.getOutput()));
					return new NoRemoveIterator<Branch>() {
						@Override
						protected Branch advance() {
							try {
								return finishIfNull(Branch.parse(br.readLine()));
							} catch (IOException e) {
								throw (NoSuchElementException)new NoSuchElementException("I/O error").initCause(e);
							}
						}
					};
				} catch (IOException e) {
					throw new IllegalStateException(e);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	}
	
	public static class Branch {
		private static final Pattern BRANCH_PATTERN = Pattern.compile("(.*)\\s+(\\d+:[0-9a-fA-F]+)( \\(inactive\\))?");
		public final String name; 
		public final Id id; 
		public final boolean active;

		private Branch(String name, Id id, boolean active) {
			this.active = active;
			this.name = name;
			this.id = id;
		}
		
		private static Branch parse(String s) {
			if(s == null) return null;
			//new-custom-time-periods     1025:b731c63b3ea0 (inactive)
			final Matcher matcher = BRANCH_PATTERN.matcher(s);
			if(!matcher.matches()) {
				throw new IllegalArgumentException("Cannot parse: " + s);
			}
			return new Branch(matcher.group(1).trim(), Id.parse(matcher.group(2)), matcher.group(3) == null);
		}
	}

	public static String branch() throws IOException {
		return Command.executeSimple("hg", "branch").trim();
	}

	static String id() throws IOException {
		return Command.executeSimple("hg", "id", "-r", "0").trim();
	}

	static AsyncCommand log(int since) throws IOException, InterruptedException {
		return new AsyncCommand("hg", "--config", "defaults.log=", "log", "-r", since +":").invoke();
	}

	public static java.util.List<ChangeSet> log(String revSet) throws IOException, InterruptedException, ParseException {
		return Sequences.reverse(ChangeSet.loadFrom(new AsyncCommand("hg", "--config", "defaults.log=", "log", "-r", revSet).invoke().getOutput())).toList();
	}

	static AsyncCommand diff(String rev) throws IOException, InterruptedException {
		return new AsyncCommand("hg", "diff", "--git", "-c", rev).invoke();
	}

	private static class QuietPipedInputStream extends PipedInputStream {
		@Override
		public int read() throws IOException {
			try {
				return super.read();
			} catch (InterruptedIOException e) {
				// Close the stream to avoid stalling the other endpoint
				close();
				throw e;
			} catch (IOException e) {
				if(e.getMessage() != null && e.getMessage().equals("Write end dead")) {
					return -1;
				}
				throw e;
			}
		}
	}

	public static class AsyncCommand {
		private String[] args;
		private PipedInputStream output;
		private Callable<Integer> exitCode;

		private AsyncCommand(String... args) {
			this.args = args;
		}

		public InputStream getOutput() {
			return output;
		}

		public Callable<Integer> getExitCode() {
			return exitCode;
		}

		private AsyncCommand invoke() throws IOException, InterruptedException {
			output = new QuietPipedInputStream();
			exitCode = new Command(args)
					.redirectError(System.err)
					.redirectOutput(new PipedOutputStream(output))
					.start();
			return this;
		}
	}
}

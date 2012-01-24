package codng.hgx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;

public class ChangeSet {
	public final Id id;
	public final String branch;
	public final String tag;
	public final String user;
	public final Date date;
	public final String summary;
	public final List<Id> parents = new ArrayList<>();

	ChangeSet(Id id, String branch, String tag, String user, Date date, String summary) {
		this.id = id;
		this.branch = branch;
		this.tag = tag;
		this.user = user;
		this.date = date;
		this.summary = summary;
	}

	public static List<ChangeSet> loadFromCurrentDirectory() throws Exception {
		final String branch = Command.executeSimple("hg", "branch").trim();
		final PipedInputStream snk = new QuietPipedInputStream();
		
		Callable<Integer> exitCode = new Command("hg", "log", "--branch", branch)
				.redirectError(System.err)
				.redirectOutput(new PipedOutputStream(snk))
				.start();

		final List<ChangeSet> changeSets = loadFrom(snk);
		System.out.println("exit code: " + exitCode.call());
		return changeSets;
	}

	@Override
	public String toString() {
		return new Formatter()
				.format("id: %s\n", id)
				.format("branch: %s\n", branch)
				.format("tag: %s\n", tag)
				.format("user: %s\n", user)
				.format("date: %s\n", date)
				.format("summary: %s\n", summary)
				.format("parents: %s\n", parents)
				.toString();
	}

	public static List<ChangeSet> loadFrom(InputStream is) throws IOException, ParseException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		final List<ChangeSet> changeSets = new ArrayList<>();
		final List<Entry> entries = new ArrayList<>();
		for(String line = br.readLine();
			line != null;
			line = br.readLine()) {
			if(line.trim().isEmpty()) {
				// End of section
				changeSets.add(ChangeSet.parse(entries));
				entries.clear();
			} else {
				entries.add(Entry.parse(line));
			}
		}

		//Link parents
		for (int i = 0; i < changeSets.size()-1; i++) {
			final ChangeSet current = changeSets.get(i);
			final ChangeSet next = changeSets.get(i + 1);
			if(current.parents.isEmpty()) {
				current.parents.add(next.id);
			}
		}
		return changeSets;
	}

	private static ChangeSet parse(List<Entry> entries) throws IOException, ParseException {
		final Id id = Id.parse(firstOr(entries, "changeset", null));
		final String branch = firstOr(entries, "branch", "");
		final String tag = firstOr(entries, "tag", "");
		final String user = firstOr(entries, "user", "");
		final Date date = DATE_FORMAT.parse(firstOr(entries, "date", ""));
		final String summary = firstOr(entries, "summary", "");
		final ChangeSet changeSet = new ChangeSet(id, branch, tag, user, date, summary);
		changeSet.parents.addAll(parents(entries));
		return changeSet;
	}

	private static List<Id> parents(List<Entry> entries) {
		final ArrayList<Id> result = new ArrayList<>();
		for (Entry entry : entries) {
			if(entry.key.equals("parent")) {
				result.add(Id.parse(entry.value));
			}
		}
		return result;
	}

	private static String firstOr(List<Entry> entries, String key, String alternative) {
		for (Entry entry : entries) {
			if(entry.key.equals(key)) {
				return entry.value;
			}
		}
		return alternative;
	}
	
	static class Entry {
		public final String key;
		public final String value;

		Entry(String key, String value) {
			this.key = key;
			this.value = value;
		}

		static Entry parse(String s) {
			final String[] parts = s.split(":\\s+", 2);
			if(parts.length != 2) {
				throw new IllegalArgumentException("Cannot parse: " + s);
			}
			return new Entry(parts[0], parts[1]);
		}
	}

	/** Thu Jan 12 09:54:28 2012 -0800 */
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

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
}

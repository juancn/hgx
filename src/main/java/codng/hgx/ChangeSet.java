package codng.hgx;

import codng.util.DefaultFunction;
import codng.util.DefaultPredicate;
import codng.util.Sequence;
import codng.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import static codng.util.Sequences.asSequence;
import static codng.util.Sequences.reverse;

public class ChangeSet 
		implements Serializable 
{
	public final Id id;
	public final String branch;
	public final String user;
	public final Date date;
	public final String summary;
	private final List<Id> parents = new ArrayList<>();
	private final List<String> tags = new ArrayList<>();

	private ChangeSet(final Id id, final String branch, final String user, final Date date, final String summary) {
		this.id = id;
		this.branch = branch;
		this.user = user;
		this.date = date;
		this.summary = summary;
	}

	private ChangeSet(final ChangeSet other) {
		this(other.id, other.branch, other.user, other.date, other.summary);
		parents.addAll(other.parents);
		tags.addAll(other.tags);
	}

	public static Sequence<ChangeSet> loadFromCurrentDirectory() throws Exception {
		final String id = Cache.repositoryId();

		final StopWatch total = new StopWatch(), partial = new StopWatch();

		System.out.println("Loading history...");
		total.reset();
		int since = Cache.loadLastRevision(id);

		// Trigger hg log in the background, while we load the history
		final Hg.AsyncCommand asyncCommand = Hg.log(since);

		final List<ChangeSet> changeSets;

		// Load the cached history
		if (since > 0) {
			partial.reset();
			changeSets = Cache.loadHistory(id);
			System.out.printf("\tCache load: %dms\n", partial.elapsed());
		} else {
			changeSets = new ArrayList<>();
		}

		// Fetch the results of hg log
		partial.reset();
		final List<ChangeSet> updated = loadFrom(asyncCommand.getOutput());
		final Callable<Integer> exitCode = asyncCommand.getExitCode();

		System.out.println("\t[hg log] exit code: " + exitCode.call());
		System.out.printf("\t[hg log] took (in parallel): %dms, retrieved %d entries\n", partial.elapsed(), updated.size());

		// And merge with cached history
		partial.reset();
		if(!updated.isEmpty()) {
			assert changeSets.isEmpty() || updated.get(0).id.equals(last(changeSets).id);
			final int from;
			if (changeSets.isEmpty()) {
				changeSets.addAll(updated);
				from = 0;
			} else {
				from = changeSets.size()-1;
				changeSets.addAll(updated.subList(1, updated.size()));
			}
			linkParents(from, changeSets);

			// I'm exploiting the fact that changeSets and id are now a final field of the inner class, linkParents happens-before new Thread()
			// and that the Thread object is completely initialized before start is called (hence changeSets and id are properly read).
			new Thread("Cache updater") {
				@Override
				public void run() {
					Cache.saveHistory(id, changeSets);
					Cache.saveLastRevision(id, last(changeSets).id.seqNo);
					System.out.println("Cache updated.");
				}
			}.start();
		}
		verifyIntegrity(changeSets);

		System.out.printf("\tHistory update and parent linking: %dms\n", partial.elapsed());
		System.out.printf("Done! Took %dms\n", total.elapsed());
		return reverse(changeSets);
	}

	private static void verifyIntegrity(List<ChangeSet> changeSets) {
		final int size = changeSets.size();
		for (int i = 0; i < size; i++) {
			final ChangeSet c = changeSets.get(i);
			if(c.id.seqNo != i) {
				throw new IllegalStateException("Non sequential history? " + i + " -> " + c.id);
			}
		}
	}

	public static List<ChangeSet> filterBranch(final String branch, final Iterable<ChangeSet> changeSets, boolean branchOnly) {
		final List<ChangeSet> result = new ArrayList<>();
		final Set<Id> unresolvedParents = new HashSet<>();
		final Set<Id> inBranch = new HashSet<>();
		for (ChangeSet changeSet : changeSets) {
			if(matchBranch(branch, changeSet)) inBranch.add(changeSet.id);
			if(matchBranch(branch, changeSet) || unresolvedParents.contains(changeSet.id)) {
				// Create a copy. The history save thread might not be done yet.
				final ChangeSet copy = new ChangeSet(changeSet);
				result.add(copy);
				unresolvedParents.addAll(copy.parents);
				unresolvedParents.remove(copy.id);
				// Attempt to keep current branch to the left
				Collections.sort(copy.parents, new Comparator<Id>() {
					@Override
					public int compare(Id o1, Id o2) {
						if(o1.equals(o2)) return 0;
						if(inBranch.contains(o1)) return -1;
						if(inBranch.contains(o2)) return 1;
						return o1.seqNo - o2.seqNo;
					}
				});
			}
		}

		// Group all changes belonging to the branch at the top
		Collections.sort(result, new Comparator<ChangeSet>() {
			@Override
			public int compare(ChangeSet o1, ChangeSet o2) {
				final boolean b1 = matchBranch(branch, o1);
				final boolean b2 = matchBranch(branch, o2);
				if(b1 && !b2) return -1;
				if(!b1 && b2) return  1;
				return (int) (o2.id.seqNo - o1.id.seqNo);
			}
		});

		return branchOnly ? filterBranchOnly(branch, inBranch, result) : result;
	}

	private static boolean matchBranch(String branch, ChangeSet changeSet) {
		return changeSet.branch.matches(branch);
	}

	private static List<ChangeSet> filterBranchOnly(final String branch, final Set<Id> inBranch, final List<ChangeSet> changeSets) {
		if(changeSets.isEmpty()) return changeSets;
		final List<ChangeSet> result = new ArrayList<>();
		for (int i = 0; i < changeSets.size()-1; i++) {
			final ChangeSet current = changeSets.get(i);
			final ChangeSet next = changeSets.get(i+1);
			if(!matchBranch(branch, next)) {
				break;
			}
			current.parents.retainAll(inBranch);
			result.add(current);
		}

		return result;
	}

	private static <X> X last(List<X> list) {
		return list.get(list.size() - 1);
	}

	@Override
	public String toString() {
		return new Formatter()
				.format("id: %s\n", id)
				.format("branch: %s\n", branch)
				.format("user: %s\n", user)
				.format("date: %s\n", date)
				.format("summary: %s\n", summary)
				.format("parents: %s\n", parents)
				.format("tags: %s\n", tags)
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

		return changeSets;
	}


	private static List<ChangeSet> linkParents(int from, final List<ChangeSet> changeSets) {
		//Link parents
		for (int i = from; i < changeSets.size()-1; i++) {
			final ChangeSet child = changeSets.get(i + 1);
			final ChangeSet parent = changeSets.get(i);
			if(child.parents.isEmpty()) {
				child.parents.add(parent.id);
			}
		}
		return changeSets;
	}

	private static ChangeSet parse(List<Entry> entries) throws IOException, ParseException {
		final Id id = Id.parse(firstOr(entries, "changeset", null));
		final String branch = firstOr(entries, "branch", "default");
		final String user = firstOr(entries, "user", "");
		final Date date = DATE_FORMAT.get().parse(firstOr(entries, "date", ""));
		final String summary = firstOr(entries, "summary", "");
		final ChangeSet changeSet = new ChangeSet(id, branch, user, date, summary);
		changeSet.parents.addAll(parents(entries));
		for(String tag : filter(entries, "tag")) changeSet.tags.add(tag);
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
	
	private static Sequence<String> filter(final List<Entry> entries, final String key) {
		return asSequence(entries).filter(new DefaultPredicate<Entry>(){
			@Override
			public boolean apply(Entry entry) {
				return entry.key.equals(key);
			}
		}).map(new DefaultFunction<Entry, String>(){
			@Override
			public String apply(Entry entry) {
				return entry.value;
			}
		});
	}

	private static String firstOr(List<Entry> entries, String key, String alternative) {
		for (Entry entry : entries) {
			if(entry.key.equals(key)) {
				return entry.value;
			}
		}
		return alternative;
	}

	public Sequence<Id> parents() {
		return asSequence(parents);
	}

	public Sequence<String> tags() {
		return asSequence(tags);
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
	private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
		}
	};
	private static final long serialVersionUID = 4;
}

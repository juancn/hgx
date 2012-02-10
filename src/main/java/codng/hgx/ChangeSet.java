package codng.hgx;

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
import java.util.Set;
import java.util.concurrent.Callable;

public class ChangeSet 
		implements Serializable 
{
	public final Id id;
	public final String branch;
	public final String user;
	public final Date date;
	public final String summary;
	public final List<Id> parents = new ArrayList<>();
	public final List<String> tags = new ArrayList<>();

	ChangeSet(Id id, String branch, String user, Date date, String summary) {
		this.id = id;
		this.branch = branch;
		this.user = user;
		this.date = date;
		this.summary = summary;
	}

	public static List<ChangeSet> loadFromCurrentDirectory() throws Exception {
		final String id = Hg.id();

		System.out.println("Loading history...");
		long start, end;
		
		start = System.currentTimeMillis();
		final List<ChangeSet> changeSets = Cache.loadHistory(id);
		end = System.currentTimeMillis();
		System.out.printf("\tCache load: %dms\n", end-start);

		start = System.currentTimeMillis();

		int since = changeSets.isEmpty() ? 0 : (int) last(changeSets).id.seqNo;
		final Hg.AsyncCommand asyncCommand = Hg.log(since);

		final List<ChangeSet> updated = loadFrom(asyncCommand.getOutput());

		final Callable<Integer> exitCode = asyncCommand.getExitCode();

		System.out.println("\t[hg log] exit code: " + exitCode.call());
		end = System.currentTimeMillis();
		System.out.printf("\t[hg log] took: %dms, retrieved %d entries\n", end-start, updated.size());
		
		start = System.currentTimeMillis();
		if(!updated.isEmpty()) {
			assert changeSets.isEmpty() || updated.get(0).id.equals(last(changeSets).id);
			if (changeSets.isEmpty()) {
				changeSets.addAll(updated);
			} else {
				changeSets.addAll(updated.subList(1, updated.size()));
			}
			Cache.saveHistory(id, changeSets);
		}

		verifyIntegrity(changeSets);
		
		Collections.reverse(changeSets);
		
		linkParents(changeSets);
		end = System.currentTimeMillis();
		System.out.printf("\tCache update and parent linking: %dms\n", end-start);
		System.out.println("Done!");
		return changeSets;
	}

	private static void verifyIntegrity(List<ChangeSet> changeSets) {
		for (int i = 0; i < changeSets.size(); i++) {
			ChangeSet c = changeSets.get(i);
			if(c.id.seqNo != i) {
				throw new IllegalStateException("Non sequential history? " + i + " -> " + c.id);
			}
		}
	}

	public static List<ChangeSet> filterBranch(final String branch, List<ChangeSet> changeSets, boolean branchOnly) {
		final List<ChangeSet> result = new ArrayList<>();
		final Set<Id> unresolvedParents = new HashSet<>();
		final Set<Id> inBranch = new HashSet<>();
		for (ChangeSet changeSet : changeSets) {
			if(matchBranch(branch, changeSet)) inBranch.add(changeSet.id);
			if(matchBranch(branch, changeSet) || unresolvedParents.contains(changeSet.id)) {
				result.add(changeSet);
				unresolvedParents.addAll(changeSet.parents);
				unresolvedParents.remove(changeSet.id);
				// Attempt to keep current branch to the left
				Collections.sort(changeSet.parents, new Comparator<Id>() {
					@Override
					public int compare(Id o1, Id o2) {
						if(o1.equals(o2)) return 0;
						if(inBranch.contains(o1)) return -1;
						if(inBranch.contains(o2)) return 1;
						return (int) (o1.seqNo - o2.seqNo);
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

	private static List<ChangeSet> filterBranchOnly(String branch, Set<Id> inBranch, List<ChangeSet> result) {
		if(result.isEmpty()) return result;
		int i = 0;
		for (; i < result.size()-1; i++) {
			ChangeSet current = result.get(i);
			ChangeSet next = result.get(i+1);
			if(!matchBranch(branch, next)) {
				break;
			}
			current.parents.retainAll(inBranch);			
		}

		return result.subList(0, i+1);
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

	public static List<ChangeSet> linkParents(final List<ChangeSet> changeSets) {
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
		final String branch = firstOr(entries, "branch", "default");
		final String user = firstOr(entries, "user", "");
		final Date date = DATE_FORMAT.parse(firstOr(entries, "date", ""));
		final String summary = firstOr(entries, "summary", "");
		final ChangeSet changeSet = new ChangeSet(id, branch, user, date, summary);
		changeSet.parents.addAll(parents(entries));
		changeSet.tags.addAll(filter(entries, "tag"));
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
	
	private static List<String> filter(List<Entry> entries, String key) {
		// I desperately need Guava or something alike
		final ArrayList<String> result = new ArrayList<>();
		for (Entry entry : entries) {
			if(entry.key.equals(key)) {
				result.add(entry.value);
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
	private static final long serialVersionUID = 1;
}

package codng.hgx;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Main {
	
	static class Id {
		public final long seqNo; 
		public final String hash;

		Id(long seqNo, String hash) {
			this.seqNo = seqNo;
			this.hash = hash;
		}

		@Override
		public int hashCode() {
			return (int)seqNo + 37*hash.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Id) {
				Id other = (Id) obj;
				return seqNo == other.seqNo && hash.equals(other.hash);
			}
			return false;
		}

		@Override
		public String toString() {
			return seqNo + ":" + hash;
		}

		static Id parse(String s) {
			final String[] parts = s.split(":");
			if(parts.length != 2) {
				throw new IllegalArgumentException("Cannot parse: " + s);
			}
			return new Id(Long.parseLong(parts[0]), parts[1]);
		}
	}
	
	static class ChangeSet {
		public final Id id;
		public final String branch;
		public final String tag;
		public final String user;
		public final Date date;
		public final String summary;
		public final List<Id> parents = new ArrayList<>();

		ChangeSet(Id id, String branch, String tag,String user, Date date, String summary) {
			this.id = id;
			this.branch = branch;
			this.tag = tag;
			this.user = user;
			this.date = date;
			this.summary = summary;
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

		static List<ChangeSet> loadFrom(InputStream is) throws IOException, ParseException {
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
	}
	
	static class Cell {
		public final Id id;
		public int parent;

		Cell(Id id, int parent) {
			this.id = id;
			this.parent = parent;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Cell) {
				Cell cell = (Cell) obj;
				return cell.id.equals(id);
			}
			return false;
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		final InputStream is = new FileInputStream(args[0]);
		final List<ChangeSet> changeSets = ChangeSet.loadFrom(is);
		dotify(changeSets, 1000, "log.dot", "case16146");
		final List<Cell> unresolved = new ArrayList<>();
		int count = 200;
		for (ChangeSet changeSet : changeSets) {
			if(!changeSet.branch.equals("case16146")) continue;
			final Cell cell = new Cell(changeSet.id, -1);
			int index = unresolved.indexOf(cell);
			StringBuilder sb = new StringBuilder();
			if(index != -1) {
				unresolved.set(index, new Cell(changeSet.parents.get(0), index));
				for(int i = 0; i < unresolved.size(); i++) {
					final Cell c = unresolved.get(i);
					if(c.equals(cell)) sb.append('-');
					else if(i == index) sb.append('*');
					else if(c.parent == i) sb.append('|');
					else if(c.parent > i) sb.append('\\');
					else if(c.parent < i) sb.append('/');
					
				}
				while (unresolved.remove(cell));

				addAll(unresolved, index, changeSet.parents.subList(1, changeSet.parents.size()));

			} else {
				for(int i = 0; i < unresolved.size(); i++) sb.append('|');
				sb.append('*');
				final List<Id> parents = changeSet.parents;
				addAll(unresolved, unresolved.size(), parents);
			}

			System.out.printf("%-80s %s: (%s) %s\n", sb, changeSet.id, changeSet.user, changeSet.summary.trim());
			if(--count == 0) {
				break;
			}
		}
	}

	private static void addAll(List<Cell> unresolved, int index, List<Id> parents) {
		for (Id id : parents) {					
			unresolved.add(index, new Cell(id, index));
		}
	}

	private static void dotify(List<ChangeSet> changeSets, int count, String filename, String branch) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
		pw.println("digraph history {");
		pw.println("node [shape=box];");
		for (ChangeSet changeSet : changeSets) {
			if(branch == null || changeSet.branch.equals(branch)) {
				pw.printf("%s [label=\"%s\\n (%s) %s\"];\n", changeSet.id.seqNo, changeSet.id, changeSet.user, chop(changeSet.summary));
				for (Id parent : changeSet.parents) {
					pw.printf("%s -> %s;\n", changeSet.id.seqNo, parent.seqNo);				
				}
				if(--count == 0) {
					break;
				}
			}
		}
		pw.println("}");
		pw.close();
	}

	private static String chop(String summary) {
		final String escaped = summary.replace("\"", "\\\"");
		final int width = 30;
		if(escaped.length() > width) {
			final String[] split = escaped.split(" ");
			StringBuilder sb = new StringBuilder();
			int last = 0;
			for (String s : split) {
				sb.append(s).append(' ');
				if(sb.length() / width > last) {
					last = sb.length() / width;
					sb.append("\\n");
				}
			}
			return sb.toString();
		}
		return escaped;
	}

}

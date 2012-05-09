package codng.hgx;


import codng.hgx.Hg.AsyncCommand;
import codng.util.Tuple;
import codng.util.Tuple3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiff {

	private final List<FileChanges> files = new ArrayList<>();

	public GitDiff merge(GitDiff other) {
		final GitDiff result = new GitDiff();
		final Map<String, FileChanges> changes = changesByFile();
		for (FileChanges otherChanges : other.files) {
			final FileChanges myChanges = changes.get(otherChanges.file);
			if (myChanges == null) {
				result.files.add(otherChanges);
			} else {
				result.files.add(myChanges.merge(otherChanges));
			}
		}
		return result;
	}

	public Map<String, FileChanges> changesByFile() {
		final HashMap<String, FileChanges> result = new HashMap<>();
		for (FileChanges fileChanges : files) {
			result.put(fileChanges.file, fileChanges);
		}
		return result;
	}

	public void parse(final BufferedReader br) {
		try {
			int oldStart = -1, newStart = -1;

			boolean skipDiff = false;
			FileChanges fileChanges = null;
			DiffHunk hunk = null;
			for(String line = br.readLine(); line != null ; line = br.readLine())  {
				if(line.startsWith("diff")) {
					skipDiff = false;
					fileChanges = file(line);
					hunk = null;
				} else if(line.startsWith("new file mode")) { // I should check that we're still in the header
					fileChanges.header(line);
				} else if(line.startsWith("deleted file mode")) {
					fileChanges.header(line);
				} else if(line.startsWith("index ")) {
					fileChanges.header(line);
				} else if(line.startsWith("Binary file ")) {
					fileChanges.header(line);
					skipDiff = true;
				} else if(line.startsWith("GIT binary patch")) {
					skipDiff = true;
					fileChanges.header(line);
				} else if(line.startsWith("+++")) {
					// Don't care
					fileChanges.header(line);
				} else if(line.startsWith("---")) {
					// Don't care
					fileChanges.header(line);
				} else if(line.startsWith("@@")) {
					hunk = fileChanges.hunk(line);
					oldStart = hunk.oldStart;
					newStart = hunk.newStart;
				} else if(line.startsWith("-")) {
					hunk.line(oldStart, -1, line);
					++oldStart;
				} else if(line.startsWith("+")) {
					hunk.line(-1, newStart, line);
					++newStart;
				} else if(!skipDiff) {
					hunk.line(oldStart, newStart, line);
					++oldStart; ++newStart;
				}
			}
		} catch (IOException e) {
			throw new Error("This shouldn't happen!");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	private FileChanges file(String line) {
		final FileChanges fileChanges = new FileChanges(line);
		files.add(fileChanges);
		return fileChanges;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (FileChanges file : files) {
			sb.append(file);
		}
		return sb.toString();
	}


	public static class FileChanges {
		private final String file;
		private final String headerLine;
		private final List<String> header = new ArrayList<>();
		private final List<DiffHunk> hunks = new ArrayList<>();

		public FileChanges(final String headerLine) {
			this.headerLine = headerLine;
			final Matcher matcher = DIFF_PATTERN.matcher(headerLine);
			if (matcher.matches()) {
				file = matcher.group(2);
			} else {
				// Malformed diff
				throw new IllegalArgumentException("malformed line: " + headerLine);
			}
		}

		public void header(final String line) {
			header.add(line);
		}

		public DiffHunk hunk(String line) {
			final DiffHunk hunk = new DiffHunk(line);
			hunks.add(hunk);
			return hunk;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(headerLine).append('\n');
			for (String line : header) {
				sb.append(line).append('\n');
			}

			for (DiffHunk hunk : hunks) {
				sb.append(hunk);
			}
			return sb.toString();
		}

		public FileChanges merge(FileChanges otherChanges) {
			if(!file.equals(otherChanges.file)) {
				throw new IllegalArgumentException(String.format("Files don't match, expected %s but got %s", file, otherChanges.file));
			}
			final FileChanges result = new FileChanges(headerLine);
			for (DiffHunk myHunk : hunks) {
				final List<DiffHunk> overlapping = otherChanges.overlapping(myHunk);
					result.hunks.add(myHunk.merge(overlapping));
			}
			return result;
		}

		private List<DiffHunk> overlapping(final DiffHunk other) {
			final ArrayList<DiffHunk> result = new ArrayList<>();
			for (DiffHunk hunk : hunks) {
				if(other.overlaps(hunk)) {
					result.add(hunk);
				}
			}
			return result;
		}
	}

	public static class DiffHunk {
		private final String header;
		private final int oldStart;
		private final int newStart;
		private int oldEnd;
		private int newEnd;
		private final List<Tuple3<Integer, Integer, String>> lines = new ArrayList<>();


		public DiffHunk(final String header) {
			this.header = header;
			final Matcher matcher = HUNK_PATTERN.matcher(header);
			if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
			oldEnd = oldStart = Integer.parseInt(matcher.group(1));
			newEnd = newStart = Integer.parseInt(matcher.group(3));
		}

		public void line(int oldLine, int newLine, String line) {
			oldEnd = Math.max(oldEnd, oldLine);
			newEnd = Math.max(newEnd, newLine);
			lines.add(Tuple.make(oldLine, newLine, line));
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(header).append('\n');
			for (Tuple3<Integer, Integer, String> line : lines) {
				sb.append(String.format("%4d %4d %s", line.first, line.second, line.third)).append('\n');
			}
			return sb.toString();
		}

		public DiffHunk merge(List<DiffHunk> overlapping) {
			final DiffHunk result = new DiffHunk(header); //TODO: Recalculate
			final List<Tuple3<Integer, Integer, String>> collected = new ArrayList<>();
			collected.addAll(lines);
			for (DiffHunk other : overlapping) {
				collected.addAll(other.lines);
			}
			Collections.sort(collected, new Comparator<Tuple3<Integer, Integer, String>>() {
				@Override
				public int compare(Tuple3<Integer, Integer, String> o1, Tuple3<Integer, Integer, String> o2) {
					int cmp1 = Integer.compare(Math.max(o1.first, o1.second), Math.max(o2.first, o2.second));
					int cmp2 = 0;//Integer.compare(o1.second, o2.second);
					int cmp3 = o1.third.compareTo(o2.third);
					return cmp1 != 0 ? cmp1 : cmp2 != 0 ? cmp2 : cmp3 != 0 ? cmp3 : 0;
				}
			});

			String last = null;
			for (Tuple3<Integer, Integer, String> line : collected) {
				if(!line.third.equals(last)) {
					result.lines.add(line);
					last = line.third;
				}
			}

			return result;  //To change body of created methods use File | Settings | File Templates.
		}

		public boolean overlaps(DiffHunk hunk) {
			return is(hunk.oldStart).between(oldStart, newStart)
					|| is(hunk.oldEnd).between(oldStart, newStart)
					|| is(hunk.newStart).between(newStart, newEnd)
					|| is(hunk.newEnd).between(newStart, newEnd)
					;
		}
	}

	public static final class RichInt {
		final int x;
		public RichInt(int x) { this.x = x; }
		public final boolean between(int a, int b) { return x >= a && x <= b; }
	}
	public static RichInt is(int x) { return new RichInt(x); }

	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");

	public static void main(String[] args) throws IOException, InterruptedException {
		GitDiff gitDiff = parseDiff("968769608dd6");
		System.out.println("#################################################################");
		System.out.println(gitDiff);
		GitDiff gitDiff1 = parseDiff("2247b5ebe951");
		System.out.println("#################################################################");
		System.out.println(gitDiff1);

		GitDiff merge = gitDiff.merge(gitDiff1);
		System.out.println("#################################################################");
		System.out.println(merge);
	}

	private static GitDiff parseDiff(String rev) throws IOException, InterruptedException {
		AsyncCommand diff = Hg.diff(rev);
		StringWriter sw = new StringWriter();
		Cache.transfer(new InputStreamReader(diff.getOutput(), "UTF8"), sw);
		GitDiff gitDiff = new GitDiff();
		gitDiff.parse(new BufferedReader(new StringReader(sw.toString())));
		return gitDiff;
	}

}

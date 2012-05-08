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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiff {

	public void parse(final BufferedReader br) {
		try {
			int oldStart = -1, newStart = -1;

			boolean skipDiff = false;
			int lineCount = 0;
			FileChanges fileChanges = null;
			DiffHunk hunk = null;
			for(String line = br.readLine(); line != null ; line = br.readLine())  {
				++lineCount;
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

		return fileChanges;
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
	}

	public static class DiffHunk {
		private final String header;
		private int oldStart;
		private int newStart;
		private final List<Tuple3<Integer, Integer, String>> lines = new ArrayList<>();


		public DiffHunk(final String header) {
			this.header = header;
			final Matcher matcher = HUNK_PATTERN.matcher(header);
			if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
			oldStart = Integer.parseInt(matcher.group(1));
			newStart = Integer.parseInt(matcher.group(3));
		}

		public void line(int oldLine, int newLine, String line) {
			lines.add(Tuple.make(oldLine, newLine, line));
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(header).append('\n');
			for (Tuple3<Integer, Integer, String> line : lines) {
				sb.append(line.third).append('\n');
			}
			return sb.toString();
		}
	}

	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");

	public static void main(String[] args) throws IOException, InterruptedException {
		AsyncCommand diff = Hg.diff(".");
		StringWriter sw = new StringWriter();
		Cache.transfer(new InputStreamReader(diff.getOutput(), "UTF8"), sw);
		new GitDiff().parse(new BufferedReader(new StringReader(sw.toString())));
	}

}

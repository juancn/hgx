import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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

		files.add(fileChanges);
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (FileChanges file : files) {
			sb.append(file);
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
		private final int oldStart;
		private final int newStart;
		private int oldEnd;
		private int newEnd;
			oldEnd = oldStart = Integer.parseInt(matcher.group(1));
			newEnd = newStart = Integer.parseInt(matcher.group(3));
			oldEnd = Math.max(oldEnd, oldLine);
			newEnd = Math.max(newEnd, newLine);
				sb.append(String.format("%4d %4d %s", line.first, line.second, line.third)).append('\n');

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
	public static final class RichInt {
		final int x;
		public RichInt(int x) { this.x = x; }
		public final boolean between(int a, int b) { return x >= a && x <= b; }
	}
	public static RichInt is(int x) { return new RichInt(x); }

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
		GitDiff gitDiff = new GitDiff();
		gitDiff.parse(new BufferedReader(new StringReader(sw.toString())));
		return gitDiff;
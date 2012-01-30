package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.Row;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RowViewer
		extends RichTextView {
	private Row row;
	
	public RowViewer() {
		this(null);
	}

	public RowViewer(Row row) {
		this.row = row;
	}

	public Row getRow() {
		return row;
	}

	public void setRow(Row row) {
		this.row = row;
		recalculate();
		revalidate();
		repaint();
	}

	private void recalculate() {
		lines.clear();
		startBlock = null;
		endBlock = null;
		if(row != null) {
			header("SHA:", row.changeSet.id);
			header("Author:", row.changeSet.user);
			header("Date:", row.changeSet.date);
			header("Summary:", text(row.changeSet.summary).bold());
			header("Parent:", row.changeSet.parents);
			header("Branch:", text(row.changeSet.branch).bold());
			hr();
			try {
				colorize(Cache.loadDiff(row));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void colorize(String diff) {
		final StringReader sr = new StringReader(diff);
		final BufferedReader br = new BufferedReader(sr);
		try {
			int oldStart = -1, newStart = -1;
			
			boolean skipDiff = false;
			Colorizer colorizer = Colorizer.plain(this);
			for(String line = br.readLine(); line != null; line = br.readLine())  {

				if(line.startsWith("diff")) {
					skipDiff = false;
					final Matcher matcher = DIFF_PATTERN.matcher(line);
					if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
					final String file = matcher.group(2);
					line().add(align(text(file).vgap(10).bold(), getParent().getWidth()-50).background(220, 220, 250));
				
					if(file.endsWith(".java")) {
						colorizer = new JavaColorizer(this);
					} else {
						colorizer = Colorizer.plain(this);
					}
				} else if(line.startsWith("new file mode")) { // I should check that we're still in the header
					line().add(code(line).rgb(127, 127, 127));
				} else if(line.startsWith("deleted file mode")) {
					line().add(code(line).rgb(127, 127, 127));
					line().add(text("File deleted").rgb(255, 0, 0));
				} else if(line.startsWith("index ")) {
					line().add(code(line).rgb(127, 127, 127));
				} else if(line.startsWith("Binary file ")) {
					skipDiff = true;
				} else if(line.startsWith("GIT binary patch")) {
					line().add(text("(Binary file, content not rendered)").rgb(127, 127, 127));
					skipDiff = true;
				} else if(line.startsWith("+++")) {
					// Don't care 
				} else if(line.startsWith("---")) {
					// Don't care 
				} else if(line.startsWith("@@")) {
					final Matcher matcher = HUNK_PATTERN.matcher(line);
					if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
					oldStart = Integer.parseInt(matcher.group(1));
					newStart = Integer.parseInt(matcher.group(3));
					line().add(code(line).rgb(127, 127, 127));
					colorizer.reset();
				} else if(line.startsWith("-")) {
					numbered(oldStart, -1, colorizer.colorizeLine(line).background(255, 238, 238));
					++oldStart;
				} else if(line.startsWith("+")) {
					numbered(-1, newStart, colorizer.colorizeLine(line).background(221, 255, 221));
					++newStart;
				} else if(!skipDiff) {
					numbered(oldStart, newStart, colorizer.colorizeLine(line));
					++oldStart; ++newStart;
				}
			}
		} catch (IOException e) {
			throw new Error("This shouldn't happen!");
		}
	}


	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
}

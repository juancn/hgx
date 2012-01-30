package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.ChangeSet;
import codng.hgx.Row;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RowViewer
		extends RichTextView {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private Row row;
	private ScheduledFuture<?> lastUpdate;

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
		if(lastUpdate != null) lastUpdate.cancel(true);
		this.row = row;
		recalculate();
		revalidate();
		repaint();
	}

	private void recalculate() {
		clear();
		if(row != null) {
			addHeader(row.changeSet);
			line().add(text("Loading...").rgb(200, 200, 200).bold().size(14));
			finishBuild();
			lastUpdate = scheduler.schedule(new Runnable() {
				final Row row = getRow();
				@Override
				public void run() {
					addHeader(this.row.changeSet);
					addDiff(this.row);
					if(interrupted()) return;
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								finishBuild();
								revalidate();
								repaint();
							}
						});
					} catch (InterruptedException | InvocationTargetException e) {
						// Don't care, really
						e.printStackTrace();
					}
				}

			}, 100, TimeUnit.MILLISECONDS);
		}
	}

	private boolean interrupted() {
		return Thread.currentThread().isInterrupted();
	}

	private void addHeader(final ChangeSet changeSet) {
		header("SHA:", changeSet.id);
		header("Author:", changeSet.user);
		header("Date:", changeSet.date);
		header("Summary:", text(changeSet.summary).bold());
		header("Parent:", changeSet.parents);
		header("Branch:", text(changeSet.branch).bold());
		hr();
	}

	private void addDiff(final Row row) {
		try {
			colorize(Cache.loadDiff(row));
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void colorize(BufferedReader br) {
		try {
			int oldStart = -1, newStart = -1;
			
			boolean skipDiff = false;
			Colorizer colorizer = Colorizer.plain(this);
			for(String line = br.readLine(); line != null && !interrupted(); line = br.readLine())  {

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
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}


	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
}

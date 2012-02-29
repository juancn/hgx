package codng.hgx.ui;

import codng.util.DefaultPredicate;
import codng.util.Predicate;

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

public abstract class DiffViewer<T>
		extends RichTextView {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> lastUpdate;
	private T data;

	public DiffViewer() { }
	public DiffViewer(final T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		if(lastUpdate != null) lastUpdate.cancel(true);
		this.data = data;
		recalculate();
		revalidate();
		repaint();
	}

	protected void recalculate() {
		clear();
		if(data != null) {
			final Text loading = loading();
			lastUpdate = scheduler.schedule(new Runnable() {
				final T data = getData();
				@Override
				public void run() {
					Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
					final DiffModel diffModel = createModel();
					diffModel.addHeader(this.data);
					diffModel.addDiff(this.data, new DefaultPredicate<String>() {
						@Override
						public boolean apply(final String status) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									loading.text(status);
									repaint();
								}
							});
							return true;
						}
					});
					if(interrupted()) return;
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								setModel(diffModel);
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

	private Text loading() {
		final DiffModel diffModel = createModel();
		diffModel.addHeader(data);
		final Text loading = diffModel.text("Loading...").color(Colors.LOADING).bold().size(14);
		diffModel.line().add(loading);
		setModel(diffModel);
		return loading;
	}

	protected DiffModel createModel() {
		return new DiffModel();
	}
	protected abstract BufferedReader loadDiff(T data) throws IOException, InterruptedException;

	private boolean interrupted() {
		return Thread.currentThread().isInterrupted();
	}

	public class DiffModel extends Model {
		private int fileIndex;

		protected void addHeader(final T data) {
			fileIndex = lines.size();
			hr();
		}

		private void colorize(final BufferedReader br, final Predicate<String> status) {
			try {
				int oldStart = -1, newStart = -1;

				boolean skipDiff = false;
				Colorizer colorizer = Colorizer.plain(this);
				int lineCount = 0;
				for(String line = br.readLine(); line != null && !interrupted(); line = br.readLine())  {
					++lineCount;
					// Pick a nice prime so numbers are not all round
					if(lineCount > 0 && lineCount % 1009 == 0) {
						status.apply(String.format("Loading... (syntax highlighting, %s lines processed)", lineCount));
					}
					if(line.startsWith("diff")) {
						skipDiff = false;
						final Matcher matcher = DIFF_PATTERN.matcher(line);
						if (matcher.matches()) {
							final String file = matcher.group(2);

							final Strip fileLine = line().add(align(text(file).vgap(10).bold(), getParent().getWidth() - 50).background(Colors.FILE_BG));
							addFileHeader(lineCount, file, fileLine);

							if(file.endsWith(".java")) {
								colorizer = new JavaColorizer(this);
							} else {
								colorizer = Colorizer.plain(this);
							}     
						} else {
							// Malformed diff
							line().add(text("(malformed)").color(Colors.WARNING), code(line));
						}
					} else if(line.startsWith("new file mode")) { // I should check that we're still in the header
						line().add(code(line).color(Colors.DE_EMPHASIZE));
					} else if(line.startsWith("deleted file mode")) {
						line().add(code(line).color(Colors.DE_EMPHASIZE));
						line().add(text("File deleted").color(Colors.WARNING));
					} else if(line.startsWith("index ")) {
						line().add(code(line).color(Colors.DE_EMPHASIZE));
					} else if(line.startsWith("Binary file ")) {
						skipDiff = true;
					} else if(line.startsWith("GIT binary patch")) {
						line().add(text("(Binary file, content not rendered)").color(Colors.DE_EMPHASIZE));
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
						line().add(code(line).color(Colors.DE_EMPHASIZE));
						colorizer.reset();
					} else if(line.startsWith("-")) {
						numbered(oldStart, -1, colorizer.colorizeLine(line).background(Colors.REMOVED_BG));
						++oldStart;
					} else if(line.startsWith("+")) {
						numbered(-1, newStart, colorizer.colorizeLine(line).background(Colors.LINE_ADDED_BG));
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

		private void addFileHeader(int lineCount, String file, Block lineStart) {
			final String label = lineCount == 1 ? "Files:" : "";
			lines.add(fileIndex++, strip()
					.add(
							align(text(label).color(Colors.DE_EMPHASIZE).bold(), 100).right(),
							text(file).color(Colors.LINK).underline().linkTo(lineStart))
			);
		}

		private void addDiff(final T data, final Predicate<String> status) {
			try {
				colorize(loadDiff(data), status);
			} catch (IOException | InterruptedException | RuntimeException e) {
				e.printStackTrace();
			}
		}
	}

	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
}

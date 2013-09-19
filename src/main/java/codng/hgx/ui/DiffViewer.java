package codng.hgx.ui;

import codng.hgx.ui.rtext.Block;
import codng.hgx.ui.rtext.HBox;
import codng.hgx.ui.rtext.RichTextView;
import codng.hgx.ui.rtext.RichTextViewModel;
import codng.hgx.ui.rtext.Strip;
import codng.hgx.ui.rtext.Text;
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

/**
 * RichTextView that knows how to colorize a git-style diff.
 * This component delay loads the diff and processes the coloring in a background thread.
 * @param <T> type of diff source data.
 */
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
					final DiffModel<T> diffModel = createModel();
					diffModel.addHeader(this.data);
					diffModel.addDiff(this.data, new DefaultPredicate<String>() {
						@Override
						public boolean accepts(final String status) {
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
		final DiffModel<T> diffModel = createModel();
		diffModel.addHeader(data);
		final Text loading = diffModel.text("Loading...").color(Colors.LOADING).bold().size(14);
		diffModel.line().add(loading);
		setModel(diffModel);
		return loading;
	}

	protected DiffModel<T> createModel() {
		return new DiffModel<>(this);
	}
	protected abstract BufferedReader loadDiff(T data) throws IOException, InterruptedException;

	boolean interrupted() {
		return Thread.currentThread().isInterrupted();
	}

	public static class DiffModel<T> extends RichTextViewModel<DiffViewer<T>> {
		private int fileIndex;

		public DiffModel(DiffViewer<T> richTextView) {
			super(richTextView);
		}

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
				for(String line = br.readLine(); line != null && !richTextView.interrupted(); line = br.readLine())  {
					++lineCount;
					// Pick a nice prime so numbers are not all round
					if(lineCount > 0 && lineCount % 1009 == 0) {
						status.accepts(String.format("Loading... (syntax highlighting, %s lines processed)", lineCount));
					}
					if(line.startsWith("diff")) {
						skipDiff = false;
						final Matcher matcher = DIFF_PATTERN.matcher(line);
						if (matcher.matches()) {
							final String file = matcher.group(2);

							final Strip fileLine = line().add(align(text(file).vgap(10).bold(), richTextView.getParent().getWidth() - 50).background(Colors.FILE_BG));
							addFileHeader(lineCount, file, fileLine);

							if(file.endsWith(".java") || file.endsWith(".js")) {
								colorizer = new JavaColorizer(this);
							} else if(file.endsWith(".m")    || file.endsWith(".mm")
									|| file.endsWith(".c")   || file.endsWith(".h")
									|| file.endsWith(".cpp") || file.endsWith(".hpp")
									) {
								colorizer = new CLikeColorizer(this);
							} else if (file.endsWith(".thrift")) {
								colorizer = new ThriftColorizer(this);
							} else {
								colorizer = Colorizer.plain(this);
							}     
						} else {
							// Malformed diff
							line().add(warning("(malformed)"), code(line));
						}
					} else if(line.startsWith("new file mode")) { // I should check that we're still in the header
						line().add(deemphasize(line));
					} else if(line.startsWith("deleted file mode")) {
						line().add(deemphasize(line));
						line().add(warning("File deleted"));
					} else if(line.startsWith("index ")) {
						line().add(deemphasize(line));
					} else if(line.startsWith("Binary file ")) {
						skipDiff = true;
					} else if(line.startsWith("GIT binary patch")) {
						line().add(deemphasize("(Binary file, content not rendered)"));
						skipDiff = true;
					} else if(line.startsWith("+++")) {
						// Don't care
					} else if(line.startsWith("---")) {
						// Don't care
					} else if(line.startsWith("@@")) {
						final Matcher matcher = HUNK_PATTERN.matcher(line);
						if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff: " + line);
						oldStart = Integer.parseInt(matcher.group(1));
						newStart = Integer.parseInt(matcher.group(3));
						line().add(deemphasize(line));
						colorizer.reset();
					} else if(line.startsWith("-")) {
						numbered(oldStart, -1, removed(colorizer.colorizeLine(line)));
						++oldStart;
					} else if(line.startsWith("+")) {
						numbered(-1, newStart, added(colorizer.colorizeLine(line)));
						++newStart;
					} else if(!skipDiff) {
						numbered(oldStart, newStart, plain(colorizer.colorizeLine(line)));
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

		protected Strip numbered(int oldStart, int newStart, Block block) {
			return line()
					.add(lineNo(oldStart))
					.add(gap(2))
					.add(lineNo(newStart))
					.add(block);
		}

		private HBox lineNo(int lineNo) {
			return align(code(lineNo == -1 ? "" : lineNo).size(10), 30).right().background(Colors.LINE_NO_BG);
		}


		private Text warning(String value) {
			return text(value).color(Colors.WARNING);
		}

		private Strip plain(Strip block) {
			return block;
		}

		private Strip added(Strip strip) {
			return strip.background(Colors.LINE_ADDED_BG);
		}

		private Strip removed(Strip strip) {
			return strip.background(Colors.REMOVED_BG);
		}

		private Text deemphasize(String line) {
			return code(line).color(Colors.DE_EMPHASIZE);
		}

		private void addFileHeader(int lineCount, String file, Block lineStart) {
			final String label = lineCount == 1 ? "Files:" : "";
			lines.add(fileIndex++, strip()
					.add(
							align(TextStyle.LABEL.applyTo(text(label)), 100).right(),
							TextStyle.LINK.applyTo(text(file)).linkTo(lineStart))
			);
		}

		private void addDiff(final T data, final Predicate<String> status) {
			try {
				colorize(richTextView.loadDiff(data), status);
			} catch (IOException | InterruptedException | RuntimeException e) {
				e.printStackTrace();
			}
		}
	}

	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@.*");
}

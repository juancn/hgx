import codng.hgx.ui.rtext.Block;
import codng.hgx.ui.rtext.HBox;
import codng.hgx.ui.rtext.RichTextView;
import codng.hgx.ui.rtext.RichTextViewModel;
import codng.hgx.ui.rtext.Strip;
import codng.hgx.ui.rtext.Text;
/**
 * RichTextView that knows how to colorize a git-style diff.
 * This component delay loads the diff and processes the coloring in a background thread.
 * @param <T> type of diff source data.
 */
					final DiffModel<T> diffModel = createModel();
		final DiffModel<T> diffModel = createModel();
	protected DiffModel<T> createModel() {
		return new DiffModel<>(this);
	boolean interrupted() {
	public static class DiffModel<T> extends RichTextViewModel<DiffViewer<T>> {
		public DiffModel(DiffViewer<T> richTextView) {
			super(richTextView);
		}

				for(String line = br.readLine(); line != null && !richTextView.interrupted(); line = br.readLine())  {
							final Strip fileLine = line().add(align(text(file).vgap(10).bold(), richTextView.getParent().getWidth() - 50).background(Colors.FILE_BG));
				colorize(richTextView.loadDiff(data), status);
package codng.hgx.ui;

abstract class Colorizer {
	protected final RowViewer rowViewer;

	protected Colorizer(RowViewer rowViewer) {
		this.rowViewer = rowViewer;
	}

	public abstract RichTextView.Strip colorizeLine(String line);
	
	public static final Colorizer plain(RowViewer rowViewer) {
		return new Colorizer(rowViewer) {
			@Override
			public RichTextView.Strip colorizeLine(final String line) {
				final RichTextView.Strip strip = rowViewer.strip();
				String chopped = line;
				
				if(chopped.length() > MAX_LINE_LENGTH) {
					chopped = chopped.substring(0, MAX_LINE_LENGTH);
					strip.add(rowViewer.text("(truncated)").bold().rgb(255, 0, 0));
				}

				while(chopped.length() > TEXT_BLOCK_CUTOFF) {
					strip.add(rowViewer.code(chopped.substring(0, TEXT_BLOCK_CUTOFF)).hgap(0));
					chopped = chopped.substring(TEXT_BLOCK_CUTOFF);
				}
				strip.add(rowViewer.code(chopped).hgap(0));
				return strip;
			}
		};
	}

	public void reset() {}

	/** Lines over this length will be truncated */
	private static final int MAX_LINE_LENGTH = 10000;
	/** Every TEXT_BLOCK_CUTOFF a new Text block will be created to allow for faster rendering */
	private static final int TEXT_BLOCK_CUTOFF = 200;
}

package codng.hgx.ui;

import codng.hgx.ui.RichTextView.Model;

abstract class Colorizer {
	protected final Model model;

	protected Colorizer(final Model model) {
		this.model = model;
	}

	public abstract RichTextView.Strip colorizeLine(String line);
	
	public static final Colorizer plain(Model model) {
		return new Colorizer(model) {
			@Override
			public RichTextView.Strip colorizeLine(final String line) {
				final RichTextView.Strip strip = model.strip();
				String chopped = line;
				
				if(chopped.length() > MAX_LINE_LENGTH) {
					chopped = chopped.substring(0, MAX_LINE_LENGTH);
					strip.add(model.text("(truncated)").bold().color(Colors.WARNING));
				}

				while(chopped.length() > TEXT_BLOCK_CUTOFF) {
					strip.add(model.code(chopped.substring(0, TEXT_BLOCK_CUTOFF)).hgap(0));
					chopped = chopped.substring(TEXT_BLOCK_CUTOFF);
				}
				strip.add(model.code(chopped).hgap(0));
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

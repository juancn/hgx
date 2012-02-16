import codng.util.DefaultPredicate;
import codng.util.Predicate;
import codng.util.StopWatch;
			final Text loading = text("Loading...").rgb(200, 200, 200).bold().size(14);
			line().add(loading);
					addDiff(this.row, new DefaultPredicate<String>() {
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
	private void addDiff(final Row row, final Predicate<String> status) {
			colorize(Cache.loadDiff(row), status);
	private void colorize(BufferedReader br, final Predicate<String> status) {
			int lineCount = 0;
				++lineCount;
				if(lineCount > 0 && lineCount % 1000 == 0) {
					status.apply(String.format("Loading... (syntax highlighting, %s lines processed)", lineCount));
				}
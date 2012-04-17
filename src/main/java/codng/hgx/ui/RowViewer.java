package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.ChangeSet;
import codng.hgx.Row;

import java.io.BufferedReader;
import java.io.IOException;

public class RowViewer
		extends DiffViewer<Row> {
	public RowViewer() {
		this(null);
	}

	public RowViewer(Row row) {
		super(row);
	}

	@Override
	protected RowModel createModel() {
		return new RowModel(this);
	}

	@Override
	protected BufferedReader loadDiff(Row data) throws IOException, InterruptedException {
		return Cache.loadDiff(data);
	}

	private static class RowModel extends DiffModel<Row> {

		public RowModel(DiffViewer<Row> richTextView) {
			super(richTextView);
		}

		protected void addHeader(final Row row) {
			final ChangeSet changeSet = row.changeSet;
			header("SHA:", changeSet.id);
			header("Author:", changeSet.user);
			header("Date:", changeSet.date);
			header("Summary:", text(changeSet.summary).bold());
			header("Parents:", changeSet.parents());
			header("Branch:", text(changeSet.branch).bold());
			if (!changeSet.tags().isEmpty()) header("Tags:", text(changeSet.tags()).bold());
			super.addHeader(row);
		}
	}
}

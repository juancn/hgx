package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.ChangeSet;
import codng.hgx.Id;
import codng.hgx.Row;
import codng.hgx.ui.rtext.Block;
import codng.hgx.ui.rtext.Link;
import codng.util.DefaultFunction;

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

	private class RowModel extends DiffModel<Row> {

		public RowModel(DiffViewer<Row> richTextView) {
			super(richTextView);
		}

		protected void addHeader(final Row row) {
			final ChangeSet changeSet = row.changeSet;
			header("SHA:", changeSet.id);
			header("Author:", changeSet.user);
			header("Date:", changeSet.date);
			header("Summary:", text(changeSet.summary).bold());
			header("Parents:", strip().add(changeSet.parents().map(idLink())));
			header("Branch:", text(changeSet.branch).bold());
			if (!changeSet.tags().isEmpty()) header("Tags:", text(changeSet.tags()).bold());
			super.addHeader(row);
		}

		private DefaultFunction<Id, Block> idLink() {
			return new DefaultFunction<Id, Block>() {
				@Override
				public Block apply(final Id id) {
					return new Link(TextStyle.LINK.applyTo(text(id)), null) {
						@Override
						public void onClick() {
							RowViewer.this.onClick(id);
						}
					};
				}
			};
		}

	}

	protected void onClick(Id id) {
	}
}

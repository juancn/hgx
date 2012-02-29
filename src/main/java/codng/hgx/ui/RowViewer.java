package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.ChangeSet;
import codng.hgx.Row;
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

public class RowViewer
		extends DiffViewer<Row> {
	public RowViewer() {
		this(null);
	}

	public RowViewer(Row row) {
		super(row);
	}

	@Override
	protected DiffModel createModel() {
		return new RowModel();
	}

	@Override
	protected BufferedReader loadDiff(Row data) throws IOException, InterruptedException {
		return Cache.loadDiff(data);
	}

	private class RowModel extends DiffModel {
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

	// Javac bug
	public void access$300() {}
}

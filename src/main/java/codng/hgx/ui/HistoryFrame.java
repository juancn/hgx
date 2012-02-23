package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.ChangeSet;
import codng.hgx.Hg;
import codng.hgx.Row;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class HistoryFrame 
		extends JFrame 
{
	private static final double GOLDEN = 1.61803399;
	private JTable historyTable;
	private JSplitPane split;
	private RowViewer detail;

	public HistoryFrame(String branch, Iterator<Row> historyGen) throws HeadlessException {
		super(branch);

		final HistoryTableModel historyTableModel = new HistoryTableModel(historyGen);
		historyTable = new JTable(historyTableModel);
		historyTable.getColumnModel().getColumn(0).setCellRenderer(new RowRenderer());
		historyTable.setRowMargin(0);
		historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tableScrollPane = new JScrollPane(historyTable);


		detail = new RowViewer();
		
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(tableScrollPane);
		final JScrollPane detailScrollPane = new JScrollPane(detail);
		split.setBottomComponent(detailScrollPane);

		getContentPane().add(split, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				savePrefs();
				System.exit(0);
			}
		});
		
		historyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && historyTable.getSelectedRow() < historyTableModel.getRowCount()) {
					detail.setRow((Row) historyTableModel.getValueAt(historyTable.getSelectedRow(), 0));
					detail.scrollRectToVisible(new Rectangle());
				}
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				savePrefs();
				// Just die
				System.exit(0);
			}
		});
	}

	private void doShow() throws InvocationTargetException, InterruptedException {
		loadPrefs();
		setSize(getUserSize());
		final Point location = getUserLocation();
		if (location.x >= 0) { setLocation(location); }
		setVisible(true);
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if (location.x < 0) {
					setLocationRelativeTo(null);
				}
				historyTable.getColumnModel().getColumn(0).setPreferredWidth(900);
				split.setDividerLocation(1 - (1 / GOLDEN));
				detail.setPreferredSize(getSize());
				// Select the first row
				historyTable.getSelectionModel().setSelectionInterval(0, 0);
			}
		});
	}

	private void loadPrefs() {
		try {
			PREFERENCES.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private void savePrefs() {
		final Point location = getLocation();
		PREFERENCES.putInt("location.x", location.x);
		PREFERENCES.putInt("location.y", location.y);
		final Dimension size = getSize();
		PREFERENCES.putInt("size.width", size.width);
		PREFERENCES.putInt("size.height", size.height);
		try {
			PREFERENCES.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private Dimension getUserSize() {
		final Dimension size = new Dimension();
		size.width = PREFERENCES.getInt("size.width", (int) (900* GOLDEN));
		size.height = PREFERENCES.getInt("size.height", 900);
		return size;
	}

	private Point getUserLocation() {
		final Point location = new Point();
		location.x = PREFERENCES.getInt("location.x", -1);
		location.y = PREFERENCES.getInt("location.y", -1);
		return location;
	}

	static class CommandLine {
		private final String[] args;
		private int next;
		private List<String> arguments = new ArrayList<>();
		private String branch;
		private boolean branchOnly;

		CommandLine(String[] args) {
			this.args = args;
		}

		public List<String> getArguments() {
			return arguments;
		}

		public boolean isBranchOnly() {
			return branchOnly;
		}

		public String getBranch() {

			return branch;
		}

		private String la() {
			return next < args.length ? args[next] : null;
		}
		
		private void consume() {
			++next;
		}
		
		CommandLine parse() {
			while (la() != null) {
				if(la().equals("--"))  { consume(); break; }
				else if(la().charAt(0) == '-') parseOption();
				else { consumeArg(); }
			}
			while (la() != null) {
				consumeArg();
			}
			return this;
		}

		private void parseOption() {
			switch (la()) {
				case "-b": case "--branch":
					consume();
					branch = la();
					consume();
					break;
				case "-B": case "--branch-only":
					consume();
					branchOnly = true;
					break;
				case "-t": case "--trunk":
					consume();
					branchOnly = true;
					branch = "trunk";
					break;
				default:
					consumeArg();
					break;
			}
		}

		private void consumeArg() {
			arguments.add(la());
			consume();
		}
	}

	public static void main(String[] args) throws Exception {
		final CommandLine cmdLine = new CommandLine(args).parse();
		
		if(!cmdLine.getArguments().isEmpty()) {
			usage();
			System.exit(1);
		}

		final String branch = cmdLine.getBranch() == null ? Hg.branch() : cmdLine.getBranch();
		final List<ChangeSet> changeSets = ChangeSet.filterBranch(branch, ChangeSet.loadFromCurrentDirectory(), cmdLine.isBranchOnly());
		final HistoryFrame historyFrame = new HistoryFrame(branch, Row.fromChangeSets(changeSets).iterator());
		historyFrame.doShow();
	}

	private static void usage() {
		try {
			final OutputStreamWriter out = new OutputStreamWriter(System.out);
			Cache.transfer(new InputStreamReader(HistoryFrame.class.getResourceAsStream("help.txt"), "UTF-8"), out);
			out.flush();
		} catch (IOException e) {
			// Shouldn't happen but log just in case
			e.printStackTrace();
		}
	}

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(HistoryFrame.class);
}

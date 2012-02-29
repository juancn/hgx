package codng.hgx.ui;

import codng.util.Command;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class DiffFrame
		extends JFrame
{
	private static final double GOLDEN = 1.61803399;
	private DiffViewer<String> detail;
	private final String diff;

	public DiffFrame(final String branch, final String diff) throws HeadlessException {
		super(branch);
		this.diff = diff;

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				savePrefs();
				// Just die
				System.exit(0);
			}
		});

		detail = new DiffViewer<String>() {
			@Override
			protected BufferedReader loadDiff(String data) throws IOException, InterruptedException {
				return new BufferedReader(new StringReader(data));
			}

			// Javac bug
			public void access$300() {}
		};
		getContentPane().add(new JScrollPane(detail));
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
				detail.setPreferredSize(getSize());
				detail.setData(diff);
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

	public static void main(String[] args) throws Exception {
		final ArrayList<String> arguments = new ArrayList<>();
		arguments.add("diff");
		arguments.add("--git");
		arguments.addAll(Arrays.asList(args));
		final DiffFrame historyFrame = new DiffFrame("", Command.executeSimple(new File("."), "hg", arguments));
		historyFrame.doShow();
	}

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(DiffFrame.class);
}

package codng.hgx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Cache {
	private static final File CACHE_DIR = new File(System.getProperty("user.home"), ".hgx");

	public static String loadDiff(Row row) throws IOException {
		final String key = row.changeSet.parents.get(0).hash + "-" + row.changeSet.id.hash;
		final File file = new File(CACHE_DIR, key);
		final String diff;
		if(file.exists()) {
			diff = readString(file);
		} else {
			diff = Command.executeSimple("hg", "diff", "--git","-r", row.changeSet.parents.get(0).hash, "-r", row.changeSet.id.hash);
			writeString(diff, file);
		}
		return diff;
	}
	
	public static List<ChangeSet> loadHistory(String id) {
		final File file = new File(CACHE_DIR, id + ".history");
		if(file.exists()) {
			try {
				return cast(readObject(file));
			} catch (IOException|ClassNotFoundException e) {
				file.delete();
				e.printStackTrace();
			}
		} 
		return new ArrayList<>();
	}

	public static void saveHistory(String id, List<ChangeSet> history) {
		final File file = new File(CACHE_DIR, id + ".history");
		
		File temp = null;
		try {
			temp = File.createTempFile("diff", "tmp", CACHE_DIR);
			writeObject(history, temp);
		} catch (IOException e) {
			if(temp != null) temp.delete();
			e.printStackTrace();
		}
		if(temp != null) temp.renameTo(file);
	}

	@SuppressWarnings("unchecked")
	private static <X> X cast(Object o) {
		return (X)o;
	}

	public static void writeString(String diff, File file) throws IOException {
		final File parentFile = file.getParentFile();
		if(!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		final File temp = File.createTempFile("diff", "tmp", parentFile);
		final FileOutputStream fos = new FileOutputStream(temp);
		final OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(fos), "UTF-8");
		out.write(diff);
		out.close();
		temp.renameTo(file);
	}

	public static void writeObject(Object obj, File file) throws IOException {
		final File parentFile = file.getParentFile();
		if(!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		final File temp = File.createTempFile("diff", "tmp", parentFile);
		final FileOutputStream fos = new FileOutputStream(temp);
		final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos));
		out.writeObject(obj);
		out.close();
		temp.renameTo(file);
	}

	public static String readString(File file) throws IOException {
		final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		final Reader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		final StringWriter out = new StringWriter();
		final char[] buffer = new char[512];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		in.close();
		return out.toString();
	}

	public static Object readObject(File file) throws IOException, ClassNotFoundException {
		final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		final ObjectInputStream in = new ObjectInputStream(inputStream);
		final Object result = in.readObject();
		in.close();
		return result;
	}
}

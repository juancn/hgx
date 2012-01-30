package codng.hgx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Cache {
	private static final File CACHE_DIR = new File(System.getProperty("user.home"), ".hgx");

	public static BufferedReader loadDiff(Row row) throws IOException, InterruptedException {
		final String key = row.changeSet.parents.get(0).hash + "-" + row.changeSet.id.hash;
		final File file = new File(CACHE_DIR, key);
		if(!file.exists()) {
			final Hg.AsyncCommand command = Hg.diff(row.changeSet.parents.get(0).hash, row.changeSet.id.hash);
			writeText(command.getOutput(), file);
			try {
				command.getExitCode().call();
			} catch (Exception e) {
				throw new Error(e);
			}
		}
		return readText(file);
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

	public static void writeText(InputStream inputStream, File file) throws IOException {
		final File parentFile = file.getParentFile();
		if(!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		final File temp = File.createTempFile("diff", "tmp", parentFile);
		final FileOutputStream fos = new FileOutputStream(temp);
		final OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(fos), "UTF-8");
		transfer(new InputStreamReader(inputStream), out);
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

	public static BufferedReader readText(File file) throws IOException {
		final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	}

	public static void transfer(Reader in, Writer out) throws IOException {
		final char[] buffer = new char[512];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	public static Object readObject(File file) throws IOException, ClassNotFoundException {
		final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		final ObjectInputStream in = new ObjectInputStream(inputStream);
		final Object result = in.readObject();
		in.close();
		return result;
	}
}

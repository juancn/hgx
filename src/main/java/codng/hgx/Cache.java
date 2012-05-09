package codng.hgx;

import codng.util.Cast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Cache {
	private static final File CACHE_DIR = new File(System.getProperty("user.home"), ".hgx");

	public static BufferedReader loadDiff(Row row) throws IOException, InterruptedException {
		final File file = ensureCached(row);
		return readText(file);
	}
	
	private static File repositoryRoot() {
		final File cwd = new File(System.getProperty("user.dir"));
		for(File f = cwd; f != null; f = f.getParentFile()) {
			if(new File(f, ".hg").isDirectory()) {
				return f;
			}
		}
		return null;
	}
	
	public static String repositoryId() {
		try {
			final MessageDigest md5 = MessageDigest.getInstance("MD5");
			final byte[] digest = md5.digest(repositoryRoot().getCanonicalFile().getPath().getBytes("UTF-8"));
			final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(digest));
			return Long.toString(dis.readLong() & 0x7FFF_FFFF_FFFF_FFFFL, 36);
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new Error(e);
		}
	}

	public static File ensureCached(Row row) throws IOException, InterruptedException {
		final File file = new File(CACHE_DIR, row.changeSet.id.hash + ".diff");
		if(!file.exists()) {
			final Hg.AsyncCommand command = Hg.diff(row.changeSet.id.hash);
			try {
				writeText(command.getOutput(), file);
			} finally {
				try {
					command.getExitCode().call();
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		}
		return file;
	}

	public static List<ChangeSet> loadHistory(String id) {
		final File file = new File(CACHE_DIR, id + ".history");
		if(file.exists()) {
			try {
				return Cast.force(readObject(file));
			} catch (IOException|ClassNotFoundException e) {
				file.delete();
				saveLastRevision(id, 0);
				e.printStackTrace();
				System.exit(-1);
			}
		} 
		return new ArrayList<>();
	}

	public static void saveHistory(String id, List<ChangeSet> history) {
		final File file = new File(CACHE_DIR, id + ".history");
		file.getParentFile().mkdirs(); // Ensure parent exists
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

	public static void saveLastRevision(String id, int revision) {
		final File file = new File(CACHE_DIR, id + ".last");

		File temp = null;
		try {
			temp = File.createTempFile("diff", "tmp", CACHE_DIR);
			writeObject(revision, temp);
		} catch (IOException e) {
			if(temp != null) temp.delete();
			e.printStackTrace();
		}
		if(temp != null) temp.renameTo(file);
	}

	public static int loadLastRevision(String id) {
		final File file = new File(CACHE_DIR, id + ".last");
		if(file.exists()) {
			try {
				return Cast.force(readObject(file));
			} catch (IOException|ClassNotFoundException e) {
				file.delete();
				e.printStackTrace();
			}
		}
		return 0;
	}

	public static void writeText(InputStream inputStream, File file) throws IOException {
		final File parentFile = file.getParentFile();
		if(!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		final File temp = File.createTempFile("diff", "tmp", parentFile);
		final FileOutputStream fos = new FileOutputStream(temp);
		final OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(fos), "UTF-8");
		transfer(new InputStreamReader(inputStream, "UTF-8"), out);
		out.close();
		inputStream.close();
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

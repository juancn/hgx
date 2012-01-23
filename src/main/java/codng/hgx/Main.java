package codng.hgx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

public class Main {

	public static void main(String[] args) throws Exception {
		final List<ChangeSet> changeSets = loadFromCurrentDirectory();
		final File dotFile = File.createTempFile("log", "dot");
		final File pdfFile = File.createTempFile("log", "pdf");
		dotify(changeSets, 1000, dotFile.getAbsolutePath(), null);
		
		Command.executeSimple("dot", "-Tpdf", dotFile.getAbsolutePath(), "-o", pdfFile.getAbsolutePath());
		dotFile.delete();
		Command.executeSimple("open", "-W", pdfFile.getAbsolutePath());
		pdfFile.delete();
	}

	private static List<ChangeSet> loadFromCurrentDirectory() throws Exception {
		final String branch = Command.executeSimple("hg", "branch").trim();
		final PipedInputStream snk = new PipedInputStream() {
			@Override
			public int read() throws IOException {
				try {
					return super.read();
				} catch (IOException e) {
					if(e.getMessage().equals("Write end dead")) {
						return -1;
					}
					throw e;
				}
			}
		};
		
		Callable<Integer> exitCode = new Command("hg", "log", "--branch", branch)
				.redirectError(System.err)
				.redirectOutput(new PipedOutputStream(snk))
				.start();

		final List<ChangeSet> changeSets = ChangeSet.loadFrom(snk);
		System.out.println("exitCode.call() = " + exitCode.call());
		return changeSets;
	}

	private static void dotify(List<ChangeSet> changeSets, int count, String filename, String branch) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
		pw.println("digraph history {");
		pw.println("node [shape=box];");
		for (ChangeSet changeSet : changeSets) {
			if(branch == null || changeSet.branch.equals(branch)) {
				pw.printf("%s [label=\"%s\\n (%s) %s\"];\n", changeSet.id.seqNo, changeSet.id, changeSet.user, chop(changeSet.summary));
				for (Id parent : changeSet.parents) {
					pw.printf("%s -> %s;\n", changeSet.id.seqNo, parent.seqNo);				
				}
				if(--count == 0) {
					break;
				}
			}
		}
		pw.println("}");
		pw.close();
	}

	private static String chop(String summary) {
		final String escaped = summary.replace("\"", "\\\"");
		final int width = 30;
		if(escaped.length() > width) {
			final String[] split = escaped.split(" ");
			StringBuilder sb = new StringBuilder();
			int last = 0;
			for (String s : split) {
				sb.append(s).append(' ');
				if(sb.length() / width > last) {
					last = sb.length() / width;
					sb.append("\\n");
				}
			}
			return sb.toString();
		}
		return escaped;
	}

}

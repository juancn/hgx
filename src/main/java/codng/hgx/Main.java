package codng.hgx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;

public class Main {

	public static void main(String[] args) throws Exception {
		final List<ChangeSet> changeSets = ChangeSet.loadFromCurrentDirectory();
		final File dotFile = File.createTempFile("log", "dot");
		final File pdfFile = File.createTempFile("log", "pdf");
		dotify(changeSets, 1000, dotFile.getAbsolutePath(), null);
		
		Command.executeSimple("dot", "-Tpdf", dotFile.getAbsolutePath(), "-o", pdfFile.getAbsolutePath());
		dotFile.delete();
		Command.executeSimple("open", "-W", pdfFile.getAbsolutePath());
		pdfFile.delete();
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

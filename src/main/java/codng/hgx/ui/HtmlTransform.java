package codng.hgx.ui;

import codng.hgx.ui.RichTextView.Block;
import codng.hgx.ui.RichTextView.BlockVisitor;
import codng.hgx.ui.RichTextView.Gap;
import codng.hgx.ui.RichTextView.HBox;
import codng.hgx.ui.RichTextView.HRuler;
import codng.hgx.ui.RichTextView.Link;
import codng.hgx.ui.RichTextView.Strip;
import codng.hgx.ui.RichTextView.Text;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

public class HtmlTransform
		implements BlockVisitor
{
	private final StringWriter sw = new StringWriter();
	private final PrintWriter pw = new PrintWriter(sw);
	private boolean monospaced;

	public HtmlTransform() {
		pw.println("<html><body>");
	}

	public void visitLine(Strip strip) {
		visit(strip);
		pw.println("<br/>");
	}

	@Override
	public void visit(Text text) {
		if(text.isMonospaced() && !monospaced) {
			monospaced = true;
			pw.print("<pre>");
		}
		if(monospaced && !text.isMonospaced()) {
			monospaced = false;
			pw.print("</pre>");
		}
		if(text.isBold()) pw.print("<bold>");
		if(text.isItalic()) pw.print("<italic>");
		pw.printf("<font color=\"%s\">%s</font>", rgb(text.color(false)), text.text());
		if(text.isItalic()) pw.print("</italic>");
		if(text.isBold()) pw.print("</bold>");
	}

	private String rgb(Color color) {
		return String.format("rgb(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue());
	}

	@Override
	public void visit(Gap gap) {

	}

	@Override
	public void visit(Strip strip) {
		for (Block block : strip.blocks()) {
			block.visit(this);
		}
	}

	@Override
	public void visit(HBox hBox) {
		hBox.block.visit(this);
		pw.print(" ");
	}

	@Override
	public void visit(Link link) {
		link.block.visit(this);
	}

	@Override
	public void visit(HRuler hRuler) {
		pw.println("<hr/>");
	}

	@Override
	public String toString() {
		pw.flush();
		return sw.toString() + "</body></html>";
	}
}

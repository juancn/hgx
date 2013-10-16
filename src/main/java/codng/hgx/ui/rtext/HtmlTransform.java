package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

public class HtmlTransform
		implements BlockVisitor
{
	private final StringWriter sw = new StringWriter();
	private final PrintWriter pw = new PrintWriter(sw);

	public HtmlTransform() {
		pw.println("<html><body>");
	}

	public void visitLine(Strip strip) {
		visit(strip);
		pw.println("<br/>");
	}

	@Override
	public void visit(Text text) {
		pw.print("<span style=\"");
		if(!text.getForegroundColor().equals(Color.BLACK)) pw.printf("color:%s;", rgb(text.getForegroundColor()));
		if(!text.getBackgroundColor(false).equals(Color.WHITE)) pw.printf("background-color:%s;", rgb(text.getBackgroundColor(false)));
		if(text.isItalic()) pw.print("font-style:italic;");
		if(text.isBold()) pw.print("font-weight:bold;");
		if(text.isMonospaced()) pw.print("font-family:monaco,courier;");
		if(text.hgap() != 0) pw.printf("margin-left:%spx;margin-right:%spx;", text.hgap()/2, text.hgap()/2);
		pw.printf("font-size:%d;", text.size());
		pw.printf("\">%s</span>", htmlEscape(text.text()));
	}

	private String htmlEscape(String text) {
		// Puaj!
		return text
				.replace(" ", "&nbsp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private String rgb(Color color) {
		return String.format("rgb(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue());
	}

	@Override
	public void visit(Gap gap) {

	}

	@Override
	public void visit(Strip strip) {

		boolean hasBackground = !strip.getBackgroundColor(false).equals(Color.WHITE);
		if(hasBackground) pw.printf("<span style=\"background-color:%s;\">", rgb(strip.getBackgroundColor(false)));
		for (Block block : strip.blocks()) {
			block.visit(this);
		}
		if(hasBackground) pw.print("</span>");
	}

	@Override
	public void visit(HBox hBox) {
		pw.printf("<div style=\"display:inline-block; width:%.0fpx; text-align:%s; background-color:%s;\" >",
				hBox.width(), hBox.align().toString().toLowerCase(), rgb(hBox.getBackgroundColor(false)));
		hBox.block.visit(this);
		pw.print("</div>");
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

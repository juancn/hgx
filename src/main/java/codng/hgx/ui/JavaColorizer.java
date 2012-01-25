package codng.hgx.ui;

import java.text.ParseException;

public class JavaColorizer {
	
	
	public static String colorizeLine(String line) {
		final JavaLexer lexer = new JavaLexer(line, 0, line.length());
		StringBuilder sb = new StringBuilder();
		try {
			for(JavaToken last = null, current = lexer.next();
				current.getType() != TokenType.EOF;
				last = current, current = lexer.next()) {
				if(last != null) sb.append(HistoryFrame.htmlEscape(line.substring(last.getEndOffset(), current.getStartOffset())));
				switch (current.getType()) {
					case ML_COMMENT:
					case SL_COMMENT:
						sb.append("<span style=\"font-style=italic; color: rgb(128,128,128);\">"); break;
					case STRING:
					case CHAR_LITERAL:
						sb.append("<span style=\"font-weight:bold; color: rgb(0,128,0);\">"); break;
					case INT_LITERAL:
					case LONG_LITERAL:
					case FLOAT_LITERAL:
					case DOUBLE_LITERAL:
						sb.append("<span style=\"color: rgb(0,0,255);\">"); break;
				}
				final boolean reserved = JavaLexer.isReserved(current.getText());
				
				if(reserved) sb.append("<span style=\"font-weight:bold; color: rgb(0,0,128);\">"); 
				sb.append(HistoryFrame.htmlEscape(current.getText()));
				if(reserved) sb.append("</span>"); 
				
				switch (current.getType()) {
					case ML_COMMENT: 
					case SL_COMMENT:
					case STRING:
					case CHAR_LITERAL:
					case INT_LITERAL:
					case LONG_LITERAL:
					case FLOAT_LITERAL:
					case DOUBLE_LITERAL:
						sb.append("</span>"); break;
				}
			}
		} catch (ParseException e) {
			return line;
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println("colorizeLine() = " + colorizeLine("-\t\t\tassertEquals(String.format(\"Using unit '%s'\", pair.getKey()), udf.getOr(null), pair.getValue());"));
	}
}

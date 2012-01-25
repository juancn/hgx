package codng.hgx.ui;

import java.text.ParseException;

public class JavaColorizer extends Colorizer {
	
	private boolean unterminatedComment;
	
	@Override
	public String colorizeLine(String line) {
		StringBuilder sb = new StringBuilder();
		try {
			
			if(unterminatedComment) {
				final int terminator = line.indexOf("*/");
				if(terminator == -1) {
					sb.append("<span style=\"font-style: italic; color: rgb(128,128,128);\">");
					sb.append(htmlEscape(line));
					sb.append("</span>");
					return sb.toString();
				} else {
					sb.append("<span style=\"font-style: italic; color: rgb(128,128,128);\">");
					sb.append(htmlEscape(line.substring(0, terminator+2)));
					sb.append("</span>");
					line = line.substring(terminator+2);
					unterminatedComment = false;
				}
			}

			final JavaLexer lexer = new JavaLexer(line, 0, line.length());
			for(JavaToken last = null, current = lexer.next();
				current.getType() != TokenType.EOF;
				last = current, current = lexer.next()) {
				if (last == null) {
					sb.append(htmlEscape(line.substring(0, current.getStartOffset())));
				} else {
					sb.append(htmlEscape(line.substring(last.getEndOffset(), current.getStartOffset())));
				}
				switch (current.getType()) {
					case ML_COMMENT:
						if(!current.getText().toString().endsWith("*/")) unterminatedComment = true;
					case SL_COMMENT:
						sb.append("<span style=\"font-style: italic; color: rgb(128,128,128);\">"); break;
					case STRING:
					case CHAR_LITERAL:
						sb.append("<span style=\"font-weight: bold; color: rgb(0,128,0);\">"); break;
					case INT_LITERAL:
					case LONG_LITERAL:
					case FLOAT_LITERAL:
					case DOUBLE_LITERAL:
						sb.append("<span style=\"color: rgb(0,0,255);\">"); break;
				}
				final boolean reserved = JavaLexer.isReserved(current.getText());
				
				if(reserved) sb.append("<span style=\"font-weight: bold; color: rgb(0,0,128);\">"); 
				sb.append(htmlEscape(current.getText()));
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
			e.printStackTrace();
			return htmlEscape(line);
		}
		return sb.toString();
	}
}

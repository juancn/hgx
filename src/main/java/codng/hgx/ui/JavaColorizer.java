package codng.hgx.ui;

import java.text.ParseException;

class JavaColorizer extends Colorizer {
	
	private boolean unterminatedComment;
	JavaColorizer(RowViewer rowViewer) {
		super(rowViewer);
	}

	@Override
	public void reset() {
		unterminatedComment = false;
	}

	@Override
	public RowViewer.Strip colorizeLine(String line) {
		final RowViewer.Strip strip = strip();
		try {
			if(unterminatedComment) {
				final int terminator = line.indexOf("*/");
				if(terminator == -1) {
					strip.add(text(line).rgb(128, 128, 128).italic());
					return strip;
				} else {
					strip.add(text(line.substring(0, terminator + 2)).rgb(128, 128, 128).italic());
					line = line.substring(terminator+2);
					unterminatedComment = false;
				}
			}

			final JavaLexer lexer = new JavaLexer(line, 0, line.length());
			for(JavaToken last = null, current = lexer.next();
				current.getType() != TokenType.EOF;
				last = current, current = lexer.next()) {
				if (last == null) {
					strip.add(text(line.substring(0, current.getStartOffset())));
				} else {
					strip.add(text(line.substring(last.getEndOffset(), current.getStartOffset())));
				}
				
				final RowViewer.Text token = text(current.getText());
				strip.add(token);

				switch (current.getType()) {
					case ML_COMMENT:
						if(!current.getText().toString().endsWith("*/")) unterminatedComment = true;
					case SL_COMMENT:
						token.rgb(128,128,128).italic(); break;
					case STRING:
					case CHAR_LITERAL:
						token.rgb(0,128,0).bold(); break;
					case ANNOTATION:
						token.rgb(128,128,0); break;
					case INT_LITERAL:
					case LONG_LITERAL:
					case FLOAT_LITERAL:
					case DOUBLE_LITERAL:
						token.rgb(0, 0, 255); break;
				}
				final boolean reserved = JavaLexer.isReserved(current.getText());
				
				if(reserved) token.rgb(0, 0, 128);;

			}
		} catch (ParseException e) {
			synchronized (System.err) {
				System.err.println("Line: " + line);
				e.printStackTrace();
			}
			return strip.add(text(line));
		}
		return strip;
	}

	private RowViewer.Text text(CharSequence text) {
		return rowViewer.code(text.toString()).hgap(0);
	}

	private RowViewer.Strip strip() {
		return rowViewer.strip();
	}
}

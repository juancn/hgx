package codng.hgx.ui;

import codng.hgx.ui.RichTextView.Model;
import codng.hgx.ui.RichTextView.Text;

import java.text.ParseException;

class JavaColorizer extends Colorizer {

	private boolean unterminatedComment;
	JavaColorizer(final Model model) {
		super(model);
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
					strip.add(comment(line));
					return strip;
				} else {
					strip.add(comment(line.substring(0, terminator + 2)));
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

				final RowViewer.Text token;
				final CharSequence text = current.getText();
				final TokenType tokenType = current.getType();

				if (tokenType == TokenType.ML_COMMENT) {
					if (!text.toString().endsWith("*/")) unterminatedComment = true;
					token = comment(text);
				} else if (tokenType == TokenType.SL_COMMENT) {
					token = comment(text);
				} else if (tokenType == TokenType.STRING || tokenType == TokenType.CHAR_LITERAL) {
					token = string(text);
				} else if (tokenType == TokenType.ANNOTATION) {
					token = annotation(text);
				} else if (tokenType == TokenType.INT_LITERAL
						|| tokenType == TokenType.LONG_LITERAL
						|| tokenType == TokenType.FLOAT_LITERAL
						|| tokenType == TokenType.DOUBLE_LITERAL) {
					token = number(text);
				} else if (JavaLexer.isReserved(text)) {
					token = keyword(text);
				}else {
					token = plain(text);
				}
				strip.add(token);
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

	private Text plain(CharSequence text) {
		return text(text);
	}

	private Text keyword(CharSequence text) {
		return text(text).color(Colors.KEYWORD);
	}

	private Text number(CharSequence text) {
		return text(text).color(Colors.NUMBER);
	}

	private Text annotation(CharSequence text) {
		return text(text).color(Colors.ANNOTATION);
	}

	private Text string(CharSequence text) {
		return text(text).color(Colors.STRING_LITERAL).bold();
	}

	private Text comment(CharSequence line) {
		return text(line).color(Colors.COMMENT).italic();
	}

	private RowViewer.Text text(CharSequence text) {
		return model.code(text.toString()).hgap(0);
	}

	private RowViewer.Strip strip() {
		return model.strip();
	}
}

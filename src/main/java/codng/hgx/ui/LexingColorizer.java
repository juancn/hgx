package codng.hgx.ui;

import codng.hgx.ui.rtext.RichTextViewModel;
import codng.hgx.ui.rtext.Strip;
import codng.hgx.ui.rtext.Text;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

abstract class LexingColorizer extends Colorizer {

	private boolean unterminatedComment;
	LexingColorizer(final RichTextViewModel model) {
		super(model);
	}

	@Override
	public void reset() {
		unterminatedComment = false;
	}

	@Override
	public Strip colorizeLine(String line) {
		final Strip strip = strip();
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
					strip.add(plain(line.substring(0, current.getStartOffset())));
				} else {
					strip.add(plain(line.substring(last.getEndOffset(), current.getStartOffset())));
				}

				final Text token;
				final CharSequence text = current.getText();
				final TokenType tokenType = current.getType();

				if (tokenType == TokenType.ML_COMMENT) {
					if (!text.toString().endsWith("*/")) unterminatedComment = true;
					token = comment(text);
				} else if (tokenType == TokenType.SL_COMMENT) {
					token = comment(text);
				} else if (tokenType == TokenType.STRING || tokenType == TokenType.CHAR_LITERAL || tokenType == TokenType.AT_STRING) {
					token = string(text);
				} else if (tokenType == TokenType.ANNOTATION) {
					token = annotation(text);
				} else if (tokenType == TokenType.DIRECTIVE) {
					token = directive(text);
				} else if (tokenType == TokenType.INT_LITERAL
						|| tokenType == TokenType.LONG_LITERAL
						|| tokenType == TokenType.FLOAT_LITERAL
						|| tokenType == TokenType.DOUBLE_LITERAL) {
					token = number(text);
				} else if (isReserved(text)) {
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
			return strip.add(plain(line));
		}
		return strip;
	}

	private Text plain(CharSequence text) {
		return TextStyle.CODE.applyTo(gapless(text));
	}

	private Text keyword(CharSequence text) {
		return TextStyle.KEYWORD.applyTo(gapless(text));
	}

	private Text number(CharSequence text) {
		return TextStyle.NUMBER.applyTo(gapless(text));
	}

	private Text annotation(CharSequence text) {
		return TextStyle.ANNOTATION.applyTo(gapless(text));
	}

	private Text directive(CharSequence text) {
		return TextStyle.DIRECTIVE.applyTo(gapless(text));
	}

	private Text string(CharSequence text) {
		return TextStyle.STRING_LITERAL.applyTo(gapless(text));
	}

	private Text comment(CharSequence line) {
		return TextStyle.COMMENT.applyTo(gapless(line));
	}

	private Text gapless(CharSequence text) {
		return model.text(text.toString()).hgap(0);
	}

	private Strip strip() {
		return model.strip();
	}

	public abstract boolean isReserved(CharSequence word);
}

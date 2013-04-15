package codng.hgx.ui;

import codng.hgx.ui.rtext.RichTextViewModel;
import codng.hgx.ui.rtext.Text;
import codng.hgx.ui.rtext.Strip;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class LexingColorizer extends Colorizer {

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
				} else if (tokenType == TokenType.STRING || tokenType == TokenType.CHAR_LITERAL) {
					token = string(text);
				} else if (tokenType == TokenType.ANNOTATION) {
					token = annotation(text);
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

	public boolean isReserved(CharSequence word) {
		return RESERVED.contains(word);
	}

	private static Set<CharSequence> RESERVED = new TreeSet<>(new CharSequenceComparator<CharSequence>());

	private static void reserved(CharSequence id)
	{
		RESERVED.add(id);
	}

	static {
		reserved("null");
		reserved("true");
		reserved("false");
		reserved("abstract");
		reserved("assert");
		reserved("boolean");
		reserved("break");
		reserved("byte");
		reserved("case");
		reserved("catch");
		reserved("char");
		reserved("class");
		reserved("const");
		reserved("continue");
		reserved("default");
		reserved("do");
		reserved("double");
		reserved("else");
		reserved("extends");
		reserved("final");
		reserved("finally");
		reserved("float");
		reserved("for");
		reserved("goto");
		reserved("if");
		reserved("implements");
		reserved("import");
		reserved("instanceof");
		reserved("int");
		reserved("interface");
		reserved("long");
		reserved("native");
		reserved("new");
		reserved("package");
		reserved("private");
		reserved("protected");
		reserved("public");
		reserved("return");
		reserved("retry");
		reserved("short");
		reserved("static");
		reserved("strictfp");
		reserved("super");
		reserved("switch");
		reserved("synchronized");
		reserved("this");
		reserved("throw");
		reserved("throws");
		reserved("transient");
		reserved("try");
		reserved("void");
		reserved("volatile");
		reserved("while");
	}

	private static class CharSequenceComparator<T extends CharSequence>
			implements Comparator<T>
	{

		public int compare(T l, T r)
		{
			int llen = l.length();
			int rlen = r.length();
			int n = Math.min(llen, rlen);
			for(int i = 0; i < n; i++) {
				char c1 = l.charAt(i);
				char c2 = r.charAt(i);
				if (c1 != c2) {
					return c1 - c2;
				}
			}
			return llen - rlen;
		}
	}
}

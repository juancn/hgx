package codng.hgx.ui;

import codng.hgx.ui.rtext.RichTextViewModel;
import codng.util.CharSequenceComparator;

import java.util.Set;
import java.util.TreeSet;

class JavaColorizer extends LexingColorizer {

	JavaColorizer(final RichTextViewModel model) {
		super(model);
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
}

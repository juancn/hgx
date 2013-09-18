package codng.hgx.ui;

import codng.hgx.ui.rtext.RichTextViewModel;
import codng.util.CharSequenceComparator;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

class CLikeColorizer extends LexingColorizer {

	private static final Pattern RESERVED_LIKE = Pattern.compile("NS\\w+");

	CLikeColorizer(final RichTextViewModel model) {
		super(model);
	}

	public boolean isReserved(CharSequence word) {
		return RESERVED.contains(word) || RESERVED_LIKE.matcher(word).matches();
	}

	private static Set<CharSequence> RESERVED = new TreeSet<>(new CharSequenceComparator<CharSequence>());

	private static void reserved(CharSequence id)
	{
		RESERVED.add(id);
	}

	static {
		reserved("alignas");
		reserved("alignof");
		reserved("and");
		reserved("and_eq");
		reserved("asm");
		reserved("auto");
		reserved("bitand");
		reserved("bitor");
		reserved("bool");
		reserved("break");
		reserved("case");
		reserved("catch");
		reserved("char");
		reserved("char16_t");
		reserved("char32_t");
		reserved("class");
		reserved("compl");
		reserved("const");
		reserved("constexpr");
		reserved("const_cast");
		reserved("continue");
		reserved("decltype");
		reserved("default");
		reserved("delete");
		reserved("do");
		reserved("double");
		reserved("dynamic_cast");
		reserved("else");
		reserved("enum");
		reserved("explicit");
		reserved("export");
		reserved("extern");
		reserved("false");
		reserved("float");
		reserved("for");
		reserved("friend");
		reserved("goto");
		reserved("if");
		reserved("inline");
		reserved("int");
		reserved("long");
		reserved("mutable");
		reserved("namespace");
		reserved("new");
		reserved("noexcept");
		reserved("not");
		reserved("not_eq");
		reserved("nullptr");
		reserved("operator");
		reserved("or");
		reserved("or_eq");
		reserved("private");
		reserved("protected");
		reserved("public");
		reserved("register");
		reserved("reinterpret_cast");
		reserved("return");
		reserved("short");
		reserved("signed");
		reserved("sizeof");
		reserved("static");
		reserved("static_assert");
		reserved("static_cast");
		reserved("struct");
		reserved("switch");
		reserved("template");
		reserved("this");
		reserved("thread_local");
		reserved("throw");
		reserved("true");
		reserved("try");
		reserved("typedef");
		reserved("typeid");
		reserved("typename");
		reserved("union");
		reserved("unsigned");
		reserved("using");
		reserved("virtual");
		reserved("void");
		reserved("volatile");
		reserved("wchar_t");
		reserved("while");
		reserved("xor");
		reserved("xor_eq");
		// Other
		reserved("BOOL");

		// Thrift
		reserved("throws");
		reserved("required");
		reserved("optional");
		reserved("service");
		reserved("exception");
		reserved("string");
		reserved("byte");
		reserved("i16");
		reserved("i32");
		reserved("i64");
		reserved("binary");
		reserved("slist");
		reserved("list");
		reserved("cpp_type");
		reserved("include");
		reserved("set");
		reserved("map");
		reserved("php_namespace");
		reserved("xsd_namespace");
		reserved("senum");
		reserved("xsd_all");
	}
}

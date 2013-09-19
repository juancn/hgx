package codng.hgx.ui;

import codng.hgx.ui.rtext.RichTextViewModel;
import codng.util.CharSequenceComparator;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

class ThriftColorizer extends CLikeColorizer {

	ThriftColorizer(final RichTextViewModel model) {
		super(model);
	}

	{
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

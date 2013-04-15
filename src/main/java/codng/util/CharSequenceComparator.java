package codng.util;

import java.util.Comparator;


/** Comparator that orders {@link CharSequence}s in lexicographical order */
public class CharSequenceComparator<T extends CharSequence>
		implements Comparator<T>
{
	@Override
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

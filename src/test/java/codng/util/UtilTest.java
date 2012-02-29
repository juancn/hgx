package codng.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static codng.util.Sequences.asSequence;
import static codng.util.Sequences.reverse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class UtilTest {
	@Test
	public void sequence() {
		final Sequence<Integer> integers = asSequence(1, 2, 3, 4, 5);
		assertEquals("[1, 2, 3, 4, 5]", integers.toList().toString());
		assertEquals("[1, 2, 3, 4, 5]", integers.toString());
		assertEquals("[1, 2, 3]", integers.limit(3).toString());
		assertEquals("[2, 4]", integers.filter(EVEN).toList().toString());
		assertEquals("[1, 3, 5]", integers.filter(EVEN.not()).toList().toString());

		assertEquals("[(1), (2), (3), (4), (5)]", integers.map(TO_STRING).toList().toString());
		assertEquals("[(2), (4)]", integers.filter(EVEN).map(TO_STRING).toList().toString());
		assertEquals("[(1), (3), (5)]", integers.filter(EVEN.not()).map(TO_STRING).toList().toString());

		assertEquals("[0, 0, 1, 0, 1, 2, 0, 1, 2, 3, 0, 1, 2, 3, 4]", integers.flatMap(TO_LIST).toList().toString());
		assertEquals("[0, 0, 0, 2, 0, 2, 0, 2, 4]", integers.flatMap(TO_LIST).filter(EVEN).toList().toString());
		assertEquals("[1, 1, 1, 3, 1, 3]", integers.flatMap(TO_LIST).filter(EVEN.not()).toList().toString());
		assertTrue(integers.toSet().containsAll(integers.toList()));
		assertEquals("[5, 4, 3, 2, 1]", reverse(Arrays.asList(1,2,3,4,5)).toString());
	}

	@Test
	public void range() {
		assertEquals("[1, 2, 3, 4, 5]", Range.closed(1, 5).toList().toString());
		assertEquals("[5, 4, 3, 2, 1]", Range.closed(5, 1).toList().toString());
	}

	@Test
	public void predicate() {
		final Predicate<Object> t = Predicates.alwaysTrue();
		final Predicate<Object> f = Predicates.alwaysFalse();

		assertTrue(t.apply(null));
		assertFalse(f.apply(null));

		assertTrue(t.or(t).apply(null));
		assertTrue(t.or(f).apply(null));
		assertTrue(f.or(t).apply(null));
		assertFalse(f.or(f).apply(null));

		assertTrue(t.and(t).apply(null));
		assertFalse(t.and(f).apply(null));
		assertFalse(f.and(t).apply(null));
		assertFalse(f.and(f).apply(null));

		assertTrue(f.not().apply(null));
		assertFalse(t.not().apply(null));

		assertFalse(Predicates.notNull().apply(null));
		assertTrue(Predicates.notNull().apply(""));
	}

	@Test
	public void function() {
		assertEquals("yay", Functions.identity().apply("yay"));
		assertEquals("(42)", TO_STRING.compose(Functions.<String>identity()).apply(42));
	}


	@Test
	public void tuple() {
		final Tuple a = Tuple.make(1, 2);
		final Tuple a1 = Tuple.make(1, 2);
		final Tuple3 b = Tuple.make(1, 2, 3);
		final Tuple3 b1 = Tuple.make(1, 2, 3);
		final Tuple4 c = Tuple.make(1, 2, 3, 4);
		final Tuple4 c1 = Tuple.make(1, 2, 3, 4);

		assertNotSame(a, a1);
		assertNotSame(b, b1);
		assertNotSame(c, c1);

		assertTrue(a.hashCode() != b.hashCode());
		assertTrue(b.hashCode() != c.hashCode());
		assertTrue(c.hashCode() != a.hashCode());

		assertEquals(a, a1);
		assertEquals(b, b1);
		assertEquals(c, c1);

		assertEquals(a.hashCode(), a1.hashCode());
		assertEquals(b.hashCode(), b1.hashCode());
		assertEquals(c.hashCode(), c1.hashCode());

		assertEquals("(1, 2)", a.toString());
		assertEquals("(1, 2, 3)", b.toString());
		assertEquals("(1, 2, 3, 4)", c.toString());
	}

	private static final Predicate<Integer> EVEN = new DefaultPredicate<Integer>() {
		@Override
		public boolean apply(Integer integer) {
			return integer % 2 == 0;
		}
	};

	private static final DefaultFunction<Integer,String> TO_STRING = new DefaultFunction<Integer, String>() {
		@Override
		public String apply(Integer integer) {
			return String.format("(%s)", integer);
		}
	};

	private static final DefaultFunction<Integer,Iterator<Integer>> TO_LIST = new DefaultFunction<Integer, Iterator<Integer>>() {
		@Override
		public Iterator<Integer> apply(Integer integer) {
			return Range.closed(0, integer-1).iterator();
		}
	};
}
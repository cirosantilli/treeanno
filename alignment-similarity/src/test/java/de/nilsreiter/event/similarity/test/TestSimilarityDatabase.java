package de.nilsreiter.event.similarity.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.nilsreiter.event.similarity.Null;
import de.nilsreiter.event.similarity.Random;
import de.nilsreiter.event.similarity.impl.SimilarityDatabase_impl;
import de.nilsreiter.util.db.DatabaseConfiguration;
import de.nilsreiter.util.db.impl.DatabaseDBConfiguration_impl;
import de.uniheidelberg.cl.a10.data2.Document;
import de.uniheidelberg.cl.a10.data2.Event;

public class TestSimilarityDatabase {

	Event[] events;
	Document[] documents;
	SimilarityDatabase_impl<Event> sd;

	@Before
	public void setUp() throws Exception {
		documents = new Document[2];
		for (int i = 0; i < 2; i++) {
			documents[i] = mock(Document.class);
			when(documents[i].getId()).thenReturn("doc" + i);
		}

		events = new Event[10];
		for (int i = 0; i < 10; i++) {
			events[i] = mock(Event.class);
			when(events[i].getId()).thenReturn(String.valueOf(i));
			when(events[i].getRitualDocument()).thenReturn(documents[i % 2]);
		}
		DatabaseConfiguration dbConf =
				DatabaseConfiguration.getDefaultConfiguration();
		dbConf.setPrefix("test");
		sd =
				new SimilarityDatabase_impl<Event>(new DatabaseDBConfiguration_impl(
						dbConf));

	}

	@After
	public void tearDown() throws Exception {
		sd.getDatabase().getConnection().close();
	}

	@Test
	public void test() throws ClassNotFoundException, SQLException {
		sd.rebuild();
		sd.putSimilarity(Null.class, events[0], events[1], 0.1);
		sd.putSimilarity(Random.class, events[1], events[1], 0.2);
		sd.putSimilarity(Null.class, events[2], events[1], 0.3);
		sd.putSimilarity(Null.class, events[3], events[1], 0.4);
		sd.putSimilarity(Null.class, events[4], events[0], 0.5);
		sd.putSimilarity(Null.class, events[5], events[2], 0.6);

		assertEquals(0.1, sd.getSimilarity(Null.class, events[0], events[1]),
				0.0);
		assertEquals(0.2, sd.getSimilarity(Random.class, events[1], events[1]),
				0.0);
		assertEquals(0.3, sd.getSimilarity(Null.class, events[2], events[1]),
				0.0);
		assertEquals(0.4, sd.getSimilarity(Null.class, events[3], events[1]),
				0.0);
		assertEquals(0.5, sd.getSimilarity(Null.class, events[4], events[0]),
				0.0);
		assertEquals(0.6, sd.getSimilarity(Null.class, events[5], events[2]),
				0.0);

	}
}

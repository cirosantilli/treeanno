package de.nilsreiter.similarity;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.kohsuke.args4j.Option;

import de.nilsreiter.event.similarity.FrameNet;
import de.nilsreiter.event.similarity.Null;
import de.nilsreiter.event.similarity.WordNet;
import de.nilsreiter.util.db.DatabaseConfiguration;
import de.uniheidelberg.cl.a10.data2.Document;
import de.uniheidelberg.cl.a10.data2.Event;
import de.uniheidelberg.cl.a10.main.MainWithInputDocuments;
import de.uniheidelberg.cl.a10.patterns.data.Probability;
import de.uniheidelberg.cl.a10.patterns.similarity.IncompatibleException;
import de.uniheidelberg.cl.a10.patterns.similarity.SimilarityDatabase;
import de.uniheidelberg.cl.a10.patterns.similarity.SimilarityFunction;

/**
 * This class is used to calculate similarities for all pairs of events across
 * two documents. We assume documents to be eventized by some preprocessing
 * step.
 * 
 * @author reiterns
 * 
 */
public class CalculateSimilarities extends MainWithInputDocuments {

	static enum Measure {
		WN, FN, GD, AT, VN
	}

	@Option(name = "--measure", usage = "The similarity measure to use")
	Measure measure = Measure.WN;

	@Option(name = "--init", usage = "Re-initialize the database, deleting everything")
	boolean rinit = false;

	SimilarityFunction<Event> function = null;
	SimilarityDatabase<Event> database = null;

	public static void main(String[] args) throws IOException, SQLException {
		CalculateSimilarities cs = new CalculateSimilarities();
		cs.processArguments(args);
		cs.init();
		cs.run();
	}

	protected void init() throws SQLException, IOException {
		try {
			database = new SimilarityDatabase<Event>(
					DatabaseConfiguration.getDefaultConfiguration());
			if (rinit)
				database.rebuild();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		switch (measure) {
		case WN:
			WordNet wns = new WordNet(new File(getConfiguration().getString(
					"paths.wnhome")), new File(getConfiguration().getString(
					"paths.nombank")));

			function = wns;
			break;
		case FN:
			FrameNet fns = new FrameNet(new File(getConfiguration().getString(
					"paths.fnhome")));
			function = fns;
			break;
		default:
			function = new Null();
		}
	}

	protected void runDocuments(Document d1, Document d2) throws SQLException {
		for (Event event1 : d1.getEvents()) {
			for (Event event2 : d2.getEvents()) {
				try {
					Probability p = function.sim(event1, event2);
					database.putSimilarity(function.getClass(), event1, event2,
							p.getProbability());
				} catch (IncompatibleException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void run() throws IOException, SQLException {
		List<Document> docs = this.getDocuments();
		for (int i = 0; i < docs.size(); i++) {
			for (int j = i + 1; j < docs.size(); j++) {
				this.runDocuments(docs.get(i), docs.get(j));
			}
		}
	}
}
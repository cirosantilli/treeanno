package de.ustu.ims.reiter.treeanno.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;

public class CsvGenerator<T> implements Generator<Map<T, Map<T, T>>> {

	Map<T, Map<T, T>> matrix;
	Function<T, String> idFunction;
	Comparator<T> comparator;

	public CsvGenerator(Function<T, String> idFunction, Comparator<T> comparator) {
		super();
		this.idFunction = idFunction;
		this.comparator = comparator;
	}

	@Override
	public void setInput(Map<T, Map<T, T>> input) {
		matrix = input;
	}

	@Override
	public InputStream generate() throws IOException {
		SortedSet<T> keys = new TreeSet<T>(this.comparator);
		keys.addAll(matrix.keySet());
		for (Map<T, T> row : matrix.values()) {
			keys.addAll(row.keySet());
		}
		StringBuilder b = new StringBuilder();
		CSVPrinter p = new CSVPrinter(b, CSVFormat.EXCEL);
		boolean headDone = false;
		for (T row : keys) {
			if (!headDone) {
				p.print("-");
				for (T col : keys) {
					p.print(idFunction.apply(col));
				}
				p.println();
				headDone = true;
			}
			p.print(idFunction.apply(row));
			for (T col : keys) {
				if (matrix.containsKey(row) && matrix.get(row).containsKey(col)) {
					p.print(idFunction.apply(matrix.get(row).get(col)));
				} else {
					p.print("-");
				}
			}
			p.println();
		}
		p.close();
		return IOUtils.toInputStream(b.toString());
	}

	@Override
	public String getSuffix() {
		return "csv";
	}

}

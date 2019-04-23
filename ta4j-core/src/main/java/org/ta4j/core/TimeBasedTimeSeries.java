package org.ta4j.core;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Function;

import org.ta4j.core.num.Num;

/**
 * 
 * 
 * 
 * Just as suggestion. Not tested!
 * 
 * @author Kilian
 */
public class TimeBasedTimeSeries extends BaseTimeSeries implements TimeIndexedTimeSeries {

	private static final long serialVersionUID = 7426071765632014847L;
	/**
	 * Mapping of bar end time to the bar index
	 */
	private NavigableMap<ZonedDateTime, Integer> timestampIndex;

	private int barIndex = 0;
	
	public TimeBasedTimeSeries(String name, List<Bar> bars, int seriesBeginIndex, int seriesEndIndex,
			boolean constrained, Function<Number, Num> numFunction) {

		super(name, bars, seriesBeginIndex, seriesEndIndex, constrained, numFunction);

		this.timestampIndex = new TreeMap<>();

		// Update the timesamp index
		for (barIndex = seriesBeginIndex; barIndex < bars.size(); barIndex++) {
			timestampIndex.put(bars.get(barIndex).getEndTime(), barIndex);
		}
	}

	@Override
	public void addBar(Bar bar, boolean replace) {
		super.addBar(bar,replace);
		if(!replace) {
			timestampIndex.put(bar.getEndTime(),barIndex++);
		}
	}

	@Override
	public int getIndex(ZonedDateTime time) {

		// Find the closest end time to our query
		Entry<ZonedDateTime, Integer> entry = timestampIndex.ceilingEntry(time);

		if (entry != null) {
			// We still need to check if the timestamp belongs to the bar.
			ZonedDateTime key = entry.getKey();
			ZonedDateTime beginOfBar = this.getBar(key).getBeginTime();
			if (time.isEqual(beginOfBar) || time.isAfter(beginOfBar)) {
				return internalIndexToIndex(entry.getValue());
			}
		}
		throw new NoSuchElementException("No bar covering " + time + " exists");
	}

	protected int internalIndexToIndex(int i) {
		//TODO we should change some stuff to protected
		int innerIndex = i - getRemovedBarsCount();
		if (innerIndex < 0) {
			if (i < 0) {
				// Cannot return the i-th bar if i < 0
				throw new IndexOutOfBoundsException();
			}
			if (this.isEmpty()) {
				throw new IndexOutOfBoundsException();
			}
			innerIndex = 0;
		} else if (innerIndex >= this.getBarData().size()) {
			// Cannot return the n-th bar if n >= bars.size()
			throw new IndexOutOfBoundsException();
		}
		return innerIndex;
	}

}

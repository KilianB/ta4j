package org.ta4j.core;

import java.time.ZonedDateTime;

/**
 * A time series which supports indexing via bar ending time.
 * 
 * @author Kilian
 *
 */
public interface TimeIndexedTimeSeries extends TimeSeries {

	/**
	 * @param time the time stamp to look up
	 * @return the bar which containing data for this time stamp
	 */
	default Bar getBar(ZonedDateTime time) {
		return getBar(getIndex(time));
	}

	/**
	 * @param time the time stamp to look up
	 * @return the index of the series containing data for this time stamp
	 */
	int getIndex(ZonedDateTime time);
}
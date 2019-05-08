package org.ta4j.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.ta4j.core.num.Num;

/**
 * Down samples a time series to a lower time frame granularity.
 * <p>
 * In other words this class may combine multiple 5 minute bars to a 15 minute
 * bar.
 * Currently trades are not transfered into the new series.
 * 
 * @author Kilian
 *
 */
public class TimeSeriesConverter {

	/**
	 * Convert a time series from a small duration to a time series with a higher
	 * duration.
	 * 
	 * @implnote This native implementation can handle gaps in data but assumes each
	 *           bar to have the same duration as the very first bar.
	 *           <p>
	 *           The created series are not linked. updates made to either series
	 *           are <b>not</b> in the other.
	 * 
	 * @param source         the time series the bars are read from
	 * @param targetDuration the duration of the newly created series. The duration
	 *                       has to be greater than the source duration and be
	 *                       divisible by it.
	 * @return the newly create time series
	 */
	public static TimeSeries convert(TimeSeries source, Duration targetDuration) {

		Bar meta = source.getFirstBar();
		Duration sourceDuration = meta.getTimePeriod();

		long sourceDurSec = sourceDuration.getSeconds();
		long targetDurSec = targetDuration.getSeconds();

		// We only downsample the duration -> 5 min to 15 min
		if (targetDurSec < sourceDurSec) {
			throw new IllegalArgumentException("Can not convert series to finer granular duration");
		}

		/*
		 * This condition is required to get correct! time series results.
		 * 
		 * There are a few possibilities to circumvent this issue: e.g. Metratrader
		 * samples ticks from the higher timeframe and "arbitrarily" assigns them to the
		 * new bars. These synthetic ticks somewhat resemble lower timeframes during
		 * backtest but when requiring high quality data the tradeoff isn't worth it in
		 * my opinion as we can not know where the high and low ticks actually occured.
		 */
		if (targetDurSec % sourceDurSec != 0) {
			throw new IllegalArgumentException("The target duraton must be a multiple of the source duration");
		}

		
		TimeSeries targetSeries = new BaseTimeSeriesBuilder().withName(source.getName()).build();
		ZonedDateTime endTimeOfFirstBar = meta.getBeginTime().plusSeconds(targetDurSec);

		// Build a bar
		BarBuilder compositeBar = new BarBuilder(endTimeOfFirstBar, targetDuration);

		for (Bar bar : source.getBarData()) {
			if (!compositeBar.belongsTo(bar)) {
				// This bar is finished add it to the new series
				targetSeries.addBar(compositeBar.buildBar());

				// Check the next valid end bar time

				long secondsUntilNextEnd = compositeBar.endTime.until(bar.getEndTime(), ChronoUnit.SECONDS);

				int durationMultiple = (int) Math.ceil((secondsUntilNextEnd / (double) targetDurSec));

				ZonedDateTime newEndTimestamp = compositeBar.endTime.plusSeconds(durationMultiple * targetDurSec);
				// We require a new bar

				compositeBar = new BarBuilder(newEndTimestamp, targetDuration);
				assert compositeBar.belongsTo(bar);
			}
			// Merge bars
			compositeBar.mergeBar(bar);
		}
		// Add the last bar
		targetSeries.addBar(compositeBar.buildBar());

		return targetSeries;
	}

	/**
	 * Utility method to aggregate bars
	 *  
	 * @author Kilian
	 */
	static class BarBuilder {

		/** Time period (e.g. 1 day, 15 min, etc.) of the bar */
		Duration timePeriod;
		/** End time of the bar */
		ZonedDateTime endTime;
		/** Open price of the period */
		Num openPrice = null;
		/** Close price of the period */
		Num closePrice = null;
		/** Max price of the period */
		Num maxPrice = null;
		/** Min price of the period */
		Num minPrice = null;
		/** Traded amount during the period */
		Num amount;
		/** Volume of the period */
		Num volume;
		/** Trade count */
		int trades = 0;

		public BarBuilder(ZonedDateTime endTime, Duration duration) {
			this.endTime = endTime;
			this.timePeriod = duration;
		}

		/**
		 * 
		 * @implNote: To ensure time integrity the function is expected to be called in
		 *            sequence. If a bar was merged no older data may be added.
		 * 
		 * @param bar the bar to merge
		 */
		public void mergeBar(Bar bar) {

			if (openPrice == null) {
				openPrice = bar.getOpenPrice();
			}

			if (maxPrice == null || bar.getHighPrice().isGreaterThan(maxPrice)) {
				maxPrice = bar.getHighPrice();
			}

			if (minPrice == null || bar.getLowPrice().isLessThan(minPrice)) {
				minPrice = bar.getLowPrice();
			}

			this.closePrice = bar.getClosePrice();

			amount = amount == null ? bar.getAmount() : amount.plus(bar.getAmount());

			volume = volume == null ? bar.getVolume() : volume.plus(bar.getVolume());

			trades += bar.getTrades();
		}

		public boolean belongsTo(Bar barToCheck) {
			return (endTime.isEqual(barToCheck.getEndTime()) || endTime.isAfter(barToCheck.getEndTime()));
		}

		public Bar buildBar() {
			// TODO we need to add trades. But "at the end" isn't reliable since these times
			// will differ. We need trade support
			// based on timestamps
			return new BaseBar(timePeriod, endTime, openPrice, maxPrice, minPrice, closePrice, volume, amount);
		}
	}

}

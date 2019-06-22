package org.ta4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.ta4j.core.mocks.MockBar;
import org.ta4j.core.mocks.MockTimeSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * @author Kilian
 *
 */
public class TimeSeriesConverterTest {

	private static Function<Number, Num> numFunction = (num) -> {
		return DoubleNum.valueOf(num);
	};

	private static ZonedDateTime curTime;

	@Test
	public void downSample() {
		List<Bar> bars = new ArrayList<Bar>();

		Duration oneMin = Duration.ofSeconds(60);
		curTime = ZonedDateTime.of(2019, 5, 13, 0, 0, 0, 0, ZoneId.of("UTC"));

		// open, close, high, low

		// 1 bar
		bars.add(mockBar(oneMin, curTime, 1, 2, 0.5, 1, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 1, 3, 1, 2, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 2, 4, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 3, 3, 2, 2, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 1, 1, 0));

		// 2 bar
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 10, 0, 4, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 4, 6, 2, 2, 0));

		TimeSeries series = new BaseTimeSeries(bars);
		TimeSeries fiveMinSeries = TimeSeriesConverter.convert(series, Duration.ofMinutes(5));

		// Bar count
		assertEquals(2, fiveMinSeries.getBarCount());

		Bar firstBar = fiveMinSeries.getBar(0);
		Bar secondBar = fiveMinSeries.getBar(1);

		assertEquals(1, firstBar.getOpenPrice().intValue());
		assertEquals(4, firstBar.getHighPrice().intValue());
		assertEquals(0, firstBar.getLowPrice().intValue());
		assertEquals(1, firstBar.getClosePrice().intValue());

		assertEquals(2, secondBar.getOpenPrice().intValue());
		assertEquals(10, secondBar.getHighPrice().intValue());
		assertEquals(0, secondBar.getLowPrice().intValue());
		assertEquals(2, secondBar.getClosePrice().intValue());
	}

	@Test
	public void downSampleWithStartDateBefore() {
		List<Bar> bars = new ArrayList<Bar>();
		Duration oneMin = Duration.ofSeconds(60);
		curTime = ZonedDateTime.of(2019, 5, 13, 1, 32, 0, 0, ZoneId.of("UTC"));

		// open, close, high, low

		// 1. bar starts at min 31 since we but since we fixed out bars to 0:00 the first composite
		//bar will only contain 4 bars.

		// :32
		bars.add(mockBar(oneMin, curTime, 1, 2, 0.5, 1, 0));
		// :33
		bars.add(mockBar(oneMin, updateTime(oneMin), 1, 3, 1, 2, 0));
		// :34
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 2, 4, 0));
		// :35
		bars.add(mockBar(oneMin, updateTime(oneMin), 3, 3, 2, 2, 0));

		// 2 bar
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 1, 1, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 10, 0, 4, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 4, 6, 2, 2, 0));

		TimeSeries series = new BaseTimeSeries(bars);
		TimeSeries fiveMinSeries = TimeSeriesConverter.convert(series, Duration.ofMinutes(5), LocalTime.of(0, 0));

		// Bar count
		assertEquals(2, fiveMinSeries.getBarCount());

		Bar firstBar = fiveMinSeries.getBar(0);
		Bar secondBar = fiveMinSeries.getBar(1);

		assertEquals(1, firstBar.getOpenPrice().intValue());
		assertEquals(4, firstBar.getHighPrice().intValue());
		assertEquals(0, firstBar.getLowPrice().intValue());
		assertEquals(2, firstBar.getClosePrice().intValue());

		assertEquals(2, secondBar.getOpenPrice().intValue());
		assertEquals(10, secondBar.getHighPrice().intValue());
		assertEquals(0, secondBar.getLowPrice().intValue());
		assertEquals(2, secondBar.getClosePrice().intValue());
	}
	
	@Test
	public void downSampleWithStartDateAfter() {
		List<Bar> bars = new ArrayList<Bar>();
		Duration oneMin = Duration.ofSeconds(60);
		curTime = ZonedDateTime.of(2019, 5, 13, 1, 32, 0, 0, ZoneId.of("UTC"));

		// open, close, high, low

		// 1. bar starts at min 31 since we but since we fixed out bars to 0:00 the first composite
		//bar will only contain 4 bars.

		// :32
		bars.add(mockBar(oneMin, curTime, 1, 2, 0.5, 1, 0));
		// :33
		bars.add(mockBar(oneMin, updateTime(oneMin), 1, 3, 1, 2, 0));
		// :34
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 2, 4, 0));
		// :35
		bars.add(mockBar(oneMin, updateTime(oneMin), 3, 3, 2, 2, 0));

		// 2 bar
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 4, 1, 1, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 2, 10, 0, 4, 0));
		bars.add(mockBar(oneMin, updateTime(oneMin), 4, 6, 2, 2, 0));

		TimeSeries series = new BaseTimeSeries(bars);
		TimeSeries fiveMinSeries = TimeSeriesConverter.convert(series, Duration.ofMinutes(5), LocalTime.of(2, 0));

		// Bar count
		assertEquals(2, fiveMinSeries.getBarCount());

		Bar firstBar = fiveMinSeries.getBar(0);
		Bar secondBar = fiveMinSeries.getBar(1);

		assertEquals(1, firstBar.getOpenPrice().intValue());
		assertEquals(4, firstBar.getHighPrice().intValue());
		assertEquals(0, firstBar.getLowPrice().intValue());
		assertEquals(2, firstBar.getClosePrice().intValue());

		assertEquals(2, secondBar.getOpenPrice().intValue());
		assertEquals(10, secondBar.getHighPrice().intValue());
		assertEquals(0, secondBar.getLowPrice().intValue());
		assertEquals(2, secondBar.getClosePrice().intValue());
	}


	private ZonedDateTime updateTime(Duration dur) {
		curTime = curTime.plusSeconds(dur.getSeconds());
		return curTime;
	}

	private static Bar mockBar(Duration duration, ZonedDateTime endTime, double open, double high, double low,
			double close, double vol) {
		return new BaseBar(duration, endTime, numFunction.apply(open), numFunction.apply(high), numFunction.apply(low),
				numFunction.apply(close), numFunction.apply(vol), numFunction.apply(0));
	}

}

package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import com.robindrew.common.date.Dates;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.InstrumentType;
import com.robindrew.trading.backtest.history.image.IImageCache;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.charts.PriceCandleCanvas;
import com.robindrew.trading.price.candle.format.PriceFormat;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSource;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceProviderManager;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceSet;
import com.robindrew.trading.price.candle.format.pcf.source.PcfSourcesStreamSource;
import com.robindrew.trading.price.candle.format.pcf.source.file.IPcfFileManager;
import com.robindrew.trading.price.candle.format.ptf.source.IPtfSourceProviderManager;
import com.robindrew.trading.price.candle.format.ptf.source.IPtfSourceSet;
import com.robindrew.trading.price.candle.format.ptf.source.file.IPtfFileManager;
import com.robindrew.trading.price.candle.interval.PriceIntervals;
import com.robindrew.trading.price.candle.io.stream.source.IPriceCandleStreamSource;
import com.robindrew.trading.provider.TradingProvider;

public class PricesPage extends AbstractServicePage {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private static final int DEFAULT_WIDTH = 1100;
	private static final int DEFAULT_HEIGHT = 600;
	private static final String DEFAULT_DATE = "2017-01-01";
	private static final String DEFAULT_TIME = "00:00";
	private static final String DEFAULT_INTERVAL = "1 MINUTES";
	private static final int DEFAULT_CANDLE_LIMIT = 230;

	private static final List<Interval> INTERVALS = new ArrayList<>();

	static {
		INTERVALS.add(new Interval(1, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(2, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(3, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(5, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(10, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(15, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(30, TimeUnit.MINUTES));
		INTERVALS.add(new Interval(1, TimeUnit.HOURS));
		INTERVALS.add(new Interval(2, TimeUnit.HOURS));
		INTERVALS.add(new Interval(3, TimeUnit.HOURS));
		INTERVALS.add(new Interval(4, TimeUnit.HOURS));
		INTERVALS.add(new Interval(1, TimeUnit.DAYS));
		INTERVALS.add(new Interval(7, TimeUnit.DAYS));
	}

	public PricesPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		PriceFormat format = request.getEnum("format", PriceFormat.class);
		TradingProvider provider = request.getEnum("provider", TradingProvider.class);
		String instrument = request.get("instrument");
		InstrumentType type = request.getEnum("type", InstrumentType.class);

		String fromDate = request.get("fromDate", null);
		String fromTime = request.get("fromTime", null);
		String interval = request.get("interval", null);

		int candleLimit = request.getInt("candleLimit", DEFAULT_CANDLE_LIMIT);
		int width = request.getInt("width", DEFAULT_WIDTH);
		int height = request.getInt("height", DEFAULT_HEIGHT);

		if (fromDate == null) {
			fromDate = request.getValue("fromDate", DEFAULT_DATE);
		}
		if (fromTime == null) {
			fromTime = request.getValue("fromTime", DEFAULT_TIME);
		}
		if (interval == null) {
			interval = request.getValue("interval", DEFAULT_INTERVAL);
		}
		request.setValue("fromDate", fromDate);
		request.setValue("fromTime", fromTime);
		request.setValue("interval", interval);

		LocalDateTime from = parseDateTime(fromDate, fromTime);
		dataMap.put("fromDate", DATE_FORMATTER.format(from));
		dataMap.put("fromTime", TIME_FORMATTER.format(from));

		if (format.equals(PriceFormat.PCF)) {
			getPcfPrices(dataMap, provider, instrument, from, interval, width, height, candleLimit);
		}
		if (format.equals(PriceFormat.PTF)) {
			getPtfPrices(dataMap, provider, instrument, from, interval, width, height, candleLimit);
		}

		dataMap.put("format", format);
		dataMap.put("provider", provider);
		dataMap.put("instrument", instrument);
		dataMap.put("type", type);
		dataMap.put("intervals", INTERVALS);

		dataMap.put("interval", interval);
		dataMap.put("width", width);
		dataMap.put("height", height);
		dataMap.put("candleLimit", candleLimit);
	}

	private void getPcfPrices(Map<String, Object> dataMap, TradingProvider provider, String instrumentName, LocalDateTime from, String interval, int width, int height, int candleLimit) {
		IPcfFileManager manager = getDependency(IPcfFileManager.class);
		IPcfSourceProviderManager pcf = manager.getProvider(provider);
		IInstrument instrument = pcf.getInstrument(instrumentName);
		IPcfSourceSet sourceSet = pcf.getSourceSet(instrument);
		SortedSet<LocalDate> months = sourceSet.getMonths();

		LocalDate firstMonth = months.first();
		LocalDate lastMonth = months.last();

		LocalDateTime to = from.plus(1, ChronoUnit.MONTHS);

		Set<? extends IPcfSource> sources = sourceSet.getSources(from, to);
		IPriceCandleStreamSource source = new PcfSourcesStreamSource(sources);

		List<IPriceCandle> candles = drainToList(source, from, candleLimit);
		if (!candles.isEmpty()) {
			PriceCandleCanvas canvas = new PriceCandleCanvas(width, height);
			canvas.renderCandles(candles, PriceIntervals.MINUTELY);
			byte[] image = canvas.toPng();

			IImageCache cache = getDependency(IImageCache.class);
			long imageId = cache.put(image);
			dataMap.put("imageId", imageId);
		}

		dataMap.put("rangeFrom", Dates.formatDate("yyyy-MM", firstMonth));
		dataMap.put("rangeTo", Dates.formatDate("yyyy-MM", lastMonth));
		dataMap.put("candles", candles);

		if (!candles.isEmpty()) {
			IPriceCandle first = candles.get(0);
			IPriceCandle last = candles.get(candles.size() - 1);
			dataMap.put("candleFrom", first);
			dataMap.put("candleTo", last);
			dataMap.put("toDate", DATE_FORMATTER.format(last.getCloseDate()));
			dataMap.put("toTime", TIME_FORMATTER.format(last.getCloseDate()));
		}
	}

	private List<IPriceCandle> drainToList(IPriceCandleStreamSource source, LocalDateTime from, int candleLimit) {

		List<IPriceCandle> list = new ArrayList<>();
		while (true) {
			IPriceCandle candle = source.getNextCandle();
			if (candle == null) {
				break;
			}
			if (candle.getOpenDate().isBefore(from)) {
				continue;
			}
			list.add(candle);
			if (list.size() >= candleLimit) {
				break;
			}
		}
		return list;
	}

	private LocalDateTime getPtfPrices(Map<String, Object> dataMap, TradingProvider provider, String instrumentName, LocalDateTime from, String interval, int width, int height, int candleLimit) {
		IPtfFileManager manager = getDependency(IPtfFileManager.class);
		IPtfSourceProviderManager ptf = manager.getProvider(provider);
		IInstrument instrument = ptf.getInstrument(instrumentName);
		IPtfSourceSet sourceSet = ptf.getSourceSet(instrument);
		SortedSet<LocalDate> days = sourceSet.getDays();

		LocalDate firstDay = days.first();
		LocalDate lastDay = days.last();

		dataMap.put("rangeFrom", Dates.formatDate("yyyy-MM", firstDay));
		dataMap.put("rangeTo", Dates.formatDate("yyyy-MM", lastDay));

		return null;
	}

	private LocalDateTime parseDateTime(String date, String time) {
		LocalTime localTime = LocalTime.parse(time);
		LocalDate localDate = LocalDate.parse(date);
		return LocalDateTime.of(localDate, localTime);
	}

	public static Interval parseInterval(String text) {
		int index = text.indexOf(' ');
		int amount = Integer.parseInt(text.substring(0, index));
		TimeUnit unit = TimeUnit.valueOf(text.substring(index + 1));
		return new Interval(amount, unit);
	}

	public static final class Interval {

		private int amount;
		private TimeUnit unit;

		public Interval(int amount, TimeUnit unit) {
			this.amount = amount;
			this.unit = unit;
		}

		public int getAmount() {
			return amount;
		}

		public TimeUnit getUnit() {
			return unit;
		}

		@Override
		public int hashCode() {
			return amount + unit.hashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (object == this) {
				return true;
			}
			if (object instanceof Interval) {
				Interval that = (Interval) object;
				return this.getAmount() == that.getAmount() && this.getUnit().equals(that.getUnit());
			}
			return false;
		}

		@Override
		public String toString() {
			return amount + " " + unit;
		}
	}
}

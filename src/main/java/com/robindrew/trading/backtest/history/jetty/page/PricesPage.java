package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;
import static com.robindrew.trading.backtest.history.jetty.page.price.PriceIntervalRange.parsePriceInterval;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.date.Dates;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.InstrumentType;
import com.robindrew.trading.backtest.history.image.IImageCache;
import com.robindrew.trading.backtest.history.jetty.page.price.PriceInterval;
import com.robindrew.trading.backtest.history.jetty.page.price.PriceIntervalRange;
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
import com.robindrew.trading.price.candle.io.stream.source.IPriceCandleStreamSource;
import com.robindrew.trading.price.candle.io.stream.source.PriceCandleIntervalStreamSource;
import com.robindrew.trading.provider.TradingProvider;

public class PricesPage extends AbstractServicePage {

	private static final Logger log = LoggerFactory.getLogger(PricesPage.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private static final int DEFAULT_WIDTH = 1100;
	private static final int DEFAULT_HEIGHT = 600;
	private static final String DEFAULT_DATE = "2017-01-01";
	private static final String DEFAULT_TIME = "00:00";
	private static final String DEFAULT_INTERVAL = "1 MINUTES";

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

		boolean next = request.getBoolean("next", false);
		boolean previous = request.getBoolean("previous", false);

		// Parse the from date
		LocalDateTime from = parseDateTime(fromDate, fromTime);

		// Parse the interval
		PriceInterval priceInterval = parsePriceInterval(interval);
		PriceIntervalRange range = new PriceIntervalRange(priceInterval, from);
		if (next) {
			range = range.next();
		}
		if (previous) {
			range = range.previous();
		}
		from = range.getDate();

		if (format.equals(PriceFormat.PCF)) {
			getPcfPrices(dataMap, provider, instrument, range, width, height);
		}
		if (format.equals(PriceFormat.PTF)) {
			getPtfPrices(dataMap, provider, instrument, range, width, height);
		}

		fromDate = DATE_FORMATTER.format(range.getDate());
		fromTime = TIME_FORMATTER.format(range.getDate());

		request.setValue("fromDate", fromDate);
		request.setValue("fromTime", fromTime);
		request.setValue("interval", interval);

		dataMap.put("format", format);
		dataMap.put("provider", provider);
		dataMap.put("instrument", instrument);
		dataMap.put("type", type);
		dataMap.put("intervals", PriceIntervalRange.INTERVALS);

		dataMap.put("fromDate", fromDate);
		dataMap.put("fromTime", fromTime);
		dataMap.put("interval", interval);
		dataMap.put("width", width);
		dataMap.put("height", height);
	}

	private void getPcfPrices(Map<String, Object> dataMap, TradingProvider provider, String instrumentName, PriceIntervalRange range, int width, int height) {
		IPcfFileManager manager = getDependency(IPcfFileManager.class);
		IPcfSourceProviderManager pcf = manager.getProvider(provider);
		IInstrument instrument = pcf.getInstrument(instrumentName);
		IPcfSourceSet sourceSet = pcf.getSourceSet(instrument);
		SortedSet<LocalDate> months = sourceSet.getMonths();

		LocalDate firstMonth = months.first();
		LocalDate lastMonth = months.last();

		PriceInterval interval = range.getInterval();
		LocalDateTime from = range.getDate();
		LocalDateTime to = range.getNextDate();

		Set<? extends IPcfSource> sources = sourceSet.getSources(from, to);
		IPriceCandleStreamSource source = new PcfSourcesStreamSource(sources);

		source = new PriceCandleIntervalStreamSource(source, interval.asPriceInterval());

		List<IPriceCandle> candles = drainToList(source, from, interval.getDisplayAmount());

		log.info("Instrument: {} / {} / {}", instrument.getName(), instrument.getType(), provider);
		log.info("Date Range: {} -> {}", from, to);
		log.info("Candles:    {} ({})", candles.size(), interval);

		if (!candles.isEmpty()) {
			PriceCandleCanvas canvas = new PriceCandleCanvas(width, height);
			canvas.renderCandles(candles, interval.asPriceInterval());
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

	private LocalDateTime getPtfPrices(Map<String, Object> dataMap, TradingProvider provider, String instrumentName, PriceIntervalRange range, int width, int height) {
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

}

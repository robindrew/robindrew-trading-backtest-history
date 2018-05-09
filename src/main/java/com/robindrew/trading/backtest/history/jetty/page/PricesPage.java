package com.robindrew.trading.backtest.history.jetty.page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;

public class PricesPage extends AbstractServicePage {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

		String type = request.get("type");
		String provider = request.get("provider");
		String instrument = request.get("instrument");
		String instrumentType = request.get("instrumentType");

		String fromDate = request.get("fromDate", null);
		String fromTime = request.get("fromTime", null);
		String interval = request.get("interval", null);

		if (fromDate == null) {
			fromDate = request.getValue("fromDate", "2017-01-01");
		}
		if (fromTime == null) {
			fromTime = request.getValue("fromTime", "00:00");
		}
		if (interval == null) {
			interval = request.getValue("interval", "1 MINUTES");
		}
		request.setValue("fromDate", fromDate);
		request.setValue("fromTime", fromTime);
		request.setValue("interval", interval);

		getPrices(type, provider, instrument, fromDate, fromTime, interval);

		LocalDateTime from = parseDateTime(fromDate, fromTime);

		dataMap.put("type", type);
		dataMap.put("provider", provider);
		dataMap.put("instrument", instrument);
		dataMap.put("instrumentType", instrumentType);
		dataMap.put("intervals", INTERVALS);

		dataMap.put("fromDate", DATE_FORMATTER.format(from));
		dataMap.put("fromTime", TIME_FORMATTER.format(from));

		dataMap.put("interval", interval);
	}

	private void getPrices(String type, String provider, String instrument, String fromDate, String fromTime, String interval) {

		// Interval intervalInstance = parseInterval(interval);

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

package com.robindrew.trading.backtest.history.jetty.page.price;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;

public class PriceIntervalRange {

	public static final List<PriceInterval> INTERVALS = getIntervalList();

	private static final List<PriceInterval> getIntervalList() {
		List<PriceInterval> list = new ArrayList<>();

		// Initialise intervals
		list.add(new PriceInterval(1, MINUTES, 180, 60));
		// intervals.add(new PriceInterval(2, MINUTES));
		// intervals.add(new PriceInterval(3, MINUTES));
		// intervals.add(new PriceInterval(5, MINUTES));
		// intervals.add(new PriceInterval(10, MINUTES));
		// intervals.add(new PriceInterval(15, MINUTES));
		// intervals.add(new PriceInterval(30, MINUTES));
		list.add(new PriceInterval(1, HOURS, 216, 24));
		// intervals.add(new PriceInterval(2, HOURS));
		// intervals.add(new PriceInterval(3, HOURS));
		// intervals.add(new PriceInterval(4, HOURS));
		list.add(new PriceInterval(1, DAYS, 210, 7));
		// intervals.add(new PriceInterval(7, DAYS));

		return ImmutableList.copyOf(list);
	}

	public static LocalDateTime normalize(LocalDateTime date, PriceInterval interval) {
		switch (interval.getUnit()) {
			case MINUTES:
				return normalizeMinutes(date);
			case HOURS:
				return normalizeHours(date);
			case DAYS:
				return normalizeDays(date);
			default:
				throw new IllegalArgumentException("interval=" + interval);
		}
	}

	private static LocalDateTime normalizeDays(LocalDateTime date) {
		LocalTime time = LocalTime.of(0, 0, 0, 0);
		return LocalDateTime.of(date.toLocalDate(), time);
	}

	private static LocalDateTime normalizeHours(LocalDateTime date) {
		int hour = date.toLocalTime().getHour();
		LocalTime time = LocalTime.of(hour, 0, 0, 0);
		return LocalDateTime.of(date.toLocalDate(), time);
	}

	private static LocalDateTime normalizeMinutes(LocalDateTime date) {
		int hour = date.toLocalTime().getHour();
		int minute = date.toLocalTime().getMinute();
		LocalTime time = LocalTime.of(hour, minute, 0, 0);
		return LocalDateTime.of(date.toLocalDate(), time);
	}

	public static PriceInterval parsePriceInterval(String text) {
		int space = text.indexOf(' ');
		int amount = Integer.parseInt(text.substring(0, space));
		TimeUnit unit = TimeUnit.valueOf(text.substring(space + 1));

		for (PriceInterval interval : INTERVALS) {
			if (interval.getAmount() == amount && interval.getUnit().equals(unit)) {
				return interval;
			}
		}
		throw new IllegalArgumentException("interval not found: " + text);
	}

	private final PriceInterval interval;
	private final LocalDateTime date;

	public PriceIntervalRange(PriceInterval interval, LocalDateTime date) {
		this.interval = interval;
		this.date = normalize(date, interval);
	}

	public PriceInterval getInterval() {
		return interval;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public LocalDateTime getNextDate() {
		LocalDateTime date = getDate();

		int move = interval.getMoveAmount();
		switch (interval.getUnit()) {
			case MINUTES:
				date = date.plusMinutes(move);
				break;
			case HOURS:
				date = date.plusHours(move);
				break;
			case DAYS:
				date = date.plusDays(move);
				break;
			default:
				throw new IllegalArgumentException("interval=" + interval);
		}

		return date;
	}

	public LocalDateTime getPreviousDate() {
		LocalDateTime date = getDate();

		int move = interval.getMoveAmount();
		switch (interval.getUnit()) {
			case MINUTES:
				date = date.minusMinutes(move);
				break;
			case HOURS:
				date = date.minusHours(move);
				break;
			case DAYS:
				date = date.minusDays(move);
				break;
			default:
				throw new IllegalArgumentException("interval=" + interval);
		}

		return date;
	}

	public PriceIntervalRange next() {
		return new PriceIntervalRange(interval, getNextDate());
	}

	public PriceIntervalRange previous() {
		return new PriceIntervalRange(interval, getPreviousDate());
	}

}

package com.robindrew.trading.backtest.history.jetty.page.price;

import java.util.concurrent.TimeUnit;

import com.robindrew.trading.price.candle.interval.IPriceInterval;
import com.robindrew.trading.price.candle.interval.TimeUnitInterval;

public class PriceInterval {

	private final int amount;
	private final TimeUnit unit;
	private final int displayAmount;
	private final int moveAmount;

	public PriceInterval(int amount, TimeUnit unit, int displayAmount, int moveAmount) {
		this.amount = amount;
		this.unit = unit;
		this.displayAmount = displayAmount;
		this.moveAmount = moveAmount;
	}

	public int getDisplayAmount() {
		return displayAmount;
	}

	public int getMoveAmount() {
		return moveAmount;
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
		if (object instanceof PriceInterval) {
			PriceInterval that = (PriceInterval) object;
			return this.getAmount() == that.getAmount() && this.getUnit().equals(that.getUnit());
		}
		return false;
	}

	@Override
	public String toString() {
		return amount + " " + unit;
	}

	public IPriceInterval asPriceInterval() {
		return new TimeUnitInterval(amount, unit);
	}
}
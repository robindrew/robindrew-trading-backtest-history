package com.robindrew.trading.backtest.history.image;

public interface IImageCache {

	long put(byte[] image);

	byte[] get(long imageId);

}

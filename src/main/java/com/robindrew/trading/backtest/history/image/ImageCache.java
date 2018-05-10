package com.robindrew.trading.backtest.history.image;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ImageCache implements IImageCache {

	private final AtomicLong nextId = new AtomicLong(0);

	private final Cache<Long, byte[]> cache;

	public ImageCache(long capacity) {
		this.cache = CacheBuilder.newBuilder().maximumSize(capacity).build();
	}

	@Override
	public long put(byte[] image) {
		if (image.length == 0) {
			throw new IllegalArgumentException("image is empty");
		}
		Long imageId = nextId.incrementAndGet();
		cache.put(imageId, image);
		return imageId;
	}

	@Override
	public byte[] get(long imageId) {
		byte[] image = cache.getIfPresent(imageId);
		if (image == null) {
			throw new IllegalArgumentException("image not found: imageId=" + imageId);
		}
		return image;
	}

}

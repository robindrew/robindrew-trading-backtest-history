package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.http.ContentType.IMAGE_PNG;

import com.google.common.io.ByteSource;
import com.robindrew.common.dependency.DependencyFactory;
import com.robindrew.common.http.servlet.executor.IHttpExecutor;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.trading.backtest.history.image.IImageCache;

public class ChartPage implements IHttpExecutor {

	@Override
	public void execute(IHttpRequest request, IHttpResponse response) {

		long imageId = request.getLong("imageId");
		IImageCache cache = DependencyFactory.getDependency(IImageCache.class);

		byte[] png = cache.get(imageId);
		response.ok(IMAGE_PNG, ByteSource.wrap(png));
	}

}

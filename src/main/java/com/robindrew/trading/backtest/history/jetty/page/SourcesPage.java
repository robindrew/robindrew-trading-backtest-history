package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.price.candle.format.IPriceFormat;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceProviderManager;
import com.robindrew.trading.price.candle.format.pcf.source.file.IPcfFileProviderLocator;
import com.robindrew.trading.price.candle.format.ptf.source.IPtfSourceProviderManager;
import com.robindrew.trading.price.candle.format.ptf.source.file.IPtfFileManager;
import com.robindrew.trading.provider.ITradingProvider;

public class SourcesPage extends AbstractServicePage {

	public SourcesPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		List<SourceInstrument> sources = new ArrayList<>();

		// PCF Files
		IPcfFileProviderLocator pcf = getDependency(IPcfFileProviderLocator.class);
		for (IPcfSourceProviderManager manager : pcf.getProviders()) {
			for (IInstrument instrument : manager.getInstruments()) {
				sources.add(new SourceInstrument(manager.getFormat(), manager.getProvider(), instrument));
			}
		}

		// PTF Files
		IPtfFileManager ptf = getDependency(IPtfFileManager.class);
		for (IPtfSourceProviderManager manager : ptf.getProviders()) {
			for (IInstrument instrument : manager.getInstruments()) {
				sources.add(new SourceInstrument(manager.getFormat(), manager.getProvider(), instrument));
			}
		}

		dataMap.put("sources", sources);
	}

	public static class SourceInstrument {

		private final IPriceFormat format;
		private final ITradingProvider provider;
		private final IInstrument instrument;

		public SourceInstrument(IPriceFormat format, ITradingProvider provider, IInstrument instrument) {
			this.format = format;
			this.provider = provider;
			this.instrument = instrument;
		}

		public IPriceFormat getFormat() {
			return format;
		}

		public ITradingProvider getProvider() {
			return provider;
		}

		public IInstrument getInstrument() {
			return instrument;
		}
	}
}

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
import com.robindrew.trading.price.candle.format.PriceFormat;
import com.robindrew.trading.price.candle.format.pcf.source.file.IPcfFileManager;
import com.robindrew.trading.price.candle.format.ptf.source.file.IPtfFileManager;
import com.robindrew.trading.provider.ITradeDataProvider;

public class SourcesPage extends AbstractServicePage {

	public SourcesPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		List<SourceInstrument> sources = new ArrayList<>();

		// PCF Files
		IPcfFileManager pcf = getDependency(IPcfFileManager.class);
		for (ITradeDataProvider provider : pcf.getProviders()) {
			for (IInstrument instrument : pcf.getInstruments(provider)) {
				sources.add(new SourceInstrument(PriceFormat.PCF, provider, instrument));
			}
		}

		// PTF Files
		IPtfFileManager ptf = getDependency(IPtfFileManager.class);
		for (ITradeDataProvider provider : ptf.getProviders()) {
			for (IInstrument instrument : ptf.getInstruments(provider)) {
				sources.add(new SourceInstrument(PriceFormat.PTF, provider, instrument));
			}
		}

		dataMap.put("sources", sources);
	}

	public static class SourceInstrument {

		private final PriceFormat format;
		private final ITradeDataProvider provider;
		private final IInstrument instrument;

		public SourceInstrument(PriceFormat format, ITradeDataProvider provider, IInstrument instrument) {
			this.format = format;
			this.provider = provider;
			this.instrument = instrument;
		}

		public PriceFormat getFormat() {
			return format;
		}

		public ITradeDataProvider getProvider() {
			return provider;
		}

		public IInstrument getInstrument() {
			return instrument;
		}
	}
}

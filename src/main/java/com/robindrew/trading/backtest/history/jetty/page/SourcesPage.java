package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.SetMultimapBuilder;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.IInstrument;
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

		// PCF Files
		IPcfFileManager pcf = getDependency(IPcfFileManager.class);
		Multimap<String, String> pcfInstruments = SetMultimapBuilder.treeKeys().treeSetValues().build();
		for (ITradeDataProvider provider : pcf.getProviders()) {
			for (IInstrument instrument : pcf.getInstruments(provider)) {
				pcfInstruments.put(provider.name(), instrument.getName());
			}
		}
		dataMap.put("pcfInstruments", pcfInstruments);

		// PTF Files
		IPtfFileManager ptf = getDependency(IPtfFileManager.class);
		Multimap<String, String> ptfInstruments = SetMultimapBuilder.treeKeys().treeSetValues().build();
		for (ITradeDataProvider provider : ptf.getProviders()) {
			for (IInstrument instrument : ptf.getInstruments(provider)) {
				ptfInstruments.put(provider.name(), instrument.getName());
			}
		}
		dataMap.put("ptfInstruments", ptfInstruments);
	}
}

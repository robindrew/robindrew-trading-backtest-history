package com.robindrew.trading.backtest.history.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.SetMultimapBuilder;
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
		Multimap<Source, String> pcfInstruments = SetMultimapBuilder.treeKeys().treeSetValues().build();
		for (ITradeDataProvider provider : pcf.getProviders()) {
			for (IInstrument instrument : pcf.getInstruments(provider)) {
				pcfInstruments.put(new Source(provider, instrument), instrument.getName());
				sources.add(new SourceInstrument(PriceFormat.PCF, provider, instrument));
			}
		}
		dataMap.put("pcfInstruments", pcfInstruments);

		// PTF Files
		IPtfFileManager ptf = getDependency(IPtfFileManager.class);
		Multimap<Source, String> ptfInstruments = SetMultimapBuilder.treeKeys().treeSetValues().build();
		for (ITradeDataProvider provider : ptf.getProviders()) {
			for (IInstrument instrument : ptf.getInstruments(provider)) {
				ptfInstruments.put(new Source(provider, instrument), instrument.getName());
				sources.add(new SourceInstrument(PriceFormat.PTF, provider, instrument));
			}
		}
		dataMap.put("ptfInstruments", ptfInstruments);

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

	public static class Source implements Comparable<Source> {

		private final String provider;
		private final String instrumentType;

		public Source(ITradeDataProvider provider, IInstrument instrument) {
			this.provider = provider.name();
			this.instrumentType = instrument.getType().name();
		}

		public String getProvider() {
			return provider;
		}

		public String getInstrumentType() {
			return instrumentType;
		}

		@Override
		public int hashCode() {
			HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(this.provider);
			hash.append(this.instrumentType);
			return hash.toHashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (object == this) {
				return true;
			}
			if (object instanceof Source) {
				Source that = (Source) object;
				EqualsBuilder equals = new EqualsBuilder();
				equals.append(this.provider, that.provider);
				equals.append(this.instrumentType, that.instrumentType);
				return equals.isEquals();
			}
			return false;
		}

		@Override
		public int compareTo(Source that) {
			CompareToBuilder compare = new CompareToBuilder();
			compare.append(this.provider, that.provider);
			compare.append(this.instrumentType, that.instrumentType);
			return compare.toComparison();
		}

	}
}

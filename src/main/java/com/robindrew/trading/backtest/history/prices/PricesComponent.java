package com.robindrew.trading.backtest.history.prices;

import static com.robindrew.common.dependency.DependencyFactory.setDependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.properties.map.type.FileProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.price.candle.format.pcf.source.file.IPcfFileManager;
import com.robindrew.trading.price.candle.format.pcf.source.file.PcfFileManager;
import com.robindrew.trading.price.candle.format.ptf.source.file.IPtfFileManager;
import com.robindrew.trading.price.candle.format.ptf.source.file.PtfFileManager;
import com.robindrew.trading.provider.ITradeDataProvider;

public class PricesComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(PricesComponent.class);

	private static final FileProperty pcfRootDir = new FileProperty("root.dir.pcf").checkDirectory();
	private static final FileProperty ptfRootDir = new FileProperty("root.dir.ptf").checkDirectory();

	@Override
	protected void startupComponent() throws Exception {

		// PCF Files
		log.info("[PCF Directory] {}", pcfRootDir.get());
		IPcfFileManager pcf = new PcfFileManager(pcfRootDir.get());
		for (ITradeDataProvider provider : pcf.getProviders()) {
			log.info("[PCF Provider] {}", provider);
		}
		setDependency(IPcfFileManager.class, pcf);

		// PTF Files
		log.info("[PTF Directory] {}", ptfRootDir.get());
		IPtfFileManager ptf = new PtfFileManager(ptfRootDir.get());
		for (ITradeDataProvider provider : ptf.getProviders()) {
			log.info("[PTF Provider] {}", provider);
		}
		setDependency(IPtfFileManager.class, ptf);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// Nothing to do
	}
}

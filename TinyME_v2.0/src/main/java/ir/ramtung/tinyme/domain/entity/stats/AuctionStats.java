package ir.ramtung.tinyme.domain.entity.stats;

import lombok.Getter;

@Getter
public class AuctionStats extends SecurityStats {

	int openingPrice;
	int tradableQuantity;

	private AuctionStats(int openingPrice, int tradableQuantity) {
		this.openingPrice = openingPrice;
		this.tradableQuantity = tradableQuantity;
	}

	public static AuctionStats createAuctionStats(int openingPrice, int tradableQuantity) {
		return new AuctionStats(openingPrice, tradableQuantity);
	}
}

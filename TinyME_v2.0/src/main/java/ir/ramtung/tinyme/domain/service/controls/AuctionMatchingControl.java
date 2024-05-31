package ir.ramtung.tinyme.domain.service.controls;

import org.springframework.stereotype.Service;

@Service
public class AuctionMatchingControl extends MatchingControl {

	public AuctionMatchingControl(
		PositionControl positionControl,
		CreditControl creditControl,
		QuantityControl quantityControl
	) {
		super(positionControl, creditControl, quantityControl);
	}
}

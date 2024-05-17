package ir.ramtung.tinyme.domain.entity.security_stats;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.SecurityState;
import ir.ramtung.tinyme.domain.entity.Trade;
import lombok.Getter;

@Getter
public class ExecuteStats extends SecurityStats {
    private long orderId;
    private List<Trade> trades;
    private SecurityState securityState;

    private ExecuteStats(long orderId, List<Trade> trades, SecurityState securityState) {
        this.orderId = orderId;
        this.trades = trades;
        this.securityState = securityState;
    }

    public static ExecuteStats createAuctionExecuteStats(List<Trade> trades) {
        return new ExecuteStats(0, trades, SecurityState.AUCTION);
    }

    public static ExecuteStats createContinuousExecuteStats(List<Trade> trades, long orderId) {
        return new ExecuteStats(orderId, trades, SecurityState.CONTINUOUS);
    }

    public long getOrderId() {
        if (securityState == SecurityState.AUCTION) {
            throw new IllegalStateException("Action execute stats can not have order id");
        }

        return this.orderId;
    }
    
    public boolean isAuction() {
        return securityState == SecurityState.AUCTION;
    }

    public boolean isCountinues() {
        return securityState == SecurityState.CONTINUOUS;
    }
}

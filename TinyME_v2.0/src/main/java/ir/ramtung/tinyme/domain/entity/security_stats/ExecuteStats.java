package ir.ramtung.tinyme.domain.entity.security_stats;

import java.util.List;

import ir.ramtung.tinyme.domain.entity.SecurityState;
import ir.ramtung.tinyme.domain.entity.Trade;
import lombok.Getter;

@Getter
public class ExecuteStats extends SecurityStats {
    private long orderId;
    private long requestId;
    private List<Trade> trades;
    private SecurityState securityState;
    private boolean isForActivatedOrder;

    private ExecuteStats(long orderId, List<Trade> trades, SecurityState securityState) {
        this.orderId = orderId;
        this.trades = trades;
        this.securityState = securityState;
        this.isForActivatedOrder = false;
    }

    private ExecuteStats(long orderId, long requestId, boolean isForActivatedOrder, List<Trade> trades, SecurityState securityState) {
        this(orderId, trades, securityState);
        this.requestId = requestId;
        this.isForActivatedOrder = isForActivatedOrder;
    }

    public static ExecuteStats createAuctionExecuteStats(List<Trade> trades) {
        return new ExecuteStats(0, trades, SecurityState.AUCTION);
    }

    public static ExecuteStats createContinuousExecuteStats(List<Trade> trades, long orderId) {
        return new ExecuteStats(orderId, trades, SecurityState.CONTINUOUS);
    }

    public static ExecuteStats createContinuousExecuteStatsForActivatedOrder(List<Trade> trades, long orderId, long requestId) {
        return new ExecuteStats(orderId, requestId, true, trades, SecurityState.CONTINUOUS);
    }

    public long getOrderId() {
        if (securityState == SecurityState.AUCTION) {
            throw new IllegalStateException("Action execute stats can not have order id");
        }
        return this.orderId;
    }

    public long getRequestId(){
        if (!isForActivatedOrder) {
            throw new IllegalStateException("Only order activated execution stats has requestId");
        }
        return requestId;
    }

    public boolean isForActivatedOrder() {
        return isForActivatedOrder;
    }
    
    public boolean isAuction() {
        return securityState == SecurityState.AUCTION;
    }

    public boolean isCountinues() {
        return securityState == SecurityState.CONTINUOUS;
    }
}

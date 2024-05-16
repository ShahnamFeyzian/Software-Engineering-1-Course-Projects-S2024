package ir.ramtung.tinyme.messaging.request;


import lombok.Getter;

@Getter
public class ChangeMatchingStateRq extends BaseRq {

    private final MatchingState targetState;

    public ChangeMatchingStateRq(String securityIsin, MatchingState targetState) {
        this.securityIsin = securityIsin;
        this.targetState = targetState;
    }
}

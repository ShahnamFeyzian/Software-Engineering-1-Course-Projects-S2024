package ir.ramtung.tinyme.messaging.request;


import lombok.Getter;

@Getter
public class ChangeMatchingStateRq extends BaseRq {

    private MatchingState targetState;
}

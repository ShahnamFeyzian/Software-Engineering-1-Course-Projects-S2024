package bank.communication;

public enum Codes {
    SUCCESSFUL,
    INSUFFICIENT,
    UNKNOWN;

    public String toString() {
        switch (this) {
            case SUCCESSFUL:
                return "0";
            case INSUFFICIENT:
                return "1";
            case UNKNOWN:
                return "2";
            default:
                return "";
        }
    }
}

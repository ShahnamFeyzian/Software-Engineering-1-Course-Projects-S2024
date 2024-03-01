package self.SE1.CA1.communication;

public enum ResponseStatus {
    SUCCESS,
    LOGIC_ERROR,
    NOT_FOUND,
    NON;

    public static int getStatusCode(ResponseStatus s) {
        switch (s) {
            case SUCCESS:     return 0;
            case LOGIC_ERROR: return 1;
            case NOT_FOUND:   return 2;
            default:          return 3;
        }
    }
}

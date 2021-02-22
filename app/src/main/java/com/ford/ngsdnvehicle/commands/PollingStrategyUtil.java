package com.ford.ngsdnvehicle.commands;

public class PollingStrategyUtil {
    public static boolean hasRequestTimedOut(long requestStartTime, long currentTimeMillis) {
        return true;
    }

    public static long getRequestDelay(long requestStartTime, long currentTimeMillis) {
        return 0;
    }
}

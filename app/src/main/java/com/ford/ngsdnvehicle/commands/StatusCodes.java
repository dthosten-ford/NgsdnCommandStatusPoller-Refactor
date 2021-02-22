package com.ford.ngsdnvehicle.commands;

public enum StatusCodes {
    ;
    public static final int ERROR_DEEP_SLEEP_V2 = 1;
    public static final int SUCCESS = 2;
    public static final int COMMAND_PROCESSING = 3;
    public static final int TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2 = 4;
    public static final int ERROR_COMMAND_SENT_FAILED_RESPONSE = 5;
    public static final int TCU_FIRMWARE_UPGRADE_IN_PROGRESS = 6;
    public static final int ERROR_POLL_TIMEOUT = 7;
//    COMMAND_PROCESSING, ERROR_CCS_SETTINGS_OFF, ERROR_COMMAND_SENT_FAILED_RESPONSE, ERROR_DEEP_SLEEP_V2, ERROR_POLL_TIMEOUT, PENDING_TCU_RESPONSE, SUCCESS, TCU_FIRMWARE_UPGRADE_IN_PROGRESS, TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2

}

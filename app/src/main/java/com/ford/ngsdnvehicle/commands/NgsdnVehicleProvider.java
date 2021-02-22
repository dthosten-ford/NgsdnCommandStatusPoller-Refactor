package com.ford.ngsdnvehicle.commands;

public interface NgsdnVehicleProvider {
    void updateWifiSettings(String vin, NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse);

    void updateCommandEventStatus(String vin, NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse);
}

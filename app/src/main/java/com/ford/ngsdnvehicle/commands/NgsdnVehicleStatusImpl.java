package com.ford.ngsdnvehicle.commands;

import java.util.BitSet;
import java.util.Optional;

import io.reactivex.Single;

public interface NgsdnVehicleStatusImpl {

    Optional<SingleValueField<Boolean>> getDeepSleepInProgress();

    Optional<SingleValueField<Boolean>> getFirmwareUpgInProgress();

}

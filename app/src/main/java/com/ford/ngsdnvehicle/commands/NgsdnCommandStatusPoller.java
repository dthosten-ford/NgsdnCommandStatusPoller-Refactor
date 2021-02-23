/*
 * CONFIDENTIAL FORD MOTOR COMPANY
 * This is an unpublished work of authorship, which contains confidential information and/or trade secrets, created in 2019. Ford Motor Company owns all rights to this work and intends to maintain it in confidence to preserve its trade secret status. Ford Motor Company reserves all rights, under the copyright laws of the United States or those of any other country that may have jurisdiction, including the right to protect this work as an unpublished work, in the event of an inadvertent or deliberate unauthorized publication. Use of this work constitutes an agreement to maintain the confidentiality of the work, and to refrain from any reverse engineering, decompilation, or disassembly of this work.
 * Ford Motor Company also reserves its rights under all copyright laws to protect this work as a published work, when appropriate. Those having access to this work may not copy it, use it, modify it, or disclose the information contained in it without the written authorization of Ford Motor Company.
 * Copyright 2019, Ford Motor Company.
 *
 */

package com.ford.ngsdnvehicle.commands;

//import com.ford.networkutils.utils.StatusCarryingException;
//import com.ford.networkutils.utils.StatusCodes;
//import com.ford.ngsdncommon.models.NgsdnException;
//import com.ford.ngsdnvehicle.models.FirmwareUpgradingException;
//import com.ford.ngsdnvehicle.models.NgsdnVehicleStatusImpl;
//import com.ford.ngsdnvehicle.models.NgsdnVehicleStatusResponse;
//import com.ford.ngsdnvehicle.models.RemoteLockFailureException;
//import com.ford.ngsdnvehicle.models.RemoteStartFailureException;
//import com.ford.ngsdnvehicle.models.TrailerLightCheckException;
//import com.ford.ngsdnvehicle.providers.NgsdnVehicleProvider;
//import com.ford.ngsdnvehicle.strategies.NgsdnVehicleCommandStrategy;
//import com.ford.rxutils.qualifiers.ComputationScheduler;
//import com.ford.utils.PollingStrategyUtil;
//import com.ford.utils.TimeProvider;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;
//smell: inject this value, don't rely on static ref
//import static com.ford.ngsdnvehicle.models.CommandEventData.LOCK_SECURE_WARNING_ON;

import java.util.*;
import java.util.function.*;

public class NgsdnCommandStatusPoller {

    private static final int POLLING_DELAY = 5;
    private static final int POLLING_STATUS_CODE = 552;
    private static final Object LOCK_SECURE_WARNING_ON = "sampleLOCK_SECURE_WARNING_ONmsgForTestingSinceTheresHardStaticRef";

    private final Scheduler computationScheduler;
    private final TimeProvider timeProvider;
    private final NgsdnVehicleProvider vehicleProvider;
    private boolean wasUpgrading = false;

    @Inject
    NgsdnCommandStatusPoller(@ComputationScheduler Scheduler computationScheduler, TimeProvider timeProvider, NgsdnVehicleProvider vehicleProvider) {
        this.computationScheduler = computationScheduler;
        this.timeProvider = timeProvider;
        this.vehicleProvider = vehicleProvider;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    Single<NgsdnVehicleStatusResponse> pollCommandStatus(final String vin, final String commandId, final NgsdnVehicleCommandStrategy ngsdnVehicleCommandStrategy) {
        return pollCommandStatusCustomTime(vin, commandId, ngsdnVehicleCommandStrategy, POLLING_DELAY);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    Single<NgsdnVehicleStatusResponse> pollCommandStatusCustomTime(final String vin, final String commandId, final NgsdnVehicleCommandStrategy ngsdnVehicleCommandStrategy, final int pollInterval) {
        final long requestStartTime = timeProvider.currentTimeMillis();
        return Single.timer(pollInterval, TimeUnit.SECONDS, computationScheduler)
                .flatMap(ignore -> getCommandStatus(vin, commandId, ngsdnVehicleCommandStrategy, requestStartTime));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Single<NgsdnVehicleStatusResponse> getCommandStatus(final String vin, final String commandId, final NgsdnVehicleCommandStrategy ngsdnVehicleCommandStrategy, final long requestStartTime) {
        return ngsdnVehicleCommandStrategy.getCommandStatus(vin, commandId)
                .flatMap(ngsdnVehicleStatusResponse -> {
                    //Smell: too much interweaving RX with BizLogic.  Get data, return data, do RX magic.
                    switch (ngsdnVehicleStatusResponse.getStatus()) {
                        case StatusCodes.SUCCESS:
                        case StatusCodes.COMMAND_PROCESSING:
                            //smell: using impl instead of interface
                            NgsdnVehicleStatusImpl vehicleStatus = ngsdnVehicleStatusResponse.getVehicleStatus();
                            if (vehicleStatus != null) {
                                //Smell: I don't think we are deep enough.  /sarcasm
                                if (vehicleStatus.getDeepSleepInProgress().isPresent() && vehicleStatus.getDeepSleepInProgress().get().getValue().or(false)) {
                                    return Single.error(new NgsdnException(StatusCodes.ERROR_DEEP_SLEEP_V2));
                                }

                                if (vehicleStatus.getFirmwareUpgInProgress().isPresent() && vehicleStatus.getFirmwareUpgInProgress().get().getValue().or(false)) {
                                    return Single.error(new NgsdnException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2));
                                }
                                return Single.just(ngsdnVehicleStatusResponse);
                            } else if (ngsdnVehicleStatusResponse.getDoorPresentStatuses() != null) {
                                return getEventDataStart(vin, ngsdnVehicleStatusResponse);
                            } else if (ngsdnVehicleStatusResponse.getCommandEventData().isPresent()) {
                                return getEventData(vin, ngsdnVehicleStatusResponse);
                            } else if (ngsdnVehicleStatusResponse.getWifiSettingsData().isPresent()) {
                                vehicleProvider.updateWifiSettings(vin, ngsdnVehicleStatusResponse);
                                return Single.just(ngsdnVehicleStatusResponse);
                            } else {
                                return Single.just(ngsdnVehicleStatusResponse);
                            }
                        case StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE:
                            if (ngsdnVehicleStatusResponse.getCommandEventData().isPresent() && ngsdnVehicleStatusResponse.getCommandEventData().get().getLockSecureWarning() == LOCK_SECURE_WARNING_ON) {
                                vehicleProvider.updateCommandEventStatus(vin, ngsdnVehicleStatusResponse);
                                return Single.just(ngsdnVehicleStatusResponse);
                            } else if (ngsdnVehicleStatusResponse.getRemoteStartFailures().isPresent()) {
                                //smell: object construction.push to factory
                                return Single.error(new RemoteStartFailureException(ngsdnVehicleStatusResponse.getStatus(), ngsdnVehicleStatusResponse.getRemoteStartFailures().get().getRemoteStartFailureErrors()));
                            } else if (ngsdnVehicleStatusResponse.getTrailerLightCheckFailureReason().isPresent()) {
                                return Single.error(new TrailerLightCheckException(ngsdnVehicleStatusResponse.getStatus(), ngsdnVehicleStatusResponse.getTrailerLightCheckFailureReason().get()));
                            } else if (ngsdnVehicleStatusResponse.getRemoteLockFailures().isPresent()) {
                                return Single.error(new RemoteLockFailureException(ngsdnVehicleStatusResponse.getStatus(), ngsdnVehicleStatusResponse.getRemoteLockFailures().get().getRemoteLockFailureErrors()));
                            } else {
                                return Single.error(new NgsdnException(ngsdnVehicleStatusResponse.getStatus()));
                            }
                        default:
                            return Single.error(new NgsdnException(ngsdnVehicleStatusResponse.getStatus()));
                    }
                })
                .doOnError(throwable -> {
                    if (new NgsdnException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS).equals(throwable) || new NgsdnException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2).equals(throwable)) {
                        wasUpgrading = true;
                    }
                })
                .retryWhen(observable -> observable.flatMap((Function<Throwable, Flowable<Long>>) e -> {
                    //smell: using reflection. can try/catch filter simplify?
                    if (e instanceof StatusCarryingException) {
                        StatusCarryingException error = (StatusCarryingException) e;
                        if (error.getStatusCode() == POLLING_STATUS_CODE) {
                            //smell: Is this a static?  wrap with interface.
                            if (PollingStrategyUtil.hasRequestTimedOut(requestStartTime, timeProvider.currentTimeMillis())) {
                                return Flowable.error(new NgsdnException(StatusCodes.ERROR_POLL_TIMEOUT));
                            } else {
                                final long delayMillis = PollingStrategyUtil.getRequestDelay(requestStartTime, timeProvider.currentTimeMillis());
                                return Flowable.timer(delayMillis, TimeUnit.MILLISECONDS, computationScheduler);
                            }
                        }
                    }
                    return Flowable.error(e);
                }))
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof NgsdnException && wasUpgrading) {
                        int statusCode = ((StatusCarryingException) throwable).getStatusCode();
                        return Single.error(new FirmwareUpgradingException(statusCode));
                    } else {
                        return Single.error(throwable);
                    }
                })
                .doAfterTerminate(() -> wasUpgrading = false);
    }

    private Single<NgsdnVehicleStatusResponse> getEventData(String vin, NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse) {
        if (ngsdnVehicleStatusResponse.getCommandEventData().get().getDoorStatuses() != null || ngsdnVehicleStatusResponse.getCommandEventData().get().getDoorPresentStatuses() != null) {
            vehicleProvider.updateCommandEventStatus(vin, ngsdnVehicleStatusResponse);
        }
        return Single.just(ngsdnVehicleStatusResponse);
    }

    private Single<NgsdnVehicleStatusResponse> getEventDataStart(String vin, NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse) {
        if (ngsdnVehicleStatusResponse.getDoorPresentStatuses() != null) {
            vehicleProvider.updateCommandEventStatus(vin, ngsdnVehicleStatusResponse);
        }
        return Single.just(ngsdnVehicleStatusResponse);
    }


}
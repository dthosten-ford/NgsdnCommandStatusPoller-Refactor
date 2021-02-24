/*
 * CONFIDENTIAL FORD MOTOR COMPANY
 * This is an unpublished work of authorship, which contains confidential information and/or trade secrets, created in 2019. Ford Motor Company owns all rights to this work and intends to maintain it in confidence to preserve its trade secret status. Ford Motor Company reserves all rights, under the copyright laws of the United States or those of any other country that may have jurisdiction, including the right to protect this work as an unpublished work, in the event of an inadvertent or deliberate unauthorized publication. Use of this work constitutes an agreement to maintain the confidentiality of the work, and to refrain from any reverse engineering, decompilation, or disassembly of this work.
 * Ford Motor Company also reserves its rights under all copyright laws to protect this work as a published work, when appropriate. Those having access to this work may not copy it, use it, modify it, or disclose the information contained in it without the written authorization of Ford Motor Company.
 * Copyright 2019, Ford Motor Company.
 *
 */

package com.ford.ngsdnvehicle.commands;

//import com.ford.networkutils.utils.StatusCodes;
//import com.ford.ngsdncommon.models.NgsdnException;
//import com.ford.ngsdnvehicle.models.CommandEventData;
//import com.ford.ngsdnvehicle.models.DoorPresentStatuses;
//import com.ford.ngsdnvehicle.models.DoorStatuses;
//import com.ford.ngsdnvehicle.models.FirmwareUpgradingException;
//import com.ford.ngsdnvehicle.models.NgsdnVehicleStatusImpl;
//import com.ford.ngsdnvehicle.models.NgsdnVehicleStatusResponse;
//import com.ford.ngsdnvehicle.models.RemoteLockFailure;
//import com.ford.ngsdnvehicle.models.RemoteLockFailureException;
//import com.ford.ngsdnvehicle.models.RemoteLockFailures;
//import com.ford.ngsdnvehicle.models.RemoteStartFailureException;
//import com.ford.ngsdnvehicle.models.RemoteStartFailures;
//import com.ford.ngsdnvehicle.models.TrailerLightCheckException;
//import com.ford.ngsdnvehicle.models.WifiSettingsData;
//import com.ford.ngsdnvehicle.models.vehiclestatus.SingleValueField;
//import com.ford.ngsdnvehicle.providers.NgsdnVehicleProvider;
//import com.ford.ngsdnvehicle.strategies.NgsdnVehicleCommandStrategy;
//import com.ford.utils.TimeProvider;
//import com.ford.vehiclecommon.models.ZoneStatuses;
import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NgsdnCommandStatusPollerTest {
    private static final String VIN = "vin";
    private static final String COMMAND_ID = "command_id";

    @Mock
    TimeProvider timeProvider;

    @Mock
    NgsdnVehicleStatusResponse commandStatusResponse;

    @Mock
    NgsdnVehicleCommandStrategy ngsdnVehicleCommandStrategy;

    @Mock
    NgsdnVehicleProvider vehicleProvider;

    @Mock
    DoorStatuses doorStatuses;

    @Mock
    DoorPresentStatuses doorPresentStatuses;

    private TestScheduler testScheduler = new TestScheduler();
    private TestScheduler pollingDelay = new TestScheduler();

    private NgsdnCommandStatusPoller subject;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.PENDING_TCU_RESPONSE);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.absent());
        when(commandStatusResponse.getWifiSettingsData()).thenReturn(Optional.absent());
        when(timeProvider.currentTimeMillis()).thenReturn(0l);
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.just(commandStatusResponse));

        subject = new NgsdnCommandStatusPoller(testScheduler, timeProvider, vehicleProvider);
    }

    @Test
    public void pollCommandStatus_shouldPollStatusWhileStillPendingTCUResponse() {
        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        // Once immediately
        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);

        // Then once every second for 15 seconds
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        when(timeProvider.currentTimeMillis()).thenReturn(15000l); // simulate 15 seconds passing

        // Then once every 2 seconds for 30 seconds
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        when(timeProvider.currentTimeMillis()).thenReturn(45000l); // simulate 30 seconds passing

        // Then once every 5 seconds
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        when(timeProvider.currentTimeMillis()).thenReturn(120000l); // simulate a total of 2 minutes passing

        testScheduler.advanceTimeBy(120, TimeUnit.SECONDS);

        // Times out after 2 minutes
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_POLL_TIMEOUT));
    }

    @Test
    public void pollCommandStatus_shouldPollStatusUntilSuccessfulResponse() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        // Once immediately
        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);

        // Then once every second for 15 seconds
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        when(timeProvider.currentTimeMillis()).thenReturn(15000l); // simulate 15 seconds passing

        // Then once every 2 seconds for 30 seconds
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        when(timeProvider.currentTimeMillis()).thenReturn(45000l); // simulate 30 seconds passing

        // Then once every 5 seconds
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        testObserver.assertNoErrors();
        testObserver.assertComplete();
    }

    @Test
    public void pollCommandStatus_onError_retry() {
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.error(new NgsdnException(552)));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertNotComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void pollCommandStatus_onCommandFailedError_notifySubscriberOfFailure() {
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.error(new NgsdnException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE)));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE));
    }

    @Test
    public void pollCommandStatus_onCommandFailedDeepSleepError_notifySubscriberOfFailure() {
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.error(new NgsdnException(StatusCodes.ERROR_DEEP_SLEEP_V2)));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_DEEP_SLEEP_V2));
    }

    @Test
    public void pollCommandStatus_onCommandFailedCcsSettingsOff_notifySubscriberOfFailure() {
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.error(new NgsdnException(StatusCodes.ERROR_CCS_SETTINGS_OFF)));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_CCS_SETTINGS_OFF));
    }

    @Test
    public void pollCommandStatus_onCommandFailed_afterFirmwareUpgrading_notifySubscriberOfFailure() {
        when(ngsdnVehicleCommandStrategy.getCommandStatus(VIN, COMMAND_ID)).thenReturn(Single.error(new NgsdnException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2)));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new FirmwareUpgradingException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2));
    }

    @Test
    public void pollCommandStatus_commandSuccessful_notifySubscriberOfSuccess() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertComplete();
    }

    @Test
    public void pollRefreshCommandStatus_statusReturnedWithDeepSleepInProgress_notifySubscriberWithDeepSleepError() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        NgsdnVehicleStatusImpl vehicleStatus = mock(NgsdnVehicleStatusImpl.class);
        SingleValueField<Boolean> deepSleepError = mock(SingleValueField.class);
        when(vehicleStatus.getDeepSleepInProgress()).thenReturn(Optional.of(deepSleepError));
        when(deepSleepError.getValue()).thenReturn(Optional.of(true));
        when(commandStatusResponse.getVehicleStatus()).thenReturn(vehicleStatus);

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_DEEP_SLEEP_V2));
    }

    @Test
    public void pollRefreshCommandStatus_statusReturnedWithTcuFirmwareUpgradeInProgress_notifySubscriberWithFotaError() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        NgsdnVehicleStatusImpl vehicleStatus = mock(NgsdnVehicleStatusImpl.class);
        SingleValueField<Boolean> fotaError = mock(SingleValueField.class);
        when(vehicleStatus.getDeepSleepInProgress()).thenReturn(Optional.absent());
        when(vehicleStatus.getFirmwareUpgInProgress()).thenReturn(Optional.of(fotaError));
        when(fotaError.getValue()).thenReturn(Optional.of(true));
        when(commandStatusResponse.getVehicleStatus()).thenReturn(vehicleStatus);

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new FirmwareUpgradingException(StatusCodes.TCU_FIRMWARE_UPGRADE_IN_PROGRESS_V2));
    }

    @Test
    public void pollCommandStatusCustomTime_waitsForZeroSecond_notifySubscriber() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        NgsdnVehicleStatusImpl vehicleStatus = mock(NgsdnVehicleStatusImpl.class);
        when(vehicleStatus.getDeepSleepInProgress()).thenReturn(Optional.absent());
        when(vehicleStatus.getFirmwareUpgInProgress()).thenReturn(Optional.absent());
        when(commandStatusResponse.getVehicleStatus()).thenReturn(vehicleStatus);

        TestObserver<NgsdnVehicleStatusResponse> testObserver = subject.pollCommandStatusCustomTime(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy, 0).test();

        testScheduler.advanceTimeBy(0, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertValue(commandStatusResponse).assertComplete();
    }

    @Test
    public void pollCommandSuccess_hasDoorStatusesInLockCommandEventData_updateLockEventStatusProvider() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        CommandEventData commandEventData = mock(CommandEventData.class);
        when(commandEventData.getDoorStatuses()).thenReturn(doorStatuses);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.of(commandEventData));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        verify(vehicleProvider).updateCommandEventStatus(VIN, commandStatusResponse);
        testObserver.assertComplete();
    }

    @Test
    public void pollCommandSuccess_hasDoorPresentStatusesInLockCommandEventData_updateLockEventStatusProvider() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        CommandEventData commandEventData = mock(CommandEventData.class);
        when(commandEventData.getDoorPresentStatuses()).thenReturn(doorPresentStatuses);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.of(commandEventData));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        verify(vehicleProvider).updateCommandEventStatus(VIN, commandStatusResponse);
        testObserver.assertComplete();
    }

    @Test
    public void pollCommandSuccess_hasZoneLightingCommandEventData_updateLockEventStatusProvider() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        CommandEventData commandEventData = mock(CommandEventData.class);
        ZoneStatuses zoneStatuses = mock(ZoneStatuses.class);
        when(commandEventData.getZoneStatuses()).thenReturn(zoneStatuses);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.of(commandEventData));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void pollCommandSuccess_hasWifiSettingsData_updateWifiSettings() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.SUCCESS);
        WifiSettingsData wifiSettingsData = mock(WifiSettingsData.class);
        when(commandStatusResponse.getWifiSettingsData()).thenReturn(Optional.of(wifiSettingsData));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        verify(vehicleProvider).updateWifiSettings(VIN, commandStatusResponse);
        testObserver.assertComplete();
    }

    @Test
    public void lockCommandFailed_hasLockCommandEventDataAndWarningIsOn_updateLockEventStatusProvider() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        CommandEventData commandEventData = mock(CommandEventData.class);
        when(commandEventData.getLockSecureWarning()).thenReturn(1);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.of(commandEventData));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        verify(vehicleProvider).updateCommandEventStatus(VIN, commandStatusResponse);
        testObserver.assertComplete();
    }

    @Test
    public void lockCommandFailed_hasLockCommandEventDataAndWarningIsOff_notifySubscriberOfFailure() {
        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);

        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        CommandEventData commandEventData = mock(CommandEventData.class);
        when(commandEventData.getLockSecureWarning()).thenReturn(0);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.of(commandEventData));

        verify(vehicleProvider, never()).updateCommandEventStatus(VIN, commandStatusResponse);
    }

    @Test
    public void lockCommandFailed_doesNotHaveLockCommandEventData_doesNotUpdateLockEventStatusProvider() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        when(commandStatusResponse.getCommandEventData()).thenReturn(Optional.absent());

        subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        verify(vehicleProvider, never()).updateCommandEventStatus(VIN, commandStatusResponse);
    }

    @Test
    public void pollCommandStatus_commandProcessing_notifySubscriberOfSuccess() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.COMMAND_PROCESSING);

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
    }

    @Test
    public void pollCommandStatus_neitherSuccessfulProcessingNorPending_notifySubscriberOfFailure() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        when(commandStatusResponse.getRemoteStartFailures()).thenReturn(Optional.absent());
        when(commandStatusResponse.getRemoteLockFailures()).thenReturn(Optional.absent());
        when(commandStatusResponse.getTrailerLightCheckFailureReason()).thenReturn(Optional.absent());

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new NgsdnException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE));
    }

    @Test
    public void pollCommandStatus_remoteStartFails_notifyRemoteStartFailureException() {
        RemoteStartFailures remoteStartFailures = mock(RemoteStartFailures.class);
        when(remoteStartFailures.getRemoteStartFailureErrors()).thenReturn(Collections.singletonList("Error Message"));
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        when(commandStatusResponse.getRemoteStartFailures()).thenReturn(Optional.of(remoteStartFailures));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new RemoteStartFailureException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE, Collections.singletonList("Error Message")));
    }

    @Test
    public void pollCommandStatus_remoteLockFailsBecauseDoubleLock_notifyRemoteLockFailureException() {
        RemoteLockFailure remoteLockFailure = mock(RemoteLockFailure.class);
        when(remoteLockFailure.getRemoteLockFailureError()).thenReturn("LOCK_DBL");
        RemoteLockFailures remoteLockFailures = mock(RemoteLockFailures.class);
        when(remoteLockFailures.getRemoteLockFailureErrors()).thenReturn(Collections.singletonList(remoteLockFailure));
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        when(commandStatusResponse.getRemoteLockFailures()).thenReturn(Optional.of(remoteLockFailures));
        when(commandStatusResponse.getRemoteStartFailures()).thenReturn(Optional.absent());
        when(commandStatusResponse.getTrailerLightCheckFailureReason()).thenReturn(Optional.absent());

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new RemoteLockFailureException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE, Collections.singletonList(remoteLockFailure)));
    }

    @Test
    public void pollCommandStatus_trailerLightCheckFails_notifyTrailerLightCheckException() {
        when(commandStatusResponse.getStatus()).thenReturn(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE);
        when(commandStatusResponse.getRemoteStartFailures()).thenReturn(Optional.absent());
        when(commandStatusResponse.getTrailerLightCheckFailureReason()).thenReturn(Optional.of(TrailerLightCheckException.IGNITION_NOT_ON));

        TestObserver testObserver = subject.pollCommandStatus(VIN, COMMAND_ID, ngsdnVehicleCommandStrategy).test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(ngsdnVehicleCommandStrategy).getCommandStatus(VIN, COMMAND_ID);
        testObserver.assertError(new TrailerLightCheckException(StatusCodes.ERROR_COMMAND_SENT_FAILED_RESPONSE, TrailerLightCheckException.IGNITION_NOT_ON));
    }
}
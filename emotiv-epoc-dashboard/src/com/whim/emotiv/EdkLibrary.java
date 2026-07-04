package com.whim.emotiv;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA binding to the Emotiv EmoEngine (edk.dll).
 *
 * 64-bit target: on x64 there is a single calling convention, so a plain
 * {@link Library} mapping is correct (no StdCallLibrary needed). On 32-bit the
 * classic edk exports are __cdecl, so a plain mapping is right there too.
 *
 * Requires edk.dll (and its dependency DLLs) on the PATH or -Djna.library.path.
 * All enum/return constants live in {@link EdkConstants}.
 */
public interface EdkLibrary extends Library {

    EdkLibrary INSTANCE = Native.load("edk", EdkLibrary.class);

    // --- connection ---
    int  EE_EngineConnect(String strDevID);                    // pass "Emotiv Systems-5"
    int  EE_EngineRemoteConnect(String szHost, short port, String strDevID);
    int  EE_EngineDisconnect();

    // --- handles ---
    Pointer EE_EmoEngineEventCreate();
    void    EE_EmoEngineEventFree(Pointer hEvent);
    Pointer EE_EmoStateCreate();
    void    EE_EmoStateFree(Pointer hState);

    // --- event pump ---
    int EE_EngineGetNextEvent(Pointer hEvent);
    int EE_EmoEngineEventGetType(Pointer hEvent);
    int EE_EmoEngineEventGetUserId(Pointer hEvent, IntByReference pUserId);
    int EE_EmoEngineEventGetEmoState(Pointer hEvent, Pointer hState);

    // --- contact quality ---
    int ES_GetNumContactQualityChannels(Pointer hState);
    int ES_GetContactQuality(Pointer hState, int electrodeIdx);

    // --- power / link telemetry ---
    void ES_GetBatteryChargeLevel(Pointer hState,
                                  IntByReference chargeLevel,
                                  IntByReference maxChargeLevel);
    int  ES_GetWirelessSignalStatus(Pointer hState);           // 0=NONE, 1=BAD, 2=GOOD

    // --- Affectiv / Performance Metrics (all return 0.0f..1.0f) ---
    float ES_AffectivGetEngagementBoredomScore(Pointer hState);
    float ES_AffectivGetExcitementShortTermScore(Pointer hState);
    float ES_AffectivGetExcitementLongTermScore(Pointer hState);
    float ES_AffectivGetMeditationScore(Pointer hState);
    float ES_AffectivGetFrustrationScore(Pointer hState);

    // --- Expressiv / Facial Expressions ---
    int   ES_ExpressivGetUpperFaceAction(Pointer hState);      // EE_ExpressivAlgo_t
    float ES_ExpressivGetUpperFaceActionPower(Pointer hState); // 0..1
    int   ES_ExpressivGetLowerFaceAction(Pointer hState);
    float ES_ExpressivGetLowerFaceActionPower(Pointer hState);
    int   ES_ExpressivIsBlink(Pointer hState);                 // 0/1
    int   ES_ExpressivIsLeftWink(Pointer hState);
    int   ES_ExpressivIsRightWink(Pointer hState);
    int   ES_ExpressivIsLookingLeft(Pointer hState);
    int   ES_ExpressivIsLookingRight(Pointer hState);

    // --- Cognitiv live output (EmoState) ---
    int   ES_CognitivGetCurrentAction(Pointer hState);         // EE_CognitivAction_t
    float ES_CognitivGetCurrentActionPower(Pointer hState);    // 0..1

    // --- Cognitiv training control (EmoEngine + userId) ---
    int EE_CognitivSetActiveActions(int userId, int activeActions); // unsigned long = 32-bit on Win x64
    int EE_CognitivSetTrainingAction(int userId, int action);
    int EE_CognitivSetTrainingControl(int userId, int control);
    int EE_CognitivGetTrainingTime(int userId, IntByReference pTrainingTime);

    // --- Cognitiv training event subtype ---
    int EE_CognitivEventGetType(Pointer hEvent);               // EE_CognitivEvent_t

    // --- User profile persistence ---
    int EE_GetUserProfile(int userId, Pointer hEvent);
    int EE_GetUserProfileSize(Pointer hEvent, IntByReference pSizeOut);
    int EE_GetUserProfileBytes(Pointer hEvent, byte[] destBuffer, int length);
    int EE_SetUserProfile(int userId, byte[] profileBuffer, int length);

    // --- Raw EEG data acquisition (Research / Raw-EEG license) ---
    Pointer EE_DataCreate();
    void    EE_DataFree(Pointer hData);
    int     EE_DataAcquisitionEnable(int userId, int enable);
    int     EE_DataSetBufferSizeInSec(float bufferSizeInSec);
    int     EE_DataUpdateHandle(int userId, Pointer hData);
    int     EE_DataGetNumberOfSample(Pointer hData, IntByReference pNumSamplesOut);
    int     EE_DataGet(Pointer hData, int channel, double[] buffer, int bufferSizeInSamples);
}

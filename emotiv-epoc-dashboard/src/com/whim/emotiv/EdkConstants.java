package com.whim.emotiv;

/**
 * All edk.dll enum/constant values used by the app, consolidated.
 *
 * Confidence tags:
 *   OK      = verified against classic v1 SDK behavior
 *   VERIFY  = confirm against your EmoStateDLL.h / edk.h before trusting labels
 * A wrong VERIFY value only mislabels; it never crashes.
 */
public final class EdkConstants {
    private EdkConstants() {}

    // --- return / event codes (OK) ---
    public static final int EDK_OK                     = 0x0000;
    public static final int EDK_NO_EVENT               = 0x0600;
    public static final int EDK_EMOENGINE_DISCONNECTED = 0x0501;

    // --- EE_Event_t (OK) ---
    public static final int EE_UserAdded       = 0x0010;
    public static final int EE_UserRemoved     = 0x0020;
    public static final int EE_EmoStateUpdated = 0x0040;
    public static final int EE_ProfileEvent    = 0x0080;
    public static final int EE_CognitivEvent   = 0x0100;
    public static final int EE_ExpressivEvent  = 0x0200;

    // --- contact quality value labels (OK) ---
    public static final String[] CQ_TEXT = { "—", "very bad", "poor", "fair", "good" };

    // --- contact-quality channel labels, index-ordered (VERIFY: 16 vs 18) ---
    public static final String[] CHANNEL_NAMES = {
        "CMS", "DRL", "AF3", "F7", "F3", "FC5", "T7", "P7",
        "O1", "O2", "P8", "T8", "FC6", "F4", "F8", "AF4"
    };

    // --- wireless signal labels (OK) ---
    public static final String[] SIGNAL_TEXT = { "no link", "weak", "good" };

    // --- Expressiv algo bitmask (set OK; hex VERIFY) ---
    public static final int EXP_NEUTRAL     = 0x0001;
    public static final int EXP_EYEBROW     = 0x0020;
    public static final int EXP_FURROW      = 0x0040;
    public static final int EXP_SMILE       = 0x0080;
    public static final int EXP_CLENCH      = 0x0100;
    public static final int EXP_LAUGH       = 0x0200;
    public static final int EXP_SMIRK_LEFT  = 0x0400;
    public static final int EXP_SMIRK_RIGHT = 0x0800;

    // --- Cognitiv actions (set OK; hex VERIFY) ---
    public static final int COG_NEUTRAL = 0x0001;
    public static final int COG_PUSH    = 0x0002;
    public static final int COG_PULL    = 0x0004;
    public static final int COG_LIFT    = 0x0008;
    public static final int COG_DROP    = 0x0010;
    public static final int COG_LEFT    = 0x0020;
    public static final int COG_RIGHT   = 0x0040;

    // --- Cognitiv training control (OK) ---
    public static final int COG_CTRL_START  = 1;
    public static final int COG_CTRL_ACCEPT = 2;
    public static final int COG_CTRL_REJECT = 3;
    public static final int COG_CTRL_ERASE  = 4;

    // --- Cognitiv training-event subtypes, index order (VERIFY ordering) ---
    public static final String[] COG_EVENTS = {
        "no-event", "training started", "training SUCCEEDED — accept or reject",
        "training FAILED", "training completed", "training data erased",
        "training rejected", "training reset", "auto neutral done", "signature updated"
    };

    // --- Raw EEG channels (ED_*), name-paired. VERIFY hex + presence in edk.h ---
    public static final int[] EEG_CHANNELS = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
    public static final String[] EEG_NAMES = {
        "AF3", "F7", "F3", "FC5", "T7", "P7", "O1", "O2", "P8", "T8", "FC6", "F4", "F8", "AF4"
    };
    public static final float RAW_BUFFER_SECONDS = 1.0f;
    public static final int   RAW_SAMPLE_HZ      = 128;

    // --- EmoComposer emulator endpoint + device id (OK) ---
    public static final String EMOCOMPOSER_HOST = "127.0.0.1";
    public static final short  EMOCOMPOSER_PORT = 1726;
    public static final String DEVICE_ID        = "Emotiv Systems-5";
}

package com.whim.emotiv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.SwingUtilities;

/**
 * Owns all native EmoEngine state and runs the event pump on a single daemon
 * thread, with connection supervision (auto-reconnect), user lifecycle,
 * Affectiv / Expressiv / Cognitiv reads, Cognitiv training via a UI->engine
 * command queue, user-profile persistence, and optional raw-EEG streaming.
 *
 * HARD RULE: every edk call in this file runs on the poller thread only.
 * Results are published to the listener on the EDT via invokeLater.
 */
public class EmoEnginePoller implements Runnable {

    private static final long RECONNECT_BACKOFF_MS = 2000L;

    /** Immutable snapshot handed to the EDT once per EmoState update. */
    public static final class Snapshot {
        public final String[] names;
        public final int[] quality;      // 0..4
        public final int battery;        // current charge
        public final int batteryMax;     // max charge (EPOC v1 ~5)
        public final int signal;         // 0=none,1=bad,2=good

        public final float engagement, excitementST, excitementLT, meditation, frustration;

        public final String upperFace, lowerFace;
        public final float upperPower, lowerPower;
        public final boolean blink, winkL, winkR, lookL, lookR;

        public final String cognitivAction;
        public final float  cognitivPower;

        Snapshot(String[] n, int[] q, int b, int bm, int s,
                 float eng, float exST, float exLT, float med, float fr,
                 String uf, float up, String lf, float lp,
                 boolean bl, boolean wl, boolean wr, boolean ll, boolean lr,
                 String cog, float cogP) {
            names = n; quality = q; battery = b; batteryMax = bm; signal = s;
            engagement = eng; excitementST = exST; excitementLT = exLT;
            meditation = med; frustration = fr;
            upperFace = uf; upperPower = up; lowerFace = lf; lowerPower = lp;
            blink = bl; winkL = wl; winkR = wr; lookL = ll; lookR = lr;
            cognitivAction = cog; cognitivPower = cogP;
        }
    }

    public interface Listener {
        void onStatus(String message);
        void onSnapshot(Snapshot s);
        void onDisconnected();
        void onTrainingEvent(String message);        // Cognitiv training feedback
        void onRawEeg(String[] names, double[][] samples); // samples[ch][i], µV
    }

    private final Listener listener;
    private final boolean useEmoComposer;

    private volatile boolean running = false;
    private Thread thread;
    private boolean channelCountLogged = false;

    private volatile int currentUserId = -1;

    // Raw EEG
    private Pointer hData;
    private int rawGate = 0;
    private boolean rawEnabled = false;
    private volatile boolean wantRaw = false;

    // Profiles
    private volatile java.io.File autoLoadFile;

    public EmoEnginePoller(Listener listener) { this(listener, false); }

    public EmoEnginePoller(Listener listener, boolean useEmoComposer) {
        this.listener = listener;
        this.useEmoComposer = useEmoComposer;
    }

    public void setRawEnabled(boolean on) { this.wantRaw = on; }
    public void setAutoLoadProfile(java.io.File f) { this.autoLoadFile = f; }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "EmoEngine-Poller");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) { thread.interrupt(); thread = null; }
    }

    // ---------------------------------------------------------------- run loop

    @Override
    public void run() {
        try {
            while (running) {                       // supervisory / reconnect loop
                boolean clean = pumpSession();      // returns on disconnect or stop
                if (!running) break;
                fireStatus(clean ? "Reconnecting…" : "Headset lost — retrying…");
                sleepQuiet(RECONNECT_BACKOFF_MS);
            }
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { listener.onDisconnected(); }
            });
        }
    }

    /** One connect+pump session. Returns true if it ended cleanly (stop requested). */
    private boolean pumpSession() {
        EdkLibrary edk = EdkLibrary.INSTANCE;
        Pointer hEvent = null;
        Pointer hState = null;
        boolean connected = false;

        try {
            int rc = useEmoComposer
                ? edk.EE_EngineRemoteConnect(EdkConstants.EMOCOMPOSER_HOST,
                                             EdkConstants.EMOCOMPOSER_PORT, EdkConstants.DEVICE_ID)
                : edk.EE_EngineConnect(EdkConstants.DEVICE_ID);
            connected = (rc == EdkConstants.EDK_OK);
            fireStatus(connected
                ? ("Connected via " + (useEmoComposer ? "EmoComposer" : "headset") + " — waiting…")
                : ("Connect failed rc=0x" + Integer.toHexString(rc)));
            if (!connected) return false;

            hEvent = edk.EE_EmoEngineEventCreate();
            hState = edk.EE_EmoStateCreate();
            hData  = edk.EE_DataCreate();
            edk.EE_DataSetBufferSizeInSec(EdkConstants.RAW_BUFFER_SECONDS);

            IntByReference userId = new IntByReference(0);
            boolean haveUser = false;

            while (running) {
                int state = edk.EE_EngineGetNextEvent(hEvent);

                if (state == EdkConstants.EDK_OK) {
                    int type = edk.EE_EmoEngineEventGetType(hEvent);
                    edk.EE_EmoEngineEventGetUserId(hEvent, userId);

                    if (type == EdkConstants.EE_UserAdded) {
                        haveUser = true;
                        currentUserId = userId.getValue();
                        fireStatus("Headset attached (user " + currentUserId + ")");
                        maybeAutoLoadProfile(edk);
                        maybeEnableRaw(edk);
                    } else if (type == EdkConstants.EE_UserRemoved) {
                        haveUser = false;
                        fireStatus("Headset removed / powered off");
                    } else if (type == EdkConstants.EE_CognitivEvent) {
                        int sub = edk.EE_CognitivEventGetType(hEvent);
                        String msg = (sub >= 0 && sub < EdkConstants.COG_EVENTS.length)
                                ? EdkConstants.COG_EVENTS[sub] : ("cognitiv event " + sub);
                        fireTraining(msg);
                    } else if (type == EdkConstants.EE_EmoStateUpdated && haveUser) {
                        edk.EE_EmoEngineEventGetEmoState(hEvent, hState);
                        publish(edk, hState);
                    }
                } else if (state == EdkConstants.EDK_EMOENGINE_DISCONNECTED) {
                    return false;                    // trigger reconnect
                } else if (state != EdkConstants.EDK_NO_EVENT) {
                    fireStatus("GetNextEvent rc=0x" + Integer.toHexString(state));
                }

                drainCommands(edk);

                if (rawEnabled && currentUserId >= 0 && (++rawGate % 8 == 0)) {
                    pumpRaw(edk);
                }

                sleepQuiet(15);
                if (Thread.currentThread().isInterrupted()) break;
            }
            return true;                             // stop() requested
        } catch (Throwable t) {
            fireStatus("Native error: " + t.getMessage());
            return false;
        } finally {
            if (hData  != null) edk.EE_DataFree(hData);
            if (hState != null) edk.EE_EmoStateFree(hState);
            if (hEvent != null) edk.EE_EmoEngineEventFree(hEvent);
            if (connected) edk.EE_EngineDisconnect();
            hData = null; rawEnabled = false; rawGate = 0; currentUserId = -1;
        }
    }

    // ---------------------------------------------------------------- publish

    private void publish(EdkLibrary edk, Pointer hState) {
        int n = edk.ES_GetNumContactQualityChannels(hState);
        if (n <= 0) return;
        if (!channelCountLogged) {
            channelCountLogged = true;
            fireStatus("Contact-quality channels reported: " + n
                       + " (verify labels against EmoStateDLL.h)");
        }

        int[] q = new int[n];
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            q[i] = edk.ES_GetContactQuality(hState, i);
            names[i] = (i < EdkConstants.CHANNEL_NAMES.length)
                    ? EdkConstants.CHANNEL_NAMES[i] : ("CH" + i);
        }

        IntByReference charge = new IntByReference(0);
        IntByReference chargeMax = new IntByReference(0);
        edk.ES_GetBatteryChargeLevel(hState, charge, chargeMax);
        int signal = edk.ES_GetWirelessSignalStatus(hState);

        float eng  = edk.ES_AffectivGetEngagementBoredomScore(hState);
        float exST = edk.ES_AffectivGetExcitementShortTermScore(hState);
        float exLT = edk.ES_AffectivGetExcitementLongTermScore(hState);
        float med  = edk.ES_AffectivGetMeditationScore(hState);
        float fr   = edk.ES_AffectivGetFrustrationScore(hState);

        String uf = expressivName(edk.ES_ExpressivGetUpperFaceAction(hState));
        float  up = edk.ES_ExpressivGetUpperFaceActionPower(hState);
        String lf = expressivName(edk.ES_ExpressivGetLowerFaceAction(hState));
        float  lp = edk.ES_ExpressivGetLowerFaceActionPower(hState);
        boolean bl = edk.ES_ExpressivIsBlink(hState) != 0;
        boolean wl = edk.ES_ExpressivIsLeftWink(hState) != 0;
        boolean wr = edk.ES_ExpressivIsRightWink(hState) != 0;
        boolean ll = edk.ES_ExpressivIsLookingLeft(hState) != 0;
        boolean lr = edk.ES_ExpressivIsLookingRight(hState) != 0;

        String cog = cognitivName(edk.ES_CognitivGetCurrentAction(hState));
        float  cogP = edk.ES_CognitivGetCurrentActionPower(hState);

        final Snapshot snap = new Snapshot(names, q,
                charge.getValue(), chargeMax.getValue(), signal,
                eng, exST, exLT, med, fr,
                uf, up, lf, lp, bl, wl, wr, ll, lr,
                cog, cogP);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { listener.onSnapshot(snap); }
        });
    }

    // ---------------------------------------------------------------- Cognitiv commands

    private interface EngineCommand { void run(EdkLibrary edk, int userId); }

    private final java.util.concurrent.ConcurrentLinkedQueue<EngineCommand> commands
        = new java.util.concurrent.ConcurrentLinkedQueue<EngineCommand>();

    private void drainCommands(EdkLibrary edk) {
        EngineCommand cmd;
        while (currentUserId >= 0 && (cmd = commands.poll()) != null) {
            cmd.run(edk, currentUserId);
        }
    }

    /** UI-facing: enable which actions the detector runs (bitmask incl. COG_NEUTRAL). */
    public void cognitivSetActive(final int actionMask) {
        commands.add(new EngineCommand() {
            public void run(EdkLibrary edk, int userId) {
                int rc = edk.EE_CognitivSetActiveActions(userId, actionMask);
                fireStatus("SetActiveActions rc=0x" + Integer.toHexString(rc));
            }
        });
    }

    /** UI-facing: pick the action to train and issue a control (START/ACCEPT/REJECT). */
    public void cognitivTrain(final int action, final int control) {
        commands.add(new EngineCommand() {
            public void run(EdkLibrary edk, int userId) {
                if (action != 0) edk.EE_CognitivSetTrainingAction(userId, action);
                int rc = edk.EE_CognitivSetTrainingControl(userId, control);
                fireStatus("TrainingControl rc=0x" + Integer.toHexString(rc));
            }
        });
    }

    // ---------------------------------------------------------------- Profiles

    /** UI-facing: save the current user's profile to disk (runs on poller thread). */
    public void cognitivSaveProfile(final java.io.File file) {
        commands.add(new EngineCommand() {
            public void run(EdkLibrary edk, int userId) {
                Pointer eProfile = edk.EE_EmoEngineEventCreate();
                try {
                    edk.EE_GetUserProfile(userId, eProfile);
                    IntByReference size = new IntByReference(0);
                    edk.EE_GetUserProfileSize(eProfile, size);
                    int nBytes = size.getValue();
                    if (nBytes <= 0) {
                        fireStatus("Profile size 0 — your SDK fetches profiles "
                                 + "asynchronously; async path not yet wired.");
                        return;
                    }
                    byte[] buf = new byte[nBytes];
                    edk.EE_GetUserProfileBytes(eProfile, buf, nBytes);
                    java.io.FileOutputStream out = new java.io.FileOutputStream(file);
                    try { out.write(buf); } finally { out.close(); }
                    fireStatus("Profile saved (" + nBytes + " bytes) → " + file.getName());
                } catch (Throwable t) {
                    fireStatus("Save failed: " + t.getMessage());
                } finally {
                    edk.EE_EmoEngineEventFree(eProfile);
                }
            }
        });
    }

    /** UI-facing: load raw profile bytes into the active user. */
    public void cognitivLoadProfile(final byte[] data) {
        commands.add(new EngineCommand() {
            public void run(EdkLibrary edk, int userId) {
                if (data == null || data.length == 0) { fireStatus("Empty profile."); return; }
                int rc = edk.EE_SetUserProfile(userId, data, data.length);
                fireStatus("SetUserProfile rc=0x" + Integer.toHexString(rc)
                           + " (" + data.length + " bytes)");
            }
        });
    }

    private void maybeAutoLoadProfile(EdkLibrary edk) {
        java.io.File f = autoLoadFile;
        if (f == null || !f.isFile()) return;
        try {
            byte[] data = readAll(f);
            int rc = edk.EE_SetUserProfile(currentUserId, data, data.length);
            fireStatus("Auto-loaded profile " + f.getName()
                       + " rc=0x" + Integer.toHexString(rc));
        } catch (Throwable t) {
            fireStatus("Auto-load failed: " + t.getMessage());
        }
    }

    // ---------------------------------------------------------------- Raw EEG

    private void maybeEnableRaw(EdkLibrary edk) {
        if (!wantRaw) return;
        int rc = edk.EE_DataAcquisitionEnable(currentUserId, 1);
        rawEnabled = (rc == EdkConstants.EDK_OK);
        fireStatus("Raw EEG acquire " + (rawEnabled ? "enabled" : "FAILED")
                   + " rc=0x" + Integer.toHexString(rc));
    }

    private void pumpRaw(EdkLibrary edk) {
        edk.EE_DataUpdateHandle(currentUserId, hData);
        IntByReference nRef = new IntByReference(0);
        edk.EE_DataGetNumberOfSample(hData, nRef);
        final int n = nRef.getValue();
        if (n <= 0) return; // no samples => likely no Raw-EEG license

        final int ch = EdkConstants.EEG_CHANNELS.length;
        final double[][] out = new double[ch][];
        for (int c = 0; c < ch; c++) {
            double[] buf = new double[n];
            edk.EE_DataGet(hData, EdkConstants.EEG_CHANNELS[c], buf, n);
            out[c] = buf;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { listener.onRawEeg(EdkConstants.EEG_NAMES, out); }
        });
    }

    // ---------------------------------------------------------------- helpers

    static String expressivName(int algo) {
        switch (algo) {
            case EdkConstants.EXP_EYEBROW:     return "raise brow";
            case EdkConstants.EXP_FURROW:      return "furrow";
            case EdkConstants.EXP_SMILE:       return "smile";
            case EdkConstants.EXP_CLENCH:      return "clench";
            case EdkConstants.EXP_LAUGH:       return "laugh";
            case EdkConstants.EXP_SMIRK_LEFT:  return "smirk L";
            case EdkConstants.EXP_SMIRK_RIGHT: return "smirk R";
            case EdkConstants.EXP_NEUTRAL:     return "neutral";
            default:                           return "neutral";
        }
    }

    static String cognitivName(int a) {
        switch (a) {
            case EdkConstants.COG_PUSH:    return "PUSH";
            case EdkConstants.COG_PULL:    return "PULL";
            case EdkConstants.COG_LIFT:    return "LIFT";
            case EdkConstants.COG_DROP:    return "DROP";
            case EdkConstants.COG_LEFT:    return "LEFT";
            case EdkConstants.COG_RIGHT:   return "RIGHT";
            case EdkConstants.COG_NEUTRAL: return "neutral";
            default:                       return "neutral";
        }
    }

    private void fireStatus(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { listener.onStatus(msg); }
        });
    }

    private void fireTraining(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { listener.onTrainingEvent(msg); }
        });
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] readAll(java.io.File f) throws java.io.IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            byte[] buf = new byte[(int) f.length()];
            int off = 0, r;
            while (off < buf.length && (r = in.read(buf, off, buf.length - off)) > 0) off += r;
            return buf;
        } finally { in.close(); }
    }
}

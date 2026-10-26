package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.data.Vitals;

public class ClientState {
    public static final java.util.concurrent.ConcurrentLinkedQueue<Integer> portals = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static final java.util.Map<Integer, Long> bandaged = new java.util.concurrent.ConcurrentHashMap<>();
    public static float water = Vitals.MAX_WATER;
    public static int bleed;
    public static int fxKind;
    public static int fxN;
    public static boolean journey;

    public static void apply(VitalsPayload p) {
        water = p.water();
        bleed = p.bleed();
        fxKind = p.fxKind();
        fxN = p.fxN();
        if (p.journey()) journey = true;
    }

    public static void reset() {
        water = Vitals.MAX_WATER;
        bleed = 0;
        fxKind = 0;
        fxN = 0;
        journey = false;
        portals.clear();
        bandaged.clear();
    }
}

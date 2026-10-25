package dev.zedkodex.modehud.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class Vitals implements INBTSerializable<CompoundTag> {
    public static final float MAX_WATER = 20f;

    public static final int FX_HIT = 1;
    public static final int FX_HUNGER = 2;
    public static final int FX_THIRST = 3;
    public static final int FX_BLEED = 4;
    public static final int FX_DROWN = 5;
    public static final int FX_HEAL = 6;

    public float water = MAX_WATER;
    public int bleed;
    public float debt;
    public boolean journeyShown;

    public double exhaust;
    public int bleedTimer;
    public int clotTimer;
    public int thirstTimer;
    public int regenTimer;

    public int fxKind;
    public int fxN;

    public int sentWater = -1;
    public int sentBleed = -1;
    public int sentFxN = -1;

    public void fx(int kind) {
        fxKind = kind;
        fxN++;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putFloat("water", water);
        t.putInt("bleed", bleed);
        t.putFloat("debt", debt);
        t.putBoolean("journey", journeyShown);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        water = t.contains("water") ? t.getFloat("water") : MAX_WATER;
        bleed = t.getInt("bleed");
        debt = t.getFloat("debt");
        journeyShown = t.getBoolean("journey");
    }
}

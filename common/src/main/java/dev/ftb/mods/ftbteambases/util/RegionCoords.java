package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.math.XZ;

public record RegionCoords(int x, int z) {
    public static final Codec<RegionCoords> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("x").forGetter(RegionCoords::x),
            Codec.INT.fieldOf("z").forGetter(RegionCoords::z)
    ).apply(inst, RegionCoords::new));

    public RegionCoords offsetBy(int xOff, int zOff) {
        return new RegionCoords(x + xOff, z + zOff);
    }

    public RegionCoords offsetBy(XZ offset) {
        return new RegionCoords(x + offset.x(), z + offset.z());
    }

    public String filename() {
        return String.format("r.%d.%d.mca", x, z);
    }

    @Override
    public String toString() {
        return "[" + x + "," + z + "]";
    }
}

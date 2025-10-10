package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.apache.commons.lang3.Validate;

public record RegionCoords(int x, int z) {
    public static final Codec<RegionCoords> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("x").forGetter(RegionCoords::x),
            Codec.INT.fieldOf("z").forGetter(RegionCoords::z)
    ).apply(inst, RegionCoords::new));
    public static final Codec<RegionCoords> STRING_CODEC = Codec.STRING.comapFlatMap(str -> {
        try {
            return DataResult.success(RegionCoords.fromString(str));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, RegionCoords::toString);

    public RegionCoords offsetBy(int xOff, int zOff) {
        return new RegionCoords(x + xOff, z + zOff);
    }

    public RegionCoords offsetBy(XZ offset) {
        return new RegionCoords(x + offset.x(), z + offset.z());
    }

    public String filename() {
        return String.format("r.%d.%d.mca", x, z);
    }

    public BlockPos getBlockPos(Vec3i offset) {
        return new BlockPos((x << 9) + offset.getX(), offset.getY(), (z << 9) + offset.getZ());
    }

    @Override
    public String toString() {
        return "[" + x + "," + z + "]";
    }

    /**
     * Parse a string representation of the coords, as produced by {@link #toString()}
     * @param str the string
     * @return a new RegionCoords object
     * @throws IllegalArgumentException if the string is not parseable
     */
    public static RegionCoords fromString(String str) throws IllegalArgumentException {
        Validate.isTrue(str.startsWith("[") && str.endsWith("]"), "Malformed string: " + str);
        String[] parts = str.substring(1, str.length() - 1).split(",", 2);
        Validate.isTrue(parts.length == 2, "Malformed string: " + str);
        return new RegionCoords(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}

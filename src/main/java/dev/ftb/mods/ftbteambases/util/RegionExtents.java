package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.math.XZ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record RegionExtents(RegionCoords start, RegionCoords end) {
    public static final Codec<RegionExtents> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegionCoords.CODEC.fieldOf("start").forGetter(RegionExtents::start),
            RegionCoords.CODEC.fieldOf("end").forGetter(RegionExtents::end)
    ).apply(inst, RegionExtents::new));

    public XZ getSize() {
        return XZ.of((end.x() - start.x()) + 1, (end.z() - start.z()) + 1);
    }

    @Override
    public String toString() {
        return start.toString() + " -> " + end.toString();
    }

    public String asBlockPosString() {
        XZ s = XZ.of(start.x() * 512, start.z() * 512);
        XZ e = XZ.of(end.x() * 512 + 511, end.z() * 512 + 511);
        return s + " -> " + e;
    }

    public Collection<Path> files(Path parentDir) {
        List<Path> res = new ArrayList<>();
        for (int x = start.x(); x <= end.x(); x++) {
            for (int z = start.z(); z <= end.z(); z++) {
                res.add(parentDir.resolve(String.format("r.%d.%d.mca", x, z)));
            }
        }
        return res;
    }
}

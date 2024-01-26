package dev.ftb.mods.ftbteambases.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dev.ftb.mods.ftbteambases.util.RegionFileRelocator.PREGEN_PATH;

public class RegionFileUtil {
    private static final Pattern MCA_FILE = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$", Pattern.CASE_INSENSITIVE);

    public static Optional<RegionCoords> getRegionCoords(Path file) {
        Matcher m = MCA_FILE.matcher(file.getFileName().toString());
        if (m.matches()) {
            try {
                int rx0 = Integer.parseInt(m.group(1));
                int rz0 = Integer.parseInt(m.group(2));
                return Optional.of(new RegionCoords(rx0, rz0));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    public static Optional<RegionExtents> getRegionExtents(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            MutableInt minX = new MutableInt(Integer.MAX_VALUE);
            MutableInt maxX = new MutableInt(Integer.MIN_VALUE);
            MutableInt minZ = new MutableInt(Integer.MAX_VALUE);
            MutableInt maxZ = new MutableInt(Integer.MIN_VALUE);
            files.forEach(file -> getRegionCoords(file).ifPresent(r -> {
                minX.setValue(Math.min(r.x(), minX.intValue()));
                minZ.setValue(Math.min(r.z(), minZ.intValue()));
                maxX.setValue(Math.max(r.x(), maxX.intValue()));
                maxZ.setValue(Math.max(r.z(), maxZ.intValue()));
            }));
            return Optional.of(new RegionExtents(new RegionCoords(minX.intValue(), minZ.intValue()), new RegionCoords(maxX.intValue(), maxZ.intValue())));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public static Path getPregenPath(String templateId, MinecraftServer server) {
        return server.getServerDirectory().toPath()
                .resolve(PREGEN_PATH).resolve(templateId);
    }

    @NotNull
    public static Path getPathForDimension(MinecraftServer server, ResourceKey<Level> levelKey, String subDirectory) {
        Path levelDataDir = server.getWorldPath(LevelResource.ROOT);
        if (Level.OVERWORLD.equals(levelKey)) {
            return levelDataDir.resolve(subDirectory);
        } else if (Level.NETHER.equals(levelKey)) {
            return levelDataDir.resolve("DIM-1").resolve(subDirectory);
        } else if (Level.END.equals(levelKey)) {
            return levelDataDir.resolve("DIM1").resolve(subDirectory);
        } else {
            return levelDataDir.resolve("dimensions").resolve(levelKey.location().getNamespace()).resolve(levelKey.location().getPath()).resolve(subDirectory);
        }
    }
}

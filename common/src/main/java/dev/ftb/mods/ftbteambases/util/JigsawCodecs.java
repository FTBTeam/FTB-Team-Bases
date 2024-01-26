package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.core.FrontAndTop;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

import java.util.Locale;

public class JigsawCodecs {
    public static final Codec<FrontAndTop> FRONT_AND_TOP = StringRepresentable.fromEnum(FrontAndTop::values);

    public static final Codec<JigsawBlockEntity.JointType> JOINT_TYPE = StringRepresentable.fromEnum(JigsawBlockEntity.JointType::values);

    private static FrontAndTop frontAndTop(String name) {
        try {
            return FrontAndTop.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            FTBTeamBases.LOGGER.warn("invalid value {} for FrontAndTop: defaulting to 'up_north'", name);
            e.printStackTrace();
            return FrontAndTop.UP_NORTH;
        }
    }
}

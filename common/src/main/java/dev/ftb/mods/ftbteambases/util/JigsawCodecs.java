package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.FrontAndTop;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class JigsawCodecs {
    public static final Codec<FrontAndTop> FRONT_AND_TOP = StringRepresentable.fromEnum(FrontAndTop::values);

    public static final Codec<JigsawBlockEntity.JointType> JOINT_TYPE = StringRepresentable.fromEnum(JigsawBlockEntity.JointType::values);
}

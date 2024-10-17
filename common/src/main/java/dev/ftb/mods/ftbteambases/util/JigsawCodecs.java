package dev.ftb.mods.ftbteambases.util;

import com.mojang.serialization.Codec;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;

public class JigsawCodecs {
    public static final Codec<FrontAndTop> FRONT_AND_TOP = StringRepresentable.fromEnum(FrontAndTop::values);
    public static final StreamCodec<FriendlyByteBuf,FrontAndTop> FRONT_AND_TOP_STREAM_CODEC = NetworkHelper.enumStreamCodec(FrontAndTop.class);

    public static final Codec<JointType> JOINT_TYPE = StringRepresentable.fromEnum(JointType::values);
    public static final StreamCodec<FriendlyByteBuf, JointType> JOINT_TYPE_STREAM_CODEC = NetworkHelper.enumStreamCodec(JointType.class);
}

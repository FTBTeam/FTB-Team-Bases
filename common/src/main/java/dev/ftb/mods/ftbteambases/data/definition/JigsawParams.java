package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteambases.util.JigsawCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

import java.util.Optional;

public record JigsawParams(ResourceLocation templatePool, ResourceLocation target, Optional<Integer> yPos, int maxGenerationDepth,
                           Optional<BlockPos> generationOffset, FrontAndTop jigsawOrientation,
                           JigsawBlockEntity.JointType jointType, String finalState) implements INetworkWritable<JigsawParams> {
    public static final Codec<JigsawParams> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("template_pool").forGetter(JigsawParams::templatePool),
            ResourceLocation.CODEC.fieldOf("target").forGetter(JigsawParams::target),
            Codec.INT.optionalFieldOf("y_pos").forGetter(JigsawParams::yPos),
            Codec.intRange(1, 20).fieldOf("max_gen_depth").forGetter(JigsawParams::maxGenerationDepth),
            BlockPos.CODEC.optionalFieldOf("generation_offset").forGetter(JigsawParams::generationOffset),
            JigsawCodecs.FRONT_AND_TOP.optionalFieldOf("orientation", FrontAndTop.UP_NORTH).forGetter(JigsawParams::jigsawOrientation),
            JigsawCodecs.JOINT_TYPE.optionalFieldOf("joint_type", JigsawBlockEntity.JointType.ROLLABLE).forGetter(JigsawParams::jointType),
            Codec.STRING.optionalFieldOf("final_state", "minecraft:air").forGetter(JigsawParams::finalState)
    ).apply(inst, JigsawParams::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, JigsawParams> STREAM_CODEC = NetworkHelper.composite(
            ResourceLocation.STREAM_CODEC, JigsawParams::templatePool,
            ResourceLocation.STREAM_CODEC, JigsawParams::target,
            ByteBufCodecs.optional(ByteBufCodecs.INT), JigsawParams::yPos,
            ByteBufCodecs.INT, JigsawParams::maxGenerationDepth,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), JigsawParams::generationOffset,
            JigsawCodecs.FRONT_AND_TOP_STREAM_CODEC, JigsawParams::jigsawOrientation,
            JigsawCodecs.JOINT_TYPE_STREAM_CODEC, JigsawParams::jointType,
            ByteBufCodecs.STRING_UTF8, JigsawParams::finalState,
            JigsawParams::new
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, JigsawParams> streamCodec() {
        return STREAM_CODEC;
    }
}

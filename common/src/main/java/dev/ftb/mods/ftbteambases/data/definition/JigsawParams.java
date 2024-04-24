package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftbteambases.util.JigsawCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

import java.util.Optional;

public record JigsawParams(ResourceLocation templatePool, ResourceLocation target, Optional<Integer> yPos, int maxGenerationDepth,
                           Optional<BlockPos> generationOffset, FrontAndTop jigsawOrientation,
                           JigsawBlockEntity.JointType jointType, String finalState) implements INetworkWritable {
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

    public static JigsawParams fromBytes(FriendlyByteBuf buf) {
        ResourceLocation templatePool = buf.readResourceLocation();
        ResourceLocation target = buf.readResourceLocation();
        Optional<Integer> yPos = buf.readOptional(FriendlyByteBuf::readInt);
        int maxGen = buf.readVarInt();
        Optional<BlockPos> offset = buf.readOptional(FriendlyByteBuf::readBlockPos);
        FrontAndTop orientation = buf.readEnum(FrontAndTop.class);
        JigsawBlockEntity.JointType jointType = buf.readEnum(JigsawBlockEntity.JointType.class);
        String finalState = buf.readUtf();

        return new JigsawParams(templatePool, target, yPos, maxGen, offset, orientation, jointType, finalState);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(templatePool);
        buf.writeResourceLocation(target);
        buf.writeOptional(yPos, FriendlyByteBuf::writeInt);
        buf.writeVarInt(maxGenerationDepth);
        buf.writeOptional(generationOffset, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(jigsawOrientation);
        buf.writeEnum(jointType);
        buf.writeUtf(finalState);
    }
}

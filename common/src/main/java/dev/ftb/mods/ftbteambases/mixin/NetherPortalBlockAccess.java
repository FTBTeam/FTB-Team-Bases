package dev.ftb.mods.ftbteambases.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetherPortalBlock.class)
public interface NetherPortalBlockAccess {
    @Invoker
    DimensionTransition invokeGetExitPortal(ServerLevel serverLevel, Entity entity, BlockPos blockPos, BlockPos blockPos2, boolean bl, WorldBorder worldBorder);
}

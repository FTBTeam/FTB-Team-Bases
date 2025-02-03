package dev.ftb.mods.ftbteambases.mixin;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.util.NetherPortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
@Debug(export = true)
public abstract class NetherPortalBlockMixin {
    @Inject(method="getPortalDestination", at = @At("HEAD"), cancellable = true)
    public void onGetPortalDestination(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<DimensionTransition> cir) {
        if (ServerConfig.TEAM_SPECIFIC_NETHER_ENTRY_POINT.get()) {
            DimensionTransition transition = NetherPortalPlacement.getTeamEntryPoint(serverLevel, entity, blockPos);
            if (transition != null) {
                cir.setReturnValue(transition);
            }
        }
    }

    @ModifyVariable(method = "getExitPortal", at = @At("HEAD"), argsOnly = true, index = 4)
    private BlockPos modifyExitPos(BlockPos exitPos) {
        return !ServerConfig.TEAM_SPECIFIC_NETHER_ENTRY_POINT.get() && ServerConfig.USE_CUSTOM_PORTAL_Y_POS.get() ?
                new BlockPos(exitPos.getX(), ServerConfig.CUSTOM_PORTAL_Y_POS.get(), exitPos.getZ()) :
                exitPos;
    }
}

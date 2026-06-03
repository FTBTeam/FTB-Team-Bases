package dev.ftb.mods.ftbteambases.mixin;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.util.NetherPortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
@Debug(export = true)
public abstract class NetherPortalBlockMixin {
    @Inject(method="getPortalDestination", at = @At("HEAD"), cancellable = true)
    public void onGetPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos, CallbackInfoReturnable<TeleportTransition> cir) {
        if (ServerConfig.TEAM_SPECIFIC_NETHER_ENTRY_POINT.get()) {
            TeleportTransition transition = NetherPortalPlacement.getTeamEntryPoint(currentLevel, entity, portalEntryPos);
            if (transition != null) {
                cir.setReturnValue(transition);
            }
        }
    }

    @ModifyVariable(method = "getExitPortal", at = @At("HEAD"), argsOnly = true, name = "approximateExitPos")
    private BlockPos modifyExitPos(BlockPos approximateExitPos) {
        return !ServerConfig.TEAM_SPECIFIC_NETHER_ENTRY_POINT.get() && ServerConfig.USE_CUSTOM_PORTAL_Y_POS.get() ?
                new BlockPos(approximateExitPos.getX(), ServerConfig.CUSTOM_PORTAL_Y_POS.get(), approximateExitPos.getZ()) :
                approximateExitPos;
    }
}

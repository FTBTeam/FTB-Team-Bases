package dev.ftb.mods.ftbteambases.mixin;

import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.util.NetherPortalPlacement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method="findDimensionEntryPoint", at=@At("HEAD"), cancellable = true)
    private void onFindDimensionEntryPoint(ServerLevel destination, CallbackInfoReturnable<PortalInfo> cir) {
        if (destination.dimension() == Level.NETHER && ServerConfig.TEAM_NETHER_ENTRY_POINT.get()) {
            cir.setReturnValue(NetherPortalPlacement.teamSpecificEntryPoint(destination, (ServerPlayer) (Object) this));
        }
    }
}

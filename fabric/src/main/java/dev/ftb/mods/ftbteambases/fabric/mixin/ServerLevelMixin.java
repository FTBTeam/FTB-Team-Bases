package dev.ftb.mods.ftbteambases.fabric.mixin;

import dev.ftb.mods.ftbteambases.util.MiscUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Shadow @Final private ServerLevelData serverLevelData;

    @Inject(method="tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    public void onTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            long newTime = this.serverLevelData.getDayTime() + 24000L;
            MiscUtil.setOverworldTime(serverLevel.getServer(), newTime - newTime % 24000L);
        }
    }
}

package dev.ftb.mods.ftbteambases.mixin.client;

import dev.ftb.mods.ftbteambases.client.VoidTeamLevelData;
import dev.ftb.mods.ftbteambases.config.ClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin implements VoidTeamLevelData {
    @Unique
    private boolean ftb$voidTeamDimension;

    @Override
    public boolean isFtb$voidTeamDimension() {
        return ftb$voidTeamDimension;
    }

    @Override
    public void ftb$setVoidTeamDimension() {
        this.ftb$voidTeamDimension = true;
    }

    @Inject(at = @At("HEAD"), method = "getHorizonHeight", cancellable = true)
    private void onGetHorizonHeight(CallbackInfoReturnable<Double> cir) {
        if (ftb$voidTeamDimension) {
            cir.setReturnValue(ClientConfig.VOID_HORIZON.get());
        }
    }

    @Inject(at = @At("HEAD"), method = "getClearColorScale", cancellable = true)
    private void onGetClearColorScale(CallbackInfoReturnable<Float> cir) {
        if (ftb$voidTeamDimension && ClientConfig.HIDE_VOID_FOG.get()) {
            cir.setReturnValue(1.0F);
        }
    }
}

package dev.ftb.mods.ftbteambases.mixin;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @ModifyVariable(method="placeNewPlayer", at=@At(value = "STORE"))
    private ResourceKey<Level> onPlaceNewPlayer(ResourceKey<Level> resourceKey, Connection connection, ServerPlayer player) {
        return FTBTeamBases.getInitialPlayerDimension(player, resourceKey);
    }
}

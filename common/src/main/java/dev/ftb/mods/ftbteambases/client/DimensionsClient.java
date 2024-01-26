package dev.ftb.mods.ftbteambases.client;

import dev.ftb.mods.ftbteambases.client.gui.BaseSelectionScreen;
import dev.ftb.mods.ftbteambases.net.CreateBaseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.Objects;
import java.util.Set;

public class DimensionsClient {
    public static Set<ResourceKey<Level>> playerLevels(Player player) {
        return ((LocalPlayer) player).connection.levels();
    }

    public static void openSelectionScreen() {
        Minecraft.getInstance().setScreen(new BaseSelectionScreen(baseId -> new CreateBaseMessage(baseId).sendToServer()));
    }

    public static Level clientLevel() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }
}

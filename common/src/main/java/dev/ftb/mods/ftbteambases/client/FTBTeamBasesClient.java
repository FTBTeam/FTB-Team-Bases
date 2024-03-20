package dev.ftb.mods.ftbteambases.client;

import dev.ftb.mods.ftbteambases.client.gui.BaseSelectionScreen;
import dev.ftb.mods.ftbteambases.client.gui.VisitScreen;
import dev.ftb.mods.ftbteambases.net.CreateBaseMessage;
import dev.ftb.mods.ftbteambases.net.OpenVisitScreenMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

public class FTBTeamBasesClient {
    public static Set<ResourceKey<Level>> playerLevels(Player player) {
        return ((LocalPlayer) player).connection.levels();
    }

    public static void openSelectionScreen() {
        Minecraft.getInstance().setScreen(new BaseSelectionScreen(baseId -> new CreateBaseMessage(baseId).sendToServer()));
    }

    public static Level clientLevel() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }

    public static void openVisitScreen(Map<ResourceLocation, List<OpenVisitScreenMessage.BaseData>> dimensionData) {
        Minecraft.getInstance().setScreen(new VisitScreen(dimensionData));
    }
}

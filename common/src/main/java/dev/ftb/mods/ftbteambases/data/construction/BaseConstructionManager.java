package dev.ftb.mods.ftbteambases.data.construction;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.command.CommandUtils;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public enum BaseConstructionManager {
    INSTANCE;

    private final Map<UUID,BaseConstructionAgent> agents = new HashMap<>();

    public void begin(ServerPlayer player, BaseDefinition baseDefinition) throws CommandSyntaxException {
        if (agents.containsKey(player.getUUID())) {
            throw CommandUtils.CONSTRUCTION_IN_PROGRESS.create(player.getUUID());
        }

        boolean isParty = FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(Team::isPartyTeam).orElse(false);
        if (isParty) {
            throw TeamArgument.ALREADY_IN_PARTY.create();
        }

        player.displayClientMessage(Component.translatable("ftbteambases.message.creation_started").withStyle(ChatFormatting.GREEN), false);

        agents.put(player.getUUID(), new BaseConstructionAgent(player, baseDefinition));
    }

    public void tick(MinecraftServer server) {
        if (!agents.isEmpty()) {
            Set<UUID> completed = new HashSet<>();

            agents.forEach((id, agent) -> {
                agent.tick();
                if (agent.isDone()) {
                    completed.add(id);
                }
            });

            completed.forEach(agents::remove);
        }
    }

    public boolean isConstructing(ServerPlayer player) {
        return agents.containsKey(player.getUUID());
    }
}

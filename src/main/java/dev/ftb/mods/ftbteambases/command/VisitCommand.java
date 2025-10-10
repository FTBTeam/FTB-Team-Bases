package dev.ftb.mods.ftbteambases.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.data.bases.BaseInstanceManager;
import dev.ftb.mods.ftbteambases.net.OpenVisitScreenMessage;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class VisitCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return literal("visit")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(ctx -> doOpenVisitScreen(ctx.getSource()))
                .then(argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> CommandUtils.suggestLiveBases(builder))
                        .executes(ctx -> doVisit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false))
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerNether() {
        return literal("nether-visit")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> CommandUtils.suggestLiveBases(builder))
                        .executes(ctx -> doVisit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true))
                );
    }

    private static int doOpenVisitScreen(CommandSourceStack source) throws CommandSyntaxException {
        Map<ResourceLocation, List<OpenVisitScreenMessage.BaseData>> dimensionData = new HashMap<>();
        Map<ResourceLocation, Double> tickTimes = new HashMap<>();

        BaseInstanceManager mgr = BaseInstanceManager.get(source.getServer());

        mgr.allLiveBases().forEach((id, base) -> {
            ServerLevel serverLevel = source.getServer().getLevel(base.dimension());
            if (serverLevel != null) {
                String teamName = FTBTeamsAPI.api().getManager().getTeamByID(id).map(Team::getShortName).orElse("???");
                double tickTime = tickTimes.computeIfAbsent(serverLevel.dimension().location(), k -> MiscUtil.getTickTime(source.getServer(), serverLevel.dimension()));
                dimensionData.computeIfAbsent(base.dimension().location(), k -> new ArrayList<>())
                        .add(OpenVisitScreenMessage.BaseData.create(serverLevel, teamName, tickTime, false));
            }
        });

        mgr.getArchivedBases().forEach(base -> {
            ServerLevel serverLevel = source.getServer().getLevel(base.dimension());
            if (serverLevel != null) {
                double tickTime = tickTimes.computeIfAbsent(serverLevel.dimension().location(), k -> MiscUtil.getTickTime(source.getServer(), serverLevel.dimension()));
                dimensionData.computeIfAbsent(base.dimension().location(), k -> new ArrayList<>())
                        .add(OpenVisitScreenMessage.BaseData.create(serverLevel, base.archiveId(), tickTime, true));
            }
        });

        if (dimensionData.isEmpty()) {
            source.sendFailure(Component.translatable("ftbteambases.message.no_dimensions"));
            return 0;
        }

        PacketDistributor.sendToPlayer(source.getPlayerOrException(), new OpenVisitScreenMessage(dimensionData));

        return 1;
    }

    private static int doVisit(CommandSourceStack source, String name, boolean gotoNether) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamByName(name)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(name));

        ServerPlayer player = source.getPlayerOrException();
        BaseInstanceManager mgr = BaseInstanceManager.get(source.getServer());
        boolean res = gotoNether ?
                mgr.teleportToNether(player) :
                mgr.teleportToBaseSpawn(player, team.getTeamId());

        if (!res) {
            throw CommandUtils.CANT_TELEPORT.create(name);
        }

        return 1;
    }
}

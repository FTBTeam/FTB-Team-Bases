package dev.ftb.mods.ftbteambases.data.definition;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.net.SyncBaseTemplatesMessage;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BaseDefinitionManager {
    private static final BaseDefinitionManager CLIENT_INSTANCE = new BaseDefinitionManager();
    private static final BaseDefinitionManager SERVER_INSTANCE = new BaseDefinitionManager();

    private final Map<Identifier, BaseDefinition> templates = new ConcurrentHashMap<>();

    public static BaseDefinitionManager getClientInstance() {
        return CLIENT_INSTANCE;
    }

    public static BaseDefinitionManager getServerInstance() {
        return SERVER_INSTANCE;
    }

    public Optional<BaseDefinition> getBaseDefinition(Identifier id) {
        return Optional.ofNullable(templates.get(id));
    }

    public Collection<Identifier> getTemplateIds() {
        return templates.keySet();
    }

    public Collection<BaseDefinition> getDefinitions() {
        return templates.values();
    }

    public void syncFromServer(Collection<BaseDefinition> structures) {
        templates.clear();
        structures.forEach(s -> templates.put(s.id(), s));
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener<BaseDefinition> {
        public ReloadListener() {
            super(BaseDefinition.CODEC, FileToIdConverter.json("ftb_base_definitions"));
        }

        @Override
        protected void apply(Map<Identifier, BaseDefinition> preparations, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<Identifier, BaseDefinition> serverTemplates = getServerInstance().templates;

            serverTemplates.clear();

            serverTemplates.putAll(preparations);

            FTBTeamBases.LOGGER.info("loaded {} base definitions", serverTemplates.size());

            if (ServerLifecycleHooks.getCurrentServer() != null) {
                PacketDistributor.sendToAllPlayers(new SyncBaseTemplatesMessage(BaseDefinitionManager.getServerInstance().getDefinitions()));
            }
        }
    }
}

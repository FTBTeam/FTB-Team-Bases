package dev.ftb.mods.ftbteambases.data.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.architectury.utils.GameInstance;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.net.SyncBaseTemplatesMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BaseDefinitionManager {
    private static final BaseDefinitionManager CLIENT_INSTANCE = new BaseDefinitionManager();
    private static final BaseDefinitionManager SERVER_INSTANCE = new BaseDefinitionManager();

    private final Map<ResourceLocation, BaseDefinition> templates = new ConcurrentHashMap<>();

    public static BaseDefinitionManager getClientInstance() {
        return CLIENT_INSTANCE;
    }

    public static BaseDefinitionManager getServerInstance() {
        return SERVER_INSTANCE;
    }

    public Optional<BaseDefinition> getBaseDefinition(ResourceLocation id) {
        return Optional.ofNullable(templates.get(id));
    }

    public Collection<ResourceLocation> getTemplateIds() {
        return templates.keySet();
    }

    public Collection<BaseDefinition> getDefinitions() {
        return templates.values();
    }

    public void syncFromServer(Collection<BaseDefinition> structures) {
        templates.clear();
        structures.forEach(s -> templates.put(s.id(), s));
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

        public ReloadListener() {
            super(GSON, "ftb_base_definitions");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, BaseDefinition> serverTemplates = getServerInstance().templates;

            serverTemplates.clear();

            object.forEach((id, json) -> BaseDefinition.fromJson(json).ifPresent(s -> serverTemplates.put(id, s)));

            FTBTeamBases.LOGGER.info("loaded {} base definitions", serverTemplates.size());

            if (GameInstance.getServer() != null) {
                new SyncBaseTemplatesMessage(BaseDefinitionManager.getServerInstance()).sendToAll(GameInstance.getServer());
            }
        }
    }
}

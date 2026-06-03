package dev.ftb.mods.ftbteambases.config;

import dev.ftb.mods.ftblibrary.client.config.editable.EditableStringifiedConfig;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class EditableIdentifier extends EditableStringifiedConfig<Identifier> {
    @Override
    public boolean parse(@Nullable Consumer<Identifier> consumer, String string) {
        Identifier id = Identifier.tryParse(string);
        return id != null && okValue(consumer, id);
    }
}

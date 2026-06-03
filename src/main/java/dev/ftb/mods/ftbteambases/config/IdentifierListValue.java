package dev.ftb.mods.ftbteambases.config;

import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import dev.ftb.mods.ftblibrary.config.value.AbstractListValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class IdentifierListValue extends AbstractListValue<Identifier> {
    private static final Identifier DEF_ID = Identifier.parse("ftb:none");
    private final String comment;

    public IdentifierListValue(Config parent, String key, List<Identifier> defaultValue, String comment) {
        super(parent, key, defaultValue, Identifier.CODEC);
        this.comment = comment;
    }

    @Override
    protected @Nullable EditableConfigValue<?> fillClientConfig(EditableConfigGroup group) {
        return group.addList(key, get(), new EditableIdentifier(), DEF_ID);
    }
}

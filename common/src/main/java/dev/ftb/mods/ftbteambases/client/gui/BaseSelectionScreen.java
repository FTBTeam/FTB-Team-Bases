package dev.ftb.mods.ftbteambases.client.gui;

import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Consumer;

public class BaseSelectionScreen extends Screen {
    private final Consumer<ResourceLocation> onSelect;
    private StartList startList;
    private EditBox searchBox;
    private Button createButton;
    private AbstractTexture fallbackIcon;

    public BaseSelectionScreen(Consumer<ResourceLocation> onSelect) {
        super(Component.empty());

        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        startList = new StartList(minecraft, width, height, 80, height - 40);
        searchBox = new EditBox(font, width / 2 - 160 / 2, 40, 160, 20, Component.empty());
        searchBox.setResponder(startList::addChildren);

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> onClose())
                .size(100, 20).pos(width / 2 - 130, height - 30).build());

        addRenderableWidget(createButton = Button.builder(Component.translatable("ftbteambases.gui.create"), btn -> doCreate())
                .size(150, 20).pos(width / 2 - 20, height - 30).build());
        createButton.active = false;

        addWidget(searchBox);
        addWidget(startList);

        fallbackIcon = minecraft.getTextureManager().getTexture(BaseDefinition.FALLBACK_IMAGE);
    }

    private void doCreate() {
        if (startList.getSelected() != null) {
            onSelect.accept(startList.getSelected().baseDef.id());
            if (minecraft.level != null) {
                onClose();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        startList.render(graphics, mouseX, mouseY, partialTick);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        super.render(graphics, mouseX, mouseY, partialTick);

        String value = Component.translatable("ftbteambases.gui.select_start").getString();
        graphics.drawString(font, value, (width - font.width(value)) / 2, 20, 0xFFFFFF);
    }

    private class StartList extends AbstractSelectionList<StartList.Entry> {
        StartList(Minecraft minecraft, int width, int height, int top, int bottom) {
            super(minecraft, width, height, top, bottom, 50); // 30 = item height

            addChildren("");
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + 170;
        }

        @Override
        public void setSelected(@Nullable BaseSelectionScreen.StartList.Entry entry) {
            BaseSelectionScreen.this.createButton.active = entry != null;
            super.setSelected(entry);
        }

        private void addChildren(String filterStr) {
            children().clear();

            children().addAll(BaseDefinitionManager.getClientInstance().getDefinitions().stream()
                    .filter(baseDef -> baseDef.matchesName(filterStr) && baseDef.shouldShowInGui())
                    .sorted(Comparator.comparingInt(BaseDefinition::displayOrder).thenComparing(BaseDefinition::description))
                    .map(Entry::new)
                    .toList()
            );
        }

        @Override
        public void updateNarration(NarrationElementOutput arg) {
        }

        private class Entry extends AbstractSelectionList.Entry<Entry> {
            private final BaseDefinition baseDef;
            private long lastClickTime;

            private Entry(BaseDefinition baseDef) {
                this.baseDef = baseDef;
            }

            @Override
            public boolean mouseClicked(double x, double y, int partialTick) {
                StartList.this.setSelected(this);

                if (Util.getMillis() - lastClickTime < 250L) {
                    BaseSelectionScreen.this.onClose();
                    BaseSelectionScreen.this.onSelect.accept(baseDef.id());
                    return true;
                } else {
                    lastClickTime = Util.getMillis();
                    return false;
                }
            }

            @Override
            public void render(GuiGraphics graphics, int entryId, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean bl, float partialTicks) {
                Font font = Minecraft.getInstance().font;

                int startX = left + 80;
                graphics.drawString(font, Component.translatable(baseDef.description()), startX, top + 10, 0xFFFFFF);
                graphics.drawString(font, Component.translatable("ftbteambases.gui.by", baseDef.author()), startX, top + 26, 0xD3D3D3);

                ResourceLocation preview = baseDef.previewImage().orElse(BaseDefinition.DEFAULT_PREVIEW);
                graphics.blit(preview, left + 7, top + 7, 0f, 0f, 56, 32, 56, 32);
            }
        }
    }
}

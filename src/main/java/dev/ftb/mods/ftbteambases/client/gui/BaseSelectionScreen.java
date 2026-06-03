package dev.ftb.mods.ftbteambases.client.gui;

import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Consumer;

public class BaseSelectionScreen extends Screen {
    private final Consumer<Identifier> onSelect;
    private StartList startList;
    private Button createButton;
    private AbstractTexture fallbackIcon;

    static final int UPPER_HEIGHT = 80;
    static final int LOWER_HEIGHT = 40;

    public BaseSelectionScreen(Consumer<Identifier> onSelect) {
        super(Component.empty());

        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        startList = new StartList(minecraft, width, height - UPPER_HEIGHT - LOWER_HEIGHT, UPPER_HEIGHT);
        EditBox searchBox = new EditBox(font, width / 2 - 160 / 2, 40, 160, 20, Component.empty());
        searchBox.setResponder(startList::addChildren);

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), _ -> onClose())
                .size(100, 20).pos(width / 2 - 130, height - 30).build());

        addRenderableWidget(createButton = Button.builder(Component.translatable("ftbteambases.gui.create"), _ -> doCreate())
                .size(150, 20).pos(width / 2 - 20, height - 30).build());
        createButton.active = false;

        addRenderableWidget(searchBox);
        addRenderableWidget(startList);

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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String value = Component.translatable("ftbteambases.gui.select_start").getString();
        graphics.text(font, value, (width - font.width(value)) / 2, 20, 0xFFFFFFFF);
    }

    private class StartList extends AbstractSelectionList<StartList.Entry> {
        StartList(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, 50); // 50 = item height

            addChildren("");
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        @Override
        protected int scrollBarX() {
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
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        private class Entry extends AbstractSelectionList.Entry<Entry> {
            private final BaseDefinition baseDef;

            private Entry(BaseDefinition baseDef) {
                this.baseDef = baseDef;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                StartList.this.setSelected(this);

                if (doubleClick) {
                    BaseSelectionScreen.this.onClose();
                    BaseSelectionScreen.this.onSelect.accept(baseDef.id());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                Font font = Minecraft.getInstance().font;

                int startX = getContentX() + 80;
                int top = getContentY();
                graphics.text(font, Component.translatable(baseDef.description()), startX, top + 10, 0xFFFFFFFF);
                graphics.text(font, Component.translatable("ftbteambases.gui.by", baseDef.author()), startX, top + 26, 0xFFD3D3D3);

                Identifier preview = baseDef.previewImage();
                graphics.blit(RenderPipelines.GUI_TEXTURED, preview, getContentX() + 7, top + 7, 0f, 0f, 56, 32, 56, 32);
            }
        }
    }
}

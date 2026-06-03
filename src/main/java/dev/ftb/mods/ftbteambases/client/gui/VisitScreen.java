package dev.ftb.mods.ftbteambases.client.gui;

import dev.ftb.mods.ftbteambases.net.OpenVisitScreenMessage;
import dev.ftb.mods.ftbteambases.net.VisitBaseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.ftb.mods.ftbteambases.client.gui.BaseSelectionScreen.LOWER_HEIGHT;
import static dev.ftb.mods.ftbteambases.client.gui.BaseSelectionScreen.UPPER_HEIGHT;

public class VisitScreen extends Screen {
    private final Map<Identifier, List<OpenVisitScreenMessage.BaseData>> baseDataMap;
    private VisitList visitList;
    private EditBox searchBox;
    private Button createButton;

    private static boolean showArchived = false; // persists

    public VisitScreen(Map<Identifier, List<OpenVisitScreenMessage.BaseData>> baseDataMap) {
        super(Component.empty());
        this.baseDataMap = baseDataMap;
    }

    @Override
    protected void init() {
        super.init();

        visitList = new VisitList(minecraft, width, height - UPPER_HEIGHT - LOWER_HEIGHT, UPPER_HEIGHT);

        searchBox = new EditBox(font, width / 2 - 160 / 2, 40, 160, 20, Component.empty());
        searchBox.setResponder(visitList::onFilterChanged);

        Component label = Component.translatable("ftbteambases.gui.show_archived");
        Checkbox checkbox = Checkbox.builder(label, font)
                .pos(width - font.width(label) - 35, 40)
                .selected(showArchived)
                .onValueChange((_, selected) -> {
                    showArchived = selected;
                    visitList.onFilterChanged(searchBox.getValue());
                }).build();

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, _ -> onClose())
                .size(100, 20).pos(width / 2 - 130, height - 30).build());

        addRenderableWidget(createButton = Button.builder(Component.translatable("ftbteambases.gui.visit"), _ -> onActivate())
                .size(150, 20).pos(width / 2 - 20, height - 30).build());

        createButton.active = false;

        addRenderableWidget(checkbox);
        addRenderableWidget(searchBox);
        addRenderableWidget(visitList);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String value = Component.translatable("ftbteambases.gui.select_dimension").getString();
        graphics.text(font, value, width / 2 - font.width(value) / 2, 20, 0xFFFFFFFF);
    }

    private void onActivate() {
        VisitList.Entry entry = visitList.getSelected();
        if (entry != null) {
            ClientPacketDistributor.sendToServer(new VisitBaseMessage(entry.data.teamName(), entry.data.archived()));
            if (minecraft.level != null) {
                onClose();
            }
        }
    }

    private class VisitList extends AbstractSelectionList<VisitList.Entry> {
        public VisitList(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, 45);

            addChildren("");
        }

        private void addChildren(String filter) {
            List<Entry> entries = new ArrayList<>();

            baseDataMap.forEach((id, dataList) ->
                    dataList.forEach(data -> {
                        if (showArchived || !data.archived()) {
                            String name = data.teamName();
                            if (filter.isEmpty() || name.toLowerCase().contains(filter)) {
                                entries.add(new Entry(id, data));
                            }
                        }
                    })
            );

            children().addAll(entries.stream().sorted(Comparator.comparing(o -> o.data.teamName())).toList());
        }

        @Override
        public int getRowWidth() {
            return 400;
        }

        @Override
        protected int scrollBarX() {
            return width / 2 + 200;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        @Override
        public void setSelected(@Nullable VisitScreen.VisitList.Entry entry) {
            VisitScreen.this.createButton.active = entry != null;
            super.setSelected(entry);
        }

        private void onFilterChanged(String filter) {
            children().clear();
            addChildren(filter);
        }

        private class Entry extends AbstractSelectionList.Entry<Entry> {
            private final Identifier dimId;
            private final OpenVisitScreenMessage.BaseData data;

            public Entry(Identifier dimId, OpenVisitScreenMessage.BaseData data) {
                this.dimId = dimId;
                this.data = data;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                VisitList.this.setSelected(this);

                if (doubleClick) {
                    VisitScreen.this.onActivate();
                }
                return true;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                Font font = Minecraft.getInstance().font;
                int lh = font.lineHeight + 1;

                int tpsCol;
                if (data.tickTime() < 50.0) {
                    tpsCol = 0x80FF80;
                } else if (data.tickTime() < 100.0) {
                    tpsCol = 0xFFFF80;
                } else {
                    tpsCol = 0xFF8080;
                }
                int startX = getContentX() + 5;
                int top = getContentY();
                graphics.text(font, Component.literal(data.teamName()), startX, top + 2, data.archived() ? 0xFF606060 : 0xFFFFFFFF);
                graphics.text(font, Component.literal(dimId.toString()), startX, top + 2 + lh, data.archived() ? 0xFF808080 : 0xFFC0C0C0);
                double tps = Math.min(1000.0 / data.tickTime(), 20.0);
                graphics.text(font, Component.literal(String.format("%.3f ms/tick (%.3f TPS)", data.tickTime(), tps)), startX + 5, top + 2 + lh * 2, tpsCol);
                if (data.archived()) {
                    graphics.text(font, Component.literal("Archived"), startX + 5, top + 2 + lh * 3, 0xFFD0A000);
                } else {
                    graphics.text(font, Component.literal("Active"), startX + 5, top + 2 + lh * 3, 0xFFD0D000);
                }
                if (hovered) {
                    List<Component> tooltip = List.of(
                            Component.translatable("ftbteambases.gui.block_entities", data.blockEntities()),
                            Component.translatable("ftbteambases.gui.entities", data.entities()),
                            Component.translatable("ftbteambases.gui.loaded_chunks", data.loadedChunks())
                    );
                    graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
                }
            }
        }
    }
}

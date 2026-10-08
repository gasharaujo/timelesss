package net.gasharaujo.timelesss.client;

import java.util.ArrayList;
import java.util.List;
import net.gasharaujo.timelesss.TimeNetwork;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TimeBindsScreen extends Screen {
    private static final int ROW_HEIGHT = 38;
    private final List<BindView> binds;
    private final long energy;
    private final long capacity;
    private BindView selected;
    private int page;

    public TimeBindsScreen(String snapshot) {
        super(Component.translatable("screen.timelesss.binds.title"));
        String[] lines = snapshot.split("\\n");
        long parsedEnergy = 0L;
        long parsedCapacity = 0L;
        List<BindView> parsed = new ArrayList<>();
        if (lines.length > 0) {
            String[] header = lines[0].split("\\|", -1);
            if (header.length >= 2) {
                parsedEnergy = parseLong(header[0]);
                parsedCapacity = parseLong(header[1]);
            }
        }
        for (int index = 1; index < lines.length; index++) {
            String[] fields = lines[index].split("\\|", -1);
            if (fields.length >= 4) {
                parsed.add(new BindView(fields[0], parseLong(fields[1]), (int) parseLong(fields[2]), fields[3]));
            }
        }
        energy = parsedEnergy;
        capacity = parsedCapacity;
        binds = parsed;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    @Override
    protected void init() {
        buildPage();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the in-world view sharp; this screen draws its own dark panel.
    }

    private int panelTop() {
        return Math.max(4, Math.round(height * 0.05F));
    }

    private int panelBottom() {
        return height - panelTop();
    }

    private int panelWidth() {
        return Math.min(520, width - 20);
    }

    private int pageSize() {
        return Math.max(1, (panelBottom() - panelTop() - 84) / ROW_HEIGHT);
    }

    private long currentConsumption() {
        long total = 0L;
        for (BindView bind : binds) {
            total += 100L * bind.multiplier * bind.multiplier;
        }
        return total;
    }

    private void buildPage() {
        clearWidgets();
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = panelTop();
        int rowWidth = (panelWidth - 24) / 2;
        int start = page * pageSize();
        int end = Math.min(binds.size(), start + pageSize());
        for (int index = start; index < end; index++) {
            BindView bind = binds.get(index);
            int rowTop = top + 48 + (index - start) * ROW_HEIGHT;
            Component label = Component.literal((bind == selected ? "> " : "") + bind.blockId);
            addRenderableWidget(Button.builder(label, button -> {
                selected = bind;
                buildPage();
            }).bounds(left + 8, rowTop, rowWidth, 20).build());
            addRenderableWidget(new BindSlider(left + panelWidth - rowWidth - 8, rowTop, rowWidth, 20, bind));
        }

        int footerY = panelBottom() - 24;
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page--;
                buildPage();
            }).bounds(left + 8, footerY, 28, 20).build());
        }
        if ((page + 1) * pageSize() < binds.size()) {
            addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page++;
                buildPage();
            }).bounds(left + panelWidth - 36, footerY, 28, 20).build());
        }
        Button delete = addRenderableWidget(Button.builder(
            Component.translatable("screen.timelesss.binds.delete_selected"), button -> deleteSelected())
            .bounds(width / 2 - 110, footerY, 106, 20).build());
        delete.active = selected != null;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(width / 2 + 4, footerY, 90, 20).build());
    }

    private void deleteSelected() {
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new TimelesssMod.RemoveBindPayload(selected.dimension, selected.position));
        binds.remove(selected);
        selected = null;
        int pages = Math.max(1, (binds.size() + pageSize() - 1) / pageSize());
        page = Math.min(page, pages - 1);
        buildPage();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = panelTop();
        int bottom = panelBottom();
        graphics.fill(left, top, left + panelWidth, bottom, 0xD018101F);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFFDD3344);
        graphics.drawCenteredString(font, title, width / 2, top + 7, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
            Component.translatable("screen.timelesss.binds.energy", energy, capacity), width / 2, top + 19, 0xFF7CFC98);
        graphics.drawCenteredString(font,
            Component.translatable("screen.timelesss.binds.consumption", currentConsumption()),
            width / 2, top + 31, 0xFFFFD166);
        int start = page * pageSize();
        int end = Math.min(binds.size(), start + pageSize());
        if (binds.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.timelesss.binds.empty"),
                width / 2, (top + bottom) / 2, 0xFFB8B8C8);
        }
        for (int index = start; index < end; index++) {
            BindView bind = binds.get(index);
            int rowTop = top + 48 + (index - start) * ROW_HEIGHT;
            if (bind == selected) {
                graphics.fill(left + 5, rowTop - 3, left + panelWidth - 5, rowTop + 34, 0x6040A060);
            }
            BlockPos pos = BlockPos.of(bind.position);
            String details = "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]  " + bind.dimension;
            graphics.drawString(font, details, left + 10, rowTop + 23, 0xFF9FAFC5, false);
        }
        int pages = Math.max(1, (binds.size() + pageSize() - 1) / pageSize());
        graphics.drawCenteredString(font, (page + 1) + "/" + pages, width / 2, bottom - 35, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class BindView {
        private final String dimension;
        private final long position;
        private int multiplier;
        private final String blockId;

        private BindView(String dimension, long position, int multiplier, String blockId) {
            this.dimension = dimension;
            this.position = position;
            this.multiplier = Mth.clamp(multiplier, 0, TimeNetwork.MAX_MULTIPLIER);
            this.blockId = blockId;
        }
    }

    private final class BindSlider extends AbstractSliderButton {
        private final BindView bind;

        private BindSlider(int x, int y, int width, int height, BindView bind) {
            super(x, y, width, height, Component.empty(), bind.multiplier / 50.0D);
            this.bind = bind;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.timelesss.value", bind.multiplier));
        }

        @Override
        protected void applyValue() {
            bind.multiplier = Mth.clamp((int) Math.round(value * 50.0D), 0, TimeNetwork.MAX_MULTIPLIER);
            updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            PacketDistributor.sendToServer(
                new TimelesssMod.SetBindMultiplierPayload(bind.dimension, bind.position, bind.multiplier)
            );
        }
    }
}

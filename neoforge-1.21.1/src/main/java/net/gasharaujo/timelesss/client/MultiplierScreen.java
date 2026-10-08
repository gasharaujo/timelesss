package net.gasharaujo.timelesss.client;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MultiplierScreen extends Screen {
    private final TimelesssLocator.Found source;
    private final ItemStack stack;
    private final int initialMultiplier;
    private int multiplier;
    private boolean updateSent;

    public MultiplierScreen(TimelesssLocator.Found source, int multiplier) {
        super(Component.translatable("screen.timelesss.title"));
        this.source = source;
        this.stack = source.stack();
        this.initialMultiplier = multiplier;
        this.multiplier = multiplier;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 2 - 10;
        this.addRenderableWidget(new MultiplierSlider(left, top, 200, 20, this.multiplier));
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 50, top + 38, 100, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = this.height / 2 - 54;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, 0xFFFFFFFF);
        graphics.drawCenteredString(
            this.font,
            Component.translatable("screen.timelesss.range"),
            this.width / 2,
            top + 31,
            0xFFB8B8C8
        );
    }

    private void setMultiplier(int value) {
        this.multiplier = Mth.clamp(value, TimelesssItem.MIN_MULTIPLIER, TimelesssItem.MAX_MULTIPLIER);
        TimelesssItem.setMultiplier(this.stack, this.multiplier);
    }

    private void sendUpdate() {
        if (this.updateSent) {
            return;
        }
        this.updateSent = true;
        if (this.multiplier != this.initialMultiplier) {
            PacketDistributor.sendToServer(
                new TimelesssMod.SetMultiplierPayload(
                    this.source.sourceType(), this.source.sourceIndex(), this.multiplier
                )
            );
        }
    }

    @Override
    public void onClose() {
        this.sendUpdate();
        super.onClose();
    }

    @Override
    public void removed() {
        this.sendUpdate();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class MultiplierSlider extends AbstractSliderButton {
        private MultiplierSlider(int x, int y, int width, int height, int multiplier) {
            super(
                x,
                y,
                width,
                height,
                Component.empty(),
                (multiplier - TimelesssItem.MIN_MULTIPLIER) / 24.0
            );
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.timelesss.value", MultiplierScreen.this.multiplier));
        }

        @Override
        protected void applyValue() {
            int next = TimelesssItem.MIN_MULTIPLIER + (int) Math.round(this.value * 24.0);
            MultiplierScreen.this.setMultiplier(next);
            this.updateMessage();
        }
    }
}

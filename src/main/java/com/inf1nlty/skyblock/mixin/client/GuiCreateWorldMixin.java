package com.inf1nlty.skyblock.mixin.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiCreateWorld;
import net.minecraft.src.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiCreateWorld.class)
public class GuiCreateWorldMixin {

    @Unique
    private boolean skyBlockSelected = false;

    @Unique
    private GuiButton skyIslandButton;

    @SuppressWarnings("unchecked")
    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        GuiCreateWorld self = (GuiCreateWorld) (Object) this;
        int btnId = 10086;
        int btnX = self.width / 2 - 75;
        int btnY = 160;
        int btnW = 150;
        int btnH = 20;
        String btnText = I18n.getString("island.createworld.voidworld");
        skyIslandButton = new GuiButton(btnId, btnX, btnY, btnW, btnH, btnText);
        skyIslandButton.drawButton = !self.moreOptions;
        self.buttonList.add(skyIslandButton);
    }

    @Inject(method = "func_82288_a", at = @At("TAIL"))
    private void onToggleMoreOptions(boolean showMore, CallbackInfo ci) {
        GuiCreateWorld self = (GuiCreateWorld) (Object) this;
        if (skyIslandButton != null) {
            skyIslandButton.drawButton = !self.moreOptions;
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        GuiCreateWorld self = (GuiCreateWorld) (Object) this;
        if (button.id == 10086) {
            skyBlockSelected = !skyBlockSelected;
            button.displayString = I18n.getString(
                    skyBlockSelected ? "island.createworld.selected" : "island.createworld.voidworld"
            );
            self.generatorOptionsToUse = skyBlockSelected ? "voidworld" : "";
        }
    }
}
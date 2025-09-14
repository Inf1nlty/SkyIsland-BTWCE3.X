package com.inf1nlty.skyblock.mixin.world.item;

import com.inf1nlty.skyblock.command.SkyBlockCommand;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEnderEye.class)
public abstract class ItemEnderEyeMixin {

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void onItemRightClick(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            if (!world.isRemote && player instanceof EntityPlayerMP) {
                ChatMessageComponent msg = SkyBlockCommand.createMessage(
                        "item.disabled_voidworld",
                        EnumChatFormatting.RED, false, false, false);
                player.sendChatToPlayer(msg);
            }
            cir.setReturnValue(stack);
        }
    }
}
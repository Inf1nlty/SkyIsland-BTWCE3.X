package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.command.SkyBlockCommand;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import com.inf1nlty.skyblock.util.VoidWorldMobCleaner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.World;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {

        WorldServer world = MinecraftServer.getServer().worldServers[0];
        SkyBlockCommand.onServerTick(world);
        SkyBlockProtectionUtil.checkSkyBlockProtection(world);

        VoidWorldMobCleaner.onServerTick(world);
    }

    @Inject(method = "isBlockProtected", at = @At("HEAD"), cancellable = true)
    private static void injectBlockProtected(World world, int x, int y, int z, EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {

        if (player.capabilities.isCreativeMode) {
            cir.setReturnValue(false);
            return;
        }

        MinecraftServer server = ((WorldServer) world).getMinecraftServer();
        int protection = server.getSpawnProtectionSize();
        if (protection > 0 && Math.abs(x) <= protection && Math.abs(z) <= protection) {
            cir.setReturnValue(true);
        }
    }

}
package com.inf1nlty.skyblock.mixin;

import com.inf1nlty.skyblock.command.SkyBlockCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {

        WorldServer world = MinecraftServer.getServer().worldServers[0];
        SkyBlockCommand.onServerTick(world);
    }
}
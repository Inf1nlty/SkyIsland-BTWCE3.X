package com.inf1nlty.skyblock.mixin.client;

import net.minecraft.src.GuiNewChat;
import net.minecraft.src.StatCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts and localizes chat messages for island commands.
 * Preserves color codes and cleans up unused parameters.
 */
@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {

    /**
     * Intercepts the chat variable used for rendering and performs localization and substitution.
     * Tries to preserve color codes and cleans up unused parameters.
     */
    @ModifyVariable(method = "drawChat", at = @At(value = "STORE", ordinal = 0), name = "var17")
    private String localizeIslandMessage(String var17) {
        if (var17 == null) return var17;

        String colorCode = "";
        if (var17.length() > 1 && var17.charAt(0) == '§') {
            colorCode = var17.substring(0, 2);
            var17 = var17.substring(2);
        }

        // Directly match "commands.island" prefix
        if (var17.startsWith("commands.island")) {
            int pipeIdx = var17.indexOf("|");
            String key = pipeIdx > -1 ? var17.substring(0, pipeIdx) : var17;
            String localized = StatCollector.translateToLocal(key);

            // If not found, fallback to raw
            if (localized.equals(key)) return colorCode + var17;

            // Substitute parameters if present
            if (pipeIdx > -1) {
                String params = var17.substring(pipeIdx + 1);
                String[] paramPairs = params.split("\\|");
                for (String pair : paramPairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        localized = localized.replace("{" + kv[0] + "}", kv[1]);
                    }
                }
                // Clean up any unsubstituted placeholders
                localized = localized.replaceAll("\\{[a-zA-Z0-9_]+}", "");
            }

            // Handle multi-line localization
            String[] lines = localized.split("\\\\n|\\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(colorCode).append(line).append("\n");
            }
            return sb.toString().trim();
        }
        return colorCode + var17;
    }
}
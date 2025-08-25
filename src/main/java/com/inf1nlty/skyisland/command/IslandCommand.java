package com.inf1nlty.skyisland.command;

import com.inf1nlty.skyisland.util.IslandDataManager;
import com.inf1nlty.skyisland.util.IslandManager;
import com.inf1nlty.skyisland.util.IslandPoint;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.*;

import java.util.*;

/**
 * Handles all island-related commands under a single /island command.
 * Subcommands:
 *   - /island info     /is i
 *   - /island new      /is n
 *   - /island delete   /is d
 *   - /island delete confirm   /is d c
 *   - /island setspawn /is s
 *   - /island tpa <player>|yes|no|setyes|setno
 */
public class IslandCommand extends CommandBase {

    // State for delayed creation and deletion, and tpa requests
    private static final Map<String, Integer> pendingIslandTeleports = new HashMap<>();
    private static final Map<String, Integer> pendingCreate = new HashMap<>();
    private static final Map<String, String> pendingDeleteRequests = new HashMap<>();
    private static final Map<String, String> pendingTPARequests = new HashMap<>();
    private static final Map<String, PendingTeleport> pendingTeleports = new HashMap<>();

    private static class PendingTeleport {
        public final EntityPlayerMP from;
        public final IslandPoint targetIsland;
        public int ticksLeft;

        public PendingTeleport(EntityPlayerMP from, IslandPoint targetIsland, int ticksLeft) {
            this.from = from;
            this.targetIsland = targetIsland;
            this.ticksLeft = ticksLeft;
        }
    }

    @Override
    public String getCommandName() { return "island"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/island info|i|new|n|delete|d[confirm]|setspawn|s|tpa|tp <player>|yes|no|setyes|setno";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        // Provide completion for subcommands and tpa targets
        if (!(sender instanceof EntityPlayerMP player)) return Collections.emptyList();
        if (args.length == 1) {
            String sub = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayerMP target = (EntityPlayerMP) obj;
                if (!target.username.equalsIgnoreCase(player.username) &&
                        target.username.toLowerCase().startsWith(sub)) {
                    names.add(target.username);
                }
            }
            if ("info".startsWith(sub)) names.add("info");
            if ("new".startsWith(sub)) names.add("new");
            if ("delete".startsWith(sub)) names.add("delete");
            if ("setspawn".startsWith(sub)) names.add("setspawn");
            if ("tpa".startsWith(sub)) names.add("tpa");
            if ("yes".startsWith(sub)) names.add("yes");
            if ("no".startsWith(sub)) names.add("no");
            if ("setyes".startsWith(sub)) names.add("setyes");
            if ("setno".startsWith(sub)) names.add("setno");
            if ("confirm".startsWith(sub)) names.add("confirm");
            return names;
        }
        // For /island tpa <player>|yes|no|setyes|setno
        if (args.length == 2 && args[0].equalsIgnoreCase("tpa")) {
            String sub = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayerMP target = (EntityPlayerMP) obj;
                if (!target.username.equalsIgnoreCase(player.username) &&
                        target.username.toLowerCase().startsWith(sub)) {
                    names.add(target.username);
                }
            }
            if ("yes".startsWith(sub)) names.add("yes");
            if ("no".startsWith(sub)) names.add("no");
            if ("setyes".startsWith(sub)) names.add("setyes");
            if ("setno".startsWith(sub)) names.add("setno");
            return names;
        }
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP player)) return;

        if (args.length == 0) {
            // /island -- delayed teleport to own island
            IslandPoint island = IslandDataManager.getIsland(player);
            if (island == null) {
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound")
                        .setColor(EnumChatFormatting.RED));
                return;
            }
            // /island -- delayed teleport to own island
            if (pendingIslandTeleports.containsKey(player.username)) {
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tp_pending|name=" + player.username)
                        .setColor(EnumChatFormatting.YELLOW));
            } else {
                pendingIslandTeleports.put(player.username, 60); // 5 seconds
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tp_wait|name=" + player.username)
                        .setColor(EnumChatFormatting.YELLOW));
            }
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "info":
            case "i":
                handleInfo(player);
                break;
            case "new":
            case "n":
                handleNew(player);
                break;
            case "delete":
            case "d":
                if (args.length > 1 && (args[1].equalsIgnoreCase("confirm") || args[1].equalsIgnoreCase("c"))) {
                    handleDeleteConfirm(player);
                } else {
                    handleDelete(player);
                }
                break;
            case "setspawn":
            case "s":
                handleSetSpawn(player);
                break;
            case "tpa":
            case "tp":
                if (args.length < 2) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.usage").setColor(EnumChatFormatting.YELLOW));
                    return;
                }
                handleTPA(player, args[1]);
                break;
            case "yes":
            case "no":
            case "setyes":
            case "setno":
                handleTPA(player, sub);
                break;
            default:
                handleInfo(player);
                break;
        }
    }

    /**
     * Displays island info for the current player.
     */
    private void handleInfo(EntityPlayerMP player) {
        IslandPoint island = IslandDataManager.getIsland(player);
        if (island != null) {
            String infoLine1 = String.format(
                    "commands.island.info.line1|name=%s|x=%d|y=%d|z=%d|dim=%d",
                    player.username, island.x, island.y, island.z, island.dim
            );
            String infoLine2 = String.format(
                    "commands.island.info.line2|spawnX=%d|spawnY=%d|spawnZ=%d|tpa=%s",
                    island.spawnX, island.spawnY, island.spawnZ, island.tpaEnabled ? "yes" : "no"
            );
            player.sendChatToPlayer(ChatMessageComponent.createFromText(infoLine1).setColor(EnumChatFormatting.AQUA));
            player.sendChatToPlayer(ChatMessageComponent.createFromText(infoLine2).setColor(EnumChatFormatting.AQUA));
        } else {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
        }
    }

    /**
     * Handles delayed creation of player's islands.
     */
    private void handleNew(EntityPlayerMP player) {
        if (IslandDataManager.getIsland(player) != null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.already|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        pendingCreate.put(player.username, 60);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.create.wait|name=" + player.username)
                .setColor(EnumChatFormatting.YELLOW));
    }

    /**
     * Schedules the player's island for deletion.
     */
    private void handleDelete(EntityPlayerMP player) {
        IslandPoint island = IslandDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        island.pendingDelete = true;
        island.pendingDeleteTime = System.currentTimeMillis();
        IslandDataManager.setIsland(player, island);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.pending|name=" + player.username)
                .setColor(EnumChatFormatting.YELLOW));
    }

    /**
     * Confirms deletion of the player's island.
     */
    private void handleDeleteConfirm(EntityPlayerMP player) {
        IslandPoint island = IslandDataManager.getIsland(player);
        if (island == null || !island.pendingDelete) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.nopending|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        IslandDataManager.setIsland(player, null);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.success|name=" + player.username)
                .setColor(EnumChatFormatting.GREEN));
    }

    /**
     * Updates the spawn location for the player's island.
     */
    private void handleSetSpawn(EntityPlayerMP player) {
        IslandPoint island = IslandDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        if (player.dimension != island.dim) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.setspawn_dim_mismatch|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        IslandManager.setSpawn(island, (int)player.posX, (int)player.posY, (int)player.posZ);
        IslandDataManager.setIsland(player, island);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.setspawn.success|name=" + player.username)
                .setColor(EnumChatFormatting.GREEN));
    }

    /**
     * Handles teleport requests between islands and tpa settings.
     */
    private void handleTPA(EntityPlayerMP player, String arg) {
        arg = arg.toLowerCase();

        switch (arg) {
            case "setyes":
            case "setno": {
                IslandPoint island = IslandDataManager.getIsland(player);
                if (island == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username).setColor(EnumChatFormatting.RED));
                    return;
                }
                island.tpaEnabled = arg.equals("setyes");
                IslandDataManager.setIsland(player, island);
                player.sendChatToPlayer(ChatMessageComponent.createFromText(
                                "commands.island.tpa.set" + (island.tpaEnabled ? "yes" : "no") + "|name=" + player.username)
                        .setColor(EnumChatFormatting.GREEN));
                return;
            }
            case "yes": {
                String requesterName = pendingTPARequests.remove(player.username);
                if (requesterName == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.norequest").setColor(EnumChatFormatting.RED));
                    return;
                }
                EntityPlayerMP requester = getOnlinePlayer(requesterName);
                if (requester == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.offline|name=" + requesterName).setColor(EnumChatFormatting.RED));
                    return;
                }
                IslandPoint targetIsland = IslandDataManager.getIsland(player);
                IslandPoint requesterIsland = IslandDataManager.getIsland(requester);

                if (!targetIsland.tpaEnabled) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.notenabled|name=" + player.username).setColor(EnumChatFormatting.RED));
                    requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.notenabled|name=" + player.username).setColor(EnumChatFormatting.RED));
                    return;
                }
                if (targetIsland.dim != requester.dimension) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.dim_mismatch|name=" + requester.username).setColor(EnumChatFormatting.RED));
                    requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.dim_mismatch|name=" + player.username).setColor(EnumChatFormatting.RED));
                    return;
                }
                pendingTeleports.put(requester.username, new PendingTeleport(requester, targetIsland, 60));
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.accepted|name=" + requester.username)
                        .setColor(EnumChatFormatting.GREEN));
                requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.accepted_by|name=" + player.username)
                        .setColor(EnumChatFormatting.GREEN));
                requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.wait|name=" + player.username)
                        .setColor(EnumChatFormatting.YELLOW));
                return;
            }
            case "no": {
                String requesterName = pendingTPARequests.remove(player.username);
                if (requesterName == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.norequest").setColor(EnumChatFormatting.RED));
                    return;
                }
                EntityPlayerMP requester = getOnlinePlayer(requesterName);
                if (requester != null) {
                    requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.denied_by|name=" + player.username).setColor(EnumChatFormatting.RED));
                }
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.denied|name=" + requesterName).setColor(EnumChatFormatting.RED));
                return;
            }
        }

        if (arg.equalsIgnoreCase(player.username)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.self").setColor(EnumChatFormatting.RED));
            return;
        }
        EntityPlayerMP target = getOnlinePlayer(arg);
        if (target == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.offline|name=" + arg).setColor(EnumChatFormatting.RED));
            return;
        }
        IslandPoint targetIsland = IslandDataManager.getIsland(target);
        IslandPoint requesterIsland = IslandDataManager.getIsland(player);
        if (targetIsland == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.target_no_island|name=" + target.username).setColor(EnumChatFormatting.RED));
            return;
        }
        if (!targetIsland.tpaEnabled) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.notenabled|name=" + target.username).setColor(EnumChatFormatting.RED));
            return;
        }
        if (targetIsland.dim != player.dimension) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.dim_mismatch|name=" + target.username).setColor(EnumChatFormatting.RED));
            return;
        }
        pendingTPARequests.put(target.username, player.username);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.request_sent|name=" + target.username).setColor(EnumChatFormatting.AQUA));
        target.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.request|name=" + player.username).setColor(EnumChatFormatting.AQUA));
    }

    /**
     * Server tick handler for delayed island creation, deletion timeout, and pending teleports.
     * Should be called each tick from mod/server code.
     */
    public static void onServerTick(World world) {
        Iterator<Map.Entry<String, Integer>> itCreate = pendingCreate.entrySet().iterator();
        while (itCreate.hasNext()) {
            Map.Entry<String, Integer> entry = itCreate.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                EntityPlayerMP player = getOnlinePlayer(entry.getKey());
                if (player != null && IslandDataManager.getIsland(player) == null) {
                    IslandPoint island = IslandManager.makeIsland(player, (WorldServer) world);
                    IslandDataManager.setIsland(player, island);
                    IslandManager.generateIsland(world, island);
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.create.success|name=" + player.username)
                            .setColor(EnumChatFormatting.GREEN));
                }
                itCreate.remove();
            } else {
                entry.setValue(ticks);
            }
        }
        // Handle pending island creation
        Iterator<Map.Entry<String, Integer>> itIslandTP = pendingIslandTeleports.entrySet().iterator();
        while (itIslandTP.hasNext()) {
            Map.Entry<String, Integer> entry = itIslandTP.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                EntityPlayerMP player = getOnlinePlayer(entry.getKey());
                if (player != null) {
                    IslandPoint island = IslandDataManager.getIsland(player);
                    if (island != null && player.dimension == island.dim) {
                        player.setPositionAndUpdate(island.spawnX, island.spawnY, island.spawnZ);
                        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tp_success|name=" + player.username)
                                .setColor(EnumChatFormatting.GREEN));
                    } else if (island == null) {
                        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                                .setColor(EnumChatFormatting.RED));
                    } else {
                        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.setspawn_dim_mismatch|name=" + player.username)
                                .setColor(EnumChatFormatting.RED));
                    }
                }
                itIslandTP.remove();
            } else {
                entry.setValue(ticks);
            }
        }

        // Handle island delete timeouts
        for (Object obj : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            IslandPoint island = IslandDataManager.getIsland(player);
            if (island != null && island.pendingDelete) {
                long now = System.currentTimeMillis();
                if (now - island.pendingDeleteTime > 60_000) {
                    island.pendingDelete = false;
                    IslandDataManager.setIsland(player, island);
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.timeout|name=" + player.username)
                            .setColor(EnumChatFormatting.RED));
                }
            }
        }

        // Handle pending teleports
        Iterator<Map.Entry<String, PendingTeleport>> itTP = pendingTeleports.entrySet().iterator();
        while (itTP.hasNext()) {
            PendingTeleport pt = itTP.next().getValue();
            pt.ticksLeft--;
            if (pt.ticksLeft <= 0) {
                pt.from.setPositionAndUpdate(pt.targetIsland.spawnX, pt.targetIsland.spawnY, pt.targetIsland.spawnZ);
                pt.from.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.tp_success|name=" + pt.targetIsland.owner).setColor(EnumChatFormatting.GREEN));
                EntityPlayerMP targetPlayer = getOnlinePlayer(pt.targetIsland.owner);
                if (targetPlayer != null) {
                    targetPlayer.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.tp_successed|name=" + pt.from.username).setColor(EnumChatFormatting.GREEN));
                }
                itTP.remove();
            }
        }
    }

    /**
     * Helper to get an online player by name.
     */
    private static EntityPlayerMP getOnlinePlayer(String name) {
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            if (player.username.equalsIgnoreCase(name)) return player;
        }
        return null;
    }
}
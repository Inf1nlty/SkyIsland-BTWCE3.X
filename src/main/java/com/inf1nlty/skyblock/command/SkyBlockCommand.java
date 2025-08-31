package com.inf1nlty.skyblock.command;

import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
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
public class SkyBlockCommand extends CommandBase {

//    private static Boolean hasShopMod = null;
//    private static java.lang.reflect.Method getBalanceTenths = null;
//    private static java.lang.reflect.Method addTenths = null;
//
//    private static boolean checkShopMod() {
//        if (hasShopMod == null) {
//            try {
//                Class<?> moneyManagerClass = Class.forName("com.inf1nlty.shop.util.MoneyManager");
//                getBalanceTenths = moneyManagerClass.getMethod("getBalanceTenths", EntityPlayer.class);
//                addTenths = moneyManagerClass.getMethod("addTenths", EntityPlayer.class, int.class);
//                hasShopMod = true;
//            } catch (Exception e) {
//                hasShopMod = false;
//            }
//        }
//        return hasShopMod;
//    }

    // State for delayed creation and deletion, and tpa requests
    private static final Map<String, Integer> pendingIslandTeleports = new HashMap<>();
    private static final Map<String, Integer> pendingCreate = new HashMap<>();
    private static final Map<String, Long> pendingDeleteRequests = new HashMap<>();
    private static final Map<String, String> pendingTPARequests = new HashMap<>();
    private static final Map<String, PendingTeleport> pendingTeleports = new HashMap<>();
    private static final Map<String, PendingTPARequest> pendingTPATIMEOUTRequests = new HashMap<>();

    private static class PendingTeleport {
        public final EntityPlayerMP from;
        public final SkyBlockPoint targetIsland;
        public int ticksLeft;

        public PendingTeleport(EntityPlayerMP from, SkyBlockPoint targetIsland, int ticksLeft) {
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
            return getStrings(args, player);
        }
        // For /island tpa <player>|yes|no|setyes|setno
        if (args.length == 2 && args[0].equalsIgnoreCase("tpa")) {
            return getStringList(args, player);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("j")) {
            return getPlayerNameCompletions(player, args[1]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("r"))) {
            SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
            if (island != null && !island.members.isEmpty()) {
                String prefix = args[1].toLowerCase();
                List<String> members = new ArrayList<>();
                for (String member : island.members) {
                    if (member.toLowerCase().startsWith(prefix)) {
                        members.add(member);
                    }
                }
                return members;
            }
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("deny")) {
            List<String> pending = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Map.Entry<String, PendingJoin> entry : pendingJoins.entrySet()) {
                PendingJoin pj = entry.getValue();
                if (pj.owner.equals(player.username) && pj.member.toLowerCase().startsWith(prefix)) {
                    pending.add(pj.member);
                }
            }
            return pending;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("a"))) {
            List<String> pending = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Map.Entry<String, PendingJoin> entry : pendingJoins.entrySet()) {
                PendingJoin pj = entry.getValue();
                if (pj.owner.equals(player.username) && pj.member.toLowerCase().startsWith(prefix)) {
                    pending.add(pj.member);
                }
            }
            return pending;
        }
        return Collections.emptyList();
    }

    private static List<String> getStringList(String[] args, EntityPlayerMP player) {
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

    private static List<String> getStrings(String[] args, EntityPlayerMP player) {
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
        return names;
    }

    private List<String> getPlayerNameCompletions(EntityPlayerMP current, String prefix) {
        List<String> names = new ArrayList<>();
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP target = (EntityPlayerMP) obj;
            if (!target.username.equalsIgnoreCase(current.username) &&
                    target.username.toLowerCase().startsWith(prefix.toLowerCase())) {
                names.add(target.username);
            }
        }
        return names;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP player)) return;

        if (args.length == 0) {
            // /island -- delayed teleport to own island
            SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
            if (island == null) {
                island = SkyBlockDataManager.getIslandForMember(player);
            }
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
            case "protect":
            case "p":
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("on")) {
                        handleProtectToggle(player, true);
                    } else if (args[1].equalsIgnoreCase("off")) {
                        handleProtectToggle(player, false);
                    } else {
                        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protect.usage").setColor(EnumChatFormatting.YELLOW));
                    }
                } else {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protect.usage").setColor(EnumChatFormatting.YELLOW));
                }
                break;
            case "join":
            case "j":
                if (args.length >= 2) {
                    handleJoin(player, args[1]);
                } else {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.usage").setColor(EnumChatFormatting.YELLOW));
                }
                break;
            case "accept":
            case "a":
                if (args.length >= 2) {
                    handleAccept(player, args[1]);
                } else {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.accept.usage").setColor(EnumChatFormatting.YELLOW));
                }
                break;
            case "deny":
                if (args.length >= 2) {
                    handleDeny(player, args[1]);
                } else {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.deny.usage").setColor(EnumChatFormatting.YELLOW));
                }
                break;
            case "remove":
            case "r":
                if (args.length >= 2) {
                    handleRemoveMember(player, args[1]);
                } else {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.remove.usage").setColor(EnumChatFormatting.YELLOW));
                }
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
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            island = SkyBlockDataManager.getIslandForMember(player);
        }
        if (island != null) {
            String infoLine1 = String.format(
                    "commands.island.info.line1|name=%s|x=%d|y=%d|z=%d|dim=%d",
                    island.owner, island.x, island.y, island.z, island.dim
            );
            String infoLine2 = String.format(
                    "commands.island.info.line2|spawnX=%.2f|spawnY=%.2f|spawnZ=%.2f|tpa=%s",
                    island.spawnX, island.spawnY, island.spawnZ, island.tpaEnabled ? "yes" : "no"
            );
            String memberList = String.join(", ", island.members);
            String infoLine3 = String.format("commands.island.info.members|members=%s", memberList);
            player.sendChatToPlayer(ChatMessageComponent.createFromText(infoLine1).setColor(EnumChatFormatting.AQUA));
            player.sendChatToPlayer(ChatMessageComponent.createFromText(infoLine2).setColor(EnumChatFormatting.AQUA));
            player.sendChatToPlayer(ChatMessageComponent.createFromText(infoLine3).setColor(EnumChatFormatting.YELLOW));
        } else {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
        }
    }

    /**
     * Handles delayed creation of player's islands.
     */
    private static final int OVERWORLD_DIM_ID = 0;

    private void handleNew(EntityPlayerMP player) {
        if (player.dimension != OVERWORLD_DIM_ID) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.new.only_in_overworld")
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        if (SkyBlockDataManager.getIsland(player) != null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.already|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        if (SkyBlockDataManager.getIslandForMember(player) != null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.already_joined_create").setColor(EnumChatFormatting.RED));
            return;
        }

//        boolean everCreated = SkyBlockDataManager.hasEverCreatedIsland(player.username);
//        if (everCreated && checkShopMod()) {
//            try {
//                int balance = (int)getBalanceTenths.invoke(null, player);
//                if (balance < 5000) {
//                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.new.not_enough_money|cost=500")
//                            .setColor(EnumChatFormatting.RED));
//                    return;
//                }
//                addTenths.invoke(null, player, -5000);
//                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.new.cost_paid|amount=500")
//                        .setColor(EnumChatFormatting.YELLOW));
//            } catch (Exception e) {
//                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.new.shopmod_error")
//                        .setColor(EnumChatFormatting.RED));
//            }
//        }

        pendingCreate.put(player.username, 60);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.create.wait|name=" + player.username)
                .setColor(EnumChatFormatting.YELLOW));
    }

    /**
     * Schedules the player's island for deletion.
     */
    private void handleDelete(EntityPlayerMP player) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        pendingDeleteRequests.put(player.username, System.currentTimeMillis());
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.pending|name=" + player.username)
                .setColor(EnumChatFormatting.YELLOW));
    }

    /**
     * Confirms deletion of the player's island.
     */
    private void handleDeleteConfirm(EntityPlayerMP player) {
        if (!pendingDeleteRequests.containsKey(player.username)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.nopending|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        pendingDeleteRequests.remove(player.username);
        SkyBlockDataManager.setIsland(player, null);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.success|name=" + player.username)
                .setColor(EnumChatFormatting.GREEN));
    }

    /**
     * Updates the spawn location for the player's island.
     */
    private static final int SPAWN_LIMIT_DISTANCE = 30;

    private void handleSetSpawn(EntityPlayerMP player) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
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
        double dx = Math.abs(player.posX - island.initSpawnX);
        double dz = Math.abs(player.posZ - island.initSpawnZ);
        if (dx > SPAWN_LIMIT_DISTANCE || dz > SPAWN_LIMIT_DISTANCE) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.setspawn.out_of_bounds|radius=" + SPAWN_LIMIT_DISTANCE)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        SkyBlockManager.setSpawn(island, player.posX, player.posY, player.posZ);
        SkyBlockDataManager.setIsland(player, island);
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
                SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
                if (island == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username).setColor(EnumChatFormatting.RED));
                    return;
                }
                island.tpaEnabled = arg.equals("setyes");
                SkyBlockDataManager.setIsland(player, island);
                player.sendChatToPlayer(ChatMessageComponent.createFromText(
                                "commands.island.tpa.set" + (island.tpaEnabled ? "yes" : "no") + "|name=" + player.username)
                        .setColor(EnumChatFormatting.GREEN));
                return;
            }
            case "yes": {
                String requesterName = pendingTPARequests.remove(player.username);
                pendingTPATIMEOUTRequests.remove(player.username);
                if (requesterName == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.norequest").setColor(EnumChatFormatting.RED));
                    return;
                }
                EntityPlayerMP requester = getOnlinePlayer(requesterName);
                if (requester == null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.offline|name=" + requesterName).setColor(EnumChatFormatting.RED));
                    return;
                }
                SkyBlockPoint targetIsland = SkyBlockDataManager.getIsland(player);
                SkyBlockPoint requesterIsland = SkyBlockDataManager.getIsland(requester);

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
                pendingTPATIMEOUTRequests.remove(player.username);
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
        SkyBlockPoint targetIsland = SkyBlockDataManager.getIsland(target);
        SkyBlockPoint requesterIsland = SkyBlockDataManager.getIsland(player);
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
        pendingTPATIMEOUTRequests.put(target.username, new PendingTPARequest(player.username, System.currentTimeMillis()));
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.request_sent|name=" + target.username).setColor(EnumChatFormatting.AQUA));
        target.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.request|name=" + player.username).setColor(EnumChatFormatting.AQUA));
    }

    private static final Map<String, PendingJoin> pendingJoins = new HashMap<>();
    private static class PendingJoin {
        public final String member;
        public final String owner;
        public int ticks;
        public PendingJoin(String member, String owner, int ticks) {
            this.member = member;
            this.owner = owner;
            this.ticks = ticks;
        }
    }

    private static class PendingTPARequest {
        public final String requester;
        public final long requestTimeMillis;
        public PendingTPARequest(String requester, long requestTimeMillis) {
            this.requester = requester;
            this.requestTimeMillis = requestTimeMillis;
        }
    }

    private void handleProtectToggle(EntityPlayerMP player, boolean enable) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username).setColor(EnumChatFormatting.RED));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protect.not_owner").setColor(EnumChatFormatting.RED));
            return;
        }
        island.protectEnabled = enable;
        SkyBlockDataManager.setIsland(player, island);
        player.sendChatToPlayer(ChatMessageComponent.createFromText(
                enable ? "commands.island.protect.enabled" : "commands.island.protect.disabled").setColor(EnumChatFormatting.GREEN));
    }

    private void handleJoin(EntityPlayerMP player, String ownerName) {
        SkyBlockPoint selfIsland = SkyBlockDataManager.getIsland(player);
        if (selfIsland != null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.already_have_island").setColor(EnumChatFormatting.RED));
            return;
        }
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP onlinePlayer = (EntityPlayerMP) obj;
            SkyBlockPoint otherIsland = SkyBlockDataManager.getIsland(onlinePlayer);
            if (otherIsland != null && otherIsland.members.contains(player.username)) {
                player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.already_joined_join").setColor(EnumChatFormatting.RED));
                return;
            }
        }
        if (pendingJoins.containsKey(player.username)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.already_pending").setColor(EnumChatFormatting.RED));
            return;
        }
        EntityPlayerMP owner = getOnlinePlayer(ownerName);
        if (owner == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.owner_offline|name=" + ownerName).setColor(EnumChatFormatting.RED));
            return;
        }
        SkyBlockPoint island = SkyBlockDataManager.getIsland(owner);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.no_island|name=" + ownerName).setColor(EnumChatFormatting.RED));
            return;
        }
        if (island.members.contains(player.username)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.already_member|name=" + ownerName).setColor(EnumChatFormatting.RED));
            return;
        }
        pendingJoins.put(player.username, new PendingJoin(player.username, ownerName, 1200));
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.request_sent|name=" + ownerName).setColor(EnumChatFormatting.YELLOW));
        owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.request|name=" + player.username).setColor(EnumChatFormatting.AQUA));
    }

    private void handleAccept(EntityPlayerMP owner, String memberName) {
        PendingJoin pj = pendingJoins.get(memberName);
        if (pj == null || !pj.owner.equals(owner.username)) {
            owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.no_request|name=" + memberName).setColor(EnumChatFormatting.RED));
            return;
        }
        pendingJoins.remove(memberName);
        SkyBlockPoint island = SkyBlockDataManager.getIsland(owner);
        if (island != null) {
            island.members.add(memberName);
            SkyBlockDataManager.setIsland(owner, island);
            EntityPlayerMP member = getOnlinePlayer(memberName);
            if (member != null) {
                member.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.accepted|name=" + owner.username).setColor(EnumChatFormatting.GREEN));
            }
            owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.accept_success|name=" + memberName).setColor(EnumChatFormatting.GREEN));
        }
    }

    private void handleDeny(EntityPlayerMP owner, String memberName) {
        PendingJoin pj = pendingJoins.get(memberName);
        if (pj == null || !pj.owner.equals(owner.username)) {
            owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.no_request|name=" + memberName).setColor(EnumChatFormatting.RED));
            return;
        }
        pendingJoins.remove(memberName);
        EntityPlayerMP member = getOnlinePlayer(memberName);
        if (member != null) {
            member.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.denied|name=" + owner.username).setColor(EnumChatFormatting.RED));
        }
        owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.deny_success|name=" + memberName).setColor(EnumChatFormatting.GREEN));
    }

    private void handleRemoveMember(EntityPlayerMP player, String memberName) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.notfound|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.remove.not_owner").setColor(EnumChatFormatting.RED));
            return;
        }
        if (!island.members.contains(memberName)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.remove.not_member|name=" + memberName)
                    .setColor(EnumChatFormatting.RED));
            return;
        }
        island.members.remove(memberName);
        SkyBlockDataManager.setIsland(player, island);
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.remove.success|name=" + memberName)
                .setColor(EnumChatFormatting.GREEN));
        EntityPlayerMP removed = getOnlinePlayer(memberName);
        if (removed != null) {
            removed.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.remove.kicked|name=" + player.username)
                    .setColor(EnumChatFormatting.RED));
        }
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
                if (player != null && SkyBlockDataManager.getIsland(player) == null) {
                    boolean everCreated = SkyBlockDataManager.hasEverCreatedIsland(player.username);
                    SkyBlockPoint island = SkyBlockManager.makeIsland(player, (WorldServer) world);
                    SkyBlockDataManager.setIsland(player, island);
                    SkyBlockManager.generateIsland(world, island);
                    pendingIslandTeleports.put(player.username, 60);
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.create.success|name=" + player.username)
                            .setColor(EnumChatFormatting.GREEN));
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tp_wait|name=" + player.username)
                            .setColor(EnumChatFormatting.YELLOW));
                    if (everCreated) {
                        String[] tauntKeys = {
                                "commands.island.taunt.1", "commands.island.taunt.2", "commands.island.taunt.3",
                                "commands.island.taunt.4", "commands.island.taunt.5", "commands.island.taunt.6",
                                "commands.island.taunt.7", "commands.island.taunt.8", "commands.island.taunt.9",
                                "commands.island.taunt.10"
                        };
                        String tauntKey = tauntKeys[(int) (Math.random() * tauntKeys.length)];
                        String tauntMsg = StatCollector.translateToLocal(tauntKey).replace("{name}", player.username);
                        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                            EntityPlayerMP target = (EntityPlayerMP) obj;
                            target.sendChatToPlayer(ChatMessageComponent.createFromText(tauntMsg).setColor(EnumChatFormatting.LIGHT_PURPLE));
                        }
                    }
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
                    SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
                    if (island == null) {
                        island = SkyBlockDataManager.getIslandForMember(player);
                    }
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
        Iterator<Map.Entry<String, Long>> itDelete = pendingDeleteRequests.entrySet().iterator();
        while (itDelete.hasNext()) {
            Map.Entry<String, Long> entry = itDelete.next();
            if (System.currentTimeMillis() - entry.getValue() > 60_000) {
                EntityPlayerMP player = getOnlinePlayer(entry.getKey());
                if (player != null) {
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.delete.timeout|name=" + player.username)
                            .setColor(EnumChatFormatting.RED));
                }
                itDelete.remove();
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
        Iterator<Map.Entry<String, PendingTPARequest>> itTPA = pendingTPATIMEOUTRequests.entrySet().iterator();
        while (itTPA.hasNext()) {
            Map.Entry<String, PendingTPARequest> entry = itTPA.next();
            if (System.currentTimeMillis() - entry.getValue().requestTimeMillis > 60_000) {
                EntityPlayerMP target = getOnlinePlayer(entry.getKey());
                EntityPlayerMP requester = getOnlinePlayer(entry.getValue().requester);
                if (requester != null) {
                    requester.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.timeout|name=" + entry.getKey()).setColor(EnumChatFormatting.RED));
                }
                if (target != null) {
                    target.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.tpa.timeout_notice|name=" + entry.getValue().requester).setColor(EnumChatFormatting.RED));
                }
                itTPA.remove();
            }
        }
        Iterator<Map.Entry<String, PendingJoin>> itJoin = pendingJoins.entrySet().iterator();
        while (itJoin.hasNext()) {
            Map.Entry<String, PendingJoin> e = itJoin.next();
            PendingJoin pj = e.getValue();
            pj.ticks--;
            if (pj.ticks <= 0) {
                EntityPlayerMP member = getOnlinePlayer(pj.member);
                EntityPlayerMP owner = getOnlinePlayer(pj.owner);
                if (member != null) {
                    member.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.timeout|name=" + pj.owner).setColor(EnumChatFormatting.RED));
                }
                if (owner != null) {
                    owner.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.join.timeout_notice|name=" + pj.member).setColor(EnumChatFormatting.RED));
                }
                itJoin.remove();
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
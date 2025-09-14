package com.inf1nlty.skyblock.command;

import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.*;

import java.io.File;
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

    private static Boolean hasShopMod = null;
    private static java.lang.reflect.Method getBalanceTenths = null;
    private static java.lang.reflect.Method addTenths = null;

    private static boolean checkShopMod() {
        if (hasShopMod == null) {
            try {
                Class<?> moneyManagerClass = Class.forName("com.inf1nlty.shop.util.MoneyManager");
                getBalanceTenths = moneyManagerClass.getMethod("getBalanceTenths", EntityPlayer.class);
                addTenths = moneyManagerClass.getMethod("addTenths", EntityPlayer.class, int.class);
                hasShopMod = true;
            } catch (Exception e) {
                hasShopMod = false;
            }
        }
        return hasShopMod;
    }

    // State for delayed creation and deletion, and tpa requests
    private static final Map<String, Integer> pendingIslandTeleports = new HashMap<>();
    private static final Map<String, Integer> pendingCreate = new HashMap<>();
    private static final Map<String, Long> pendingDeleteRequests = new HashMap<>();
    private static final Map<String, Long> pendingLeaveRequests = new HashMap<>();
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
                player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                        EnumChatFormatting.RED, false, false, false, player.username));
                return;
            }
            // /island -- delayed teleport to own island
            if (pendingIslandTeleports.containsKey(player.username)) {
                player.sendChatToPlayer(createFormattedMessage("commands.island.tp_pending",
                        EnumChatFormatting.YELLOW, false, false, false, player.username));
            } else {
                pendingIslandTeleports.put(player.username, 60); // 5 seconds
                player.sendChatToPlayer(createFormattedMessage("commands.island.tp_wait",
                        EnumChatFormatting.YELLOW, false, false, false, player.username));
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
                    player.sendChatToPlayer(createMessage("commands.island.tpa.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
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
                        player.sendChatToPlayer(createMessage("commands.island.protect.usage",
                                EnumChatFormatting.YELLOW, false, false, false));
                    }
                } else {
                    player.sendChatToPlayer(createMessage("commands.island.protect.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
                }
                break;
            case "join":
            case "j":
                if (args.length >= 2) {
                    handleJoin(player, args[1]);
                } else {
                    player.sendChatToPlayer(createMessage("commands.island.join.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
                }
                break;
            case "accept":
            case "a":
                if (args.length >= 2) {
                    handleAccept(player, args[1]);
                } else {
                    player.sendChatToPlayer(createMessage("commands.island.join.accept.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
                }
                break;
            case "deny":
                if (args.length >= 2) {
                    handleDeny(player, args[1]);
                } else {
                    player.sendChatToPlayer(createMessage("commands.island.join.deny.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
                }
                break;
            case "remove":
            case "r":
                if (args.length >= 2) {
                    handleRemoveMember(player, args[1]);
                } else {
                    player.sendChatToPlayer(createMessage("commands.island.remove.usage",
                            EnumChatFormatting.YELLOW, false, false, false));
                }
                break;
            case "leave":
            case "l":
                if (args.length > 1 && (args[1].equalsIgnoreCase("confirm") || args[1].equalsIgnoreCase("c"))) {
                    handleLeaveConfirm(player);
                } else {
                    handleLeave(player);
                }
                break;
            case "reload":
                handleReload(player);
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
            player.sendChatToPlayer(createFormattedMessage("commands.island.info.line1",
                    EnumChatFormatting.AQUA, false, false, false, island.owner, island.x, island.y, island.z, island.dim));

            player.sendChatToPlayer(createFormattedMessage("commands.island.info.line2",
                    EnumChatFormatting.AQUA, false, false, false, island.spawnX, island.spawnY, island.spawnZ, island.tpaEnabled ? "yes" : "no"));

            String memberList = String.join(", ", island.members);

            player.sendChatToPlayer(createFormattedMessage("commands.island.info.members",
                    EnumChatFormatting.YELLOW, false, false, false, memberList));
        } else {
            player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                    EnumChatFormatting.RED, false, false, false, player.username));
        }
    }

    private void handleReload(EntityPlayerMP player) {
        com.inf1nlty.skyblock.SkyblockConfig.reload();
        player.sendChatToPlayer(createMessage("commands.island.reload.success",
                EnumChatFormatting.GREEN, false, false, false));
    }

    /**
     * Handles delayed creation of player's islands.
     */
    private static final int OVERWORLD_DIM_ID = 0;

    private void handleNew(EntityPlayerMP player) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) {
            player.sendChatToPlayer(createMessage("commands.island.new.only_in_voidworld",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        if (player.dimension != OVERWORLD_DIM_ID) {
            player.sendChatToPlayer(createMessage("commands.island.new.only_in_overworld",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        if (SkyBlockDataManager.getIsland(player) != null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.already",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (SkyBlockDataManager.getIslandForMember(player) != null) {
            player.sendChatToPlayer(createMessage("commands.island.already_joined_create",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }

        boolean everCreated = SkyBlockDataManager.hasEverCreatedIsland(player.username);
        if (everCreated && checkShopMod()) {
            try {
                int balance = (int)getBalanceTenths.invoke(null, player);
                if (balance < 5000) {
                    player.sendChatToPlayer(createFormattedMessage("commands.island.new.not_enough_money",
                            EnumChatFormatting.RED, false, false, false, 500));
                    return;
                }
                addTenths.invoke(null, player, -5000);
                player.sendChatToPlayer(createFormattedMessage("commands.island.new.cost_paid",
                        EnumChatFormatting.YELLOW, false, false, false, 500));
            } catch (Exception e) {
                player.sendChatToPlayer(createMessage("commands.island.new.shopmod_error",
                        EnumChatFormatting.RED, false, false, false));
            }
        }

        pendingCreate.put(player.username, 60);
        player.sendChatToPlayer(createFormattedMessage("commands.island.create.wait",
                EnumChatFormatting.YELLOW, false, false, false, player.username));
    }

    /**
     * Schedules the player's island for deletion.
     */
    private void handleDelete(EntityPlayerMP player) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(createMessage("commands.island.not_owner",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        if (pendingDeleteRequests.containsKey(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.delete.already_pending",
                    EnumChatFormatting.YELLOW, false, false, false, player.username));
            return;
        }
        pendingDeleteRequests.put(player.username, System.currentTimeMillis());
        player.sendChatToPlayer(createFormattedMessage("commands.island.delete.pending",
                EnumChatFormatting.YELLOW, false, false, false, player.username));
    }

    /**
     * Confirms deletion of the player's island.
     */
    private void handleDeleteConfirm(EntityPlayerMP player) {
        if (!pendingDeleteRequests.containsKey(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.delete.nopending",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        pendingDeleteRequests.remove(player.username);

        if (player.dimension != 0) {
            MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player, 0);
        }
        player.setPositionAndUpdate(0.5, 101, 0.5);

        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island != null) {
            for (String member : new HashSet<>(island.members)) {
                SkyBlockDataManager.setGlobalMember(member, null);
                SkyBlockDataManager.setIsland(member, null);
            }
            if (!player.username.equals(island.owner)) {
                player.sendChatToPlayer(createMessage("commands.island.not_owner",
                        EnumChatFormatting.RED, false, false, false));
                return;
            }
            SkyBlockDataManager.setGlobalMember(player.username, null);
            SkyBlockDataManager.setIsland(player, null);
            island.members.clear();

            WorldServer world = MinecraftServer.getServer().worldServers[0];
            ISaveHandler saveHandler = world.getSaveHandler();
            String worldDirName = saveHandler.getWorldDirectoryName();
            File worldDir = new File("saves", worldDirName);
            if (!worldDir.exists()) {
                worldDir = new File(worldDirName);
            }

            SkyBlockManager.freeIslandRegions(island);
            pendingIslandDeletes.put(player.username, new DeleteTask(island, worldDir, world, 60));
        }
        player.sendChatToPlayer(createFormattedMessage("commands.island.delete.success",
                EnumChatFormatting.GREEN, false, false, false, player.username));
    }

    /**
     * Updates the spawn location for the player's island.
     */
    private static final int SPAWN_LIMIT_DISTANCE = 30;

    private void handleSetSpawn(EntityPlayerMP player) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(createMessage("commands.island.not_owner",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        if (player.dimension != island.dim) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.setspawn_dim_mismatch",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        double dx = Math.abs(player.posX - island.initSpawnX);
        double dz = Math.abs(player.posZ - island.initSpawnZ);
        if (dx > SPAWN_LIMIT_DISTANCE || dz > SPAWN_LIMIT_DISTANCE) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.setspawn.out_of_bounds",
                    EnumChatFormatting.RED, false, false, false, SPAWN_LIMIT_DISTANCE));
            return;
        }
        SkyBlockManager.setSpawn(island, player.posX, player.posY, player.posZ);
        SkyBlockDataManager.setIsland(player, island);
        player.sendChatToPlayer(createFormattedMessage("commands.island.setspawn.success",
                EnumChatFormatting.GREEN, false, false, false, player.username));
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
                    player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                            EnumChatFormatting.RED, false, false, false, player.username));
                    return;
                }
                if (!player.username.equals(island.owner)) {
                    player.sendChatToPlayer(createMessage("commands.island.not_owner",
                            EnumChatFormatting.RED, false, false, false));
                    return;
                }
                island.tpaEnabled = arg.equals("setyes");
                SkyBlockDataManager.setIsland(player, island);
                player.sendChatToPlayer(createFormattedMessage(
                        "commands.island.tpa.set" + (island.tpaEnabled ? "yes" : "no"),
                        EnumChatFormatting.GREEN, false, false, false, player.username));
                return;
            }
            case "yes": {
                String requesterName = pendingTPARequests.remove(player.username);
                pendingTPATIMEOUTRequests.remove(player.username);
                if (requesterName == null) {
                    player.sendChatToPlayer(createMessage("commands.island.tpa.norequest",
                            EnumChatFormatting.RED, false, false, false));
                    return;
                }
                EntityPlayerMP requester = getOnlinePlayer(requesterName);
                if (requester == null) {
                    player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.offline",
                            EnumChatFormatting.RED, false, false, false, requesterName));
                    return;
                }
                SkyBlockPoint targetIsland = SkyBlockDataManager.getIsland(player);
                SkyBlockPoint requesterIsland = SkyBlockDataManager.getIsland(requester);

                if (!targetIsland.tpaEnabled) {
                    player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.notenabled",
                            EnumChatFormatting.RED, false, false, false, player.username));

                    requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.notenabled",
                            EnumChatFormatting.RED, false, false, false, player.username));
                    return;
                }
                if (targetIsland.dim != requester.dimension) {
                    player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.dim_mismatch",
                            EnumChatFormatting.RED, false, false, false, requester.username));

                    requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.dim_mismatch",
                            EnumChatFormatting.RED, false, false, false, player.username));
                    return;
                }
                pendingTeleports.put(requester.username, new PendingTeleport(requester, targetIsland, 60));
                player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.accepted",
                        EnumChatFormatting.GREEN, false, false, false, requester.username));

                requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.accepted_by",
                        EnumChatFormatting.GREEN, false, false, false, player.username));

                requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.wait",
                        EnumChatFormatting.YELLOW, false, false, false, player.username));
                return;
            }
            case "no": {
                String requesterName = pendingTPARequests.remove(player.username);
                pendingTPATIMEOUTRequests.remove(player.username);
                if (requesterName == null) {
                    player.sendChatToPlayer(createMessage("commands.island.tpa.norequest", EnumChatFormatting.RED, false, false, false));
                    return;
                }
                EntityPlayerMP requester = getOnlinePlayer(requesterName);
                if (requester != null) {
                    requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.denied_by", EnumChatFormatting.RED, false, false, false, player.username));
                }
                player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.denied", EnumChatFormatting.RED, false, false, false, requesterName));
                return;
            }
        }

        if (arg.equalsIgnoreCase(player.username)) {
            player.sendChatToPlayer(createMessage("commands.island.tpa.self", EnumChatFormatting.RED, false, false, false));
            return;
        }
        EntityPlayerMP target = getOnlinePlayer(arg);
        if (target == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.offline", EnumChatFormatting.RED, false, false, false, arg));
            return;
        }
        SkyBlockPoint targetIsland = SkyBlockDataManager.getIsland(target);
        SkyBlockPoint requesterIsland = SkyBlockDataManager.getIsland(player);
        if (targetIsland == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.target_no_island", EnumChatFormatting.RED, false, false, false, target.username));
            return;
        }
        if (!targetIsland.tpaEnabled) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.notenabled", EnumChatFormatting.RED, false, false, false, target.username));
            return;
        }
        if (targetIsland.dim != player.dimension) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.dim_mismatch", EnumChatFormatting.RED, false, false, false, target.username));
            return;
        }
        if (pendingTPARequests.containsKey(target.username)) {
            String oldRequester = pendingTPARequests.get(target.username);
            EntityPlayerMP oldRequesterPlayer = getOnlinePlayer(oldRequester);
            if (oldRequesterPlayer == null)
            {
                pendingTPARequests.remove(target.username);
                pendingTPATIMEOUTRequests.remove(target.username);
            }
            else {
                player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.already_pending", EnumChatFormatting.YELLOW, false, false, false, target.username));
                return;
            }
        }
        pendingTPARequests.put(target.username, player.username);
        pendingTPATIMEOUTRequests.put(target.username, new PendingTPARequest(player.username, System.currentTimeMillis()));
        player.sendChatToPlayer(createFormattedMessage("commands.island.tpa.request_sent", EnumChatFormatting.AQUA, false, false, false, target.username));
        target.sendChatToPlayer(createFormattedMessage("commands.island.tpa.request", EnumChatFormatting.AQUA, false, false, false, player.username));
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
            player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(createMessage("commands.island.not_owner",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        island.protectEnabled = enable;
        SkyBlockDataManager.setIsland(player, island);
        player.sendChatToPlayer(createMessage(
                enable ? "commands.island.protect.enabled" : "commands.island.protect.disabled",
                EnumChatFormatting.GREEN, false, false, false));
    }

    private void handleJoin(EntityPlayerMP player, String ownerName) {
        SkyBlockPoint selfIsland = SkyBlockDataManager.getIsland(player);
        if (selfIsland != null) {
            player.sendChatToPlayer(createMessage("commands.island.join.already_have_island",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP onlinePlayer = (EntityPlayerMP) obj;
            SkyBlockPoint otherIsland = SkyBlockDataManager.getIsland(onlinePlayer);
            if (otherIsland != null && otherIsland.members.contains(player.username)) {
                player.sendChatToPlayer(createMessage("commands.island.already_joined_join",
                        EnumChatFormatting.RED, false, false, false));
                return;
            }
        }
        if (pendingJoins.containsKey(player.username)) {
            player.sendChatToPlayer(createMessage("commands.island.join.already_pending",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        EntityPlayerMP owner = getOnlinePlayer(ownerName);
        if (owner == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.join.owner_offline",
                    EnumChatFormatting.RED, false, false, false, ownerName));
            return;
        }
        SkyBlockPoint island = SkyBlockDataManager.getIsland(owner);
        if (island == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.join.no_island",
                    EnumChatFormatting.RED, false, false, false, ownerName));
            return;
        }
        if (island.members.contains(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.join.already_member",
                    EnumChatFormatting.RED, false, false, false, ownerName));
            return;
        }
        pendingJoins.put(player.username, new PendingJoin(player.username, ownerName, 1200));
        player.sendChatToPlayer(createFormattedMessage("commands.island.join.request_sent",
                EnumChatFormatting.YELLOW, false, false, false, ownerName));

        owner.sendChatToPlayer(createFormattedMessage("commands.island.join.request",
                EnumChatFormatting.AQUA, false, false, false,
                player.username, player.username, player.username, player.username, player.username));
    }

    private void handleAccept(EntityPlayerMP owner, String memberName) {
        PendingJoin pj = pendingJoins.get(memberName);
        if (pj == null || !pj.owner.equals(owner.username)) {
            owner.sendChatToPlayer(createFormattedMessage("commands.island.join.no_request",
                    EnumChatFormatting.RED, false, false, false, memberName));
            return;
        }
        pendingJoins.remove(memberName);
        SkyBlockPoint island = SkyBlockDataManager.getIsland(owner);
        if (island != null) {
            island.members.add(memberName);
            syncAllMembers(island);
            SkyBlockDataManager.setGlobalMember(memberName, owner.username);
            EntityPlayerMP member = getOnlinePlayer(memberName);
            if (member != null) {
                member.sendChatToPlayer(createFormattedMessage("commands.island.join.accepted",
                        EnumChatFormatting.GREEN, false, false, false, owner.username));
            }
            owner.sendChatToPlayer(createFormattedMessage("commands.island.join.accept_success",
                    EnumChatFormatting.GREEN, false, false, false, memberName));
        }
    }

    private void handleDeny(EntityPlayerMP owner, String memberName) {
        PendingJoin pj = pendingJoins.get(memberName);
        if (pj == null || !pj.owner.equals(owner.username)) {
            owner.sendChatToPlayer(createFormattedMessage("commands.island.join.no_request",
                    EnumChatFormatting.RED, false, false, false, memberName));
            return;
        }
        pendingJoins.remove(memberName);
        EntityPlayerMP member = getOnlinePlayer(memberName);
        if (member != null) {
            member.sendChatToPlayer(createFormattedMessage("commands.island.join.denied",
                    EnumChatFormatting.RED, false, false, false, owner.username));
        }
        owner.sendChatToPlayer(createFormattedMessage("commands.island.join.deny_success",
                EnumChatFormatting.GREEN, false, false, false, memberName));
    }

    private void handleRemoveMember(EntityPlayerMP player, String memberName) {
        SkyBlockPoint island = SkyBlockDataManager.getIsland(player);
        if (island == null) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (!player.username.equals(island.owner)) {
            player.sendChatToPlayer(createMessage("commands.island.remove.not_owner",
                    EnumChatFormatting.RED, false, false, false));
            return;
        }
        if (!island.members.contains(memberName)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.remove.not_member",
                    EnumChatFormatting.RED, false, false, false, memberName));
            return;
        }
        island.members.remove(memberName);
        SkyBlockDataManager.setIsland(memberName, null);
        SkyBlockDataManager.setGlobalMember(memberName, null);
        syncAllMembers(island);
        player.sendChatToPlayer(createFormattedMessage("commands.island.remove.success",
                EnumChatFormatting.GREEN, false, false, false, memberName));
        EntityPlayerMP removed = getOnlinePlayer(memberName);
        if (removed != null) {
            SkyBlockDataManager.setIsland(removed, null);
            removed.sendChatToPlayer(createFormattedMessage("commands.island.remove.kicked",
                    EnumChatFormatting.RED, false, false, false, player.username));
        }
    }

    private void handleLeave(EntityPlayerMP player) {
        SkyBlockPoint island = SkyBlockDataManager.getIslandForMember(player);
        if (island == null || island.owner.equals(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.leave.not_member",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        if (pendingLeaveRequests.containsKey(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.leave.already_pending",
                    EnumChatFormatting.YELLOW, false, false, false, player.username));
            return;
        }
        pendingLeaveRequests.put(player.username, System.currentTimeMillis());
        player.sendChatToPlayer(createFormattedMessage("commands.island.leave.pending",
                EnumChatFormatting.YELLOW, false, false, false, player.username));
    }

    private void handleLeaveConfirm(EntityPlayerMP player) {
        if (!pendingLeaveRequests.containsKey(player.username)) {
            player.sendChatToPlayer(createFormattedMessage("commands.island.leave.nopending",
                    EnumChatFormatting.RED, false, false, false, player.username));
            return;
        }
        SkyBlockPoint island = SkyBlockDataManager.getIslandForMember(player);
        if (island != null && !island.owner.equals(player.username)) {
            island.members.remove(player.username);
            SkyBlockDataManager.setIsland(player, null);
            SkyBlockDataManager.setGlobalMember(player.username, null);
            syncAllMembers(island);
            pendingLeaveRequests.remove(player.username);
            player.sendChatToPlayer(createFormattedMessage("commands.island.leave.success",
                    EnumChatFormatting.GREEN, false, false, false, player.username));
        } else {
            pendingLeaveRequests.remove(player.username);
            player.sendChatToPlayer(createFormattedMessage("commands.island.leave.not_member",
                    EnumChatFormatting.RED, false, false, false, player.username));
        }
    }

    private static final Map<String, DeleteTask> pendingIslandDeletes = new HashMap<>();

    public static class DeleteTask {
        public final SkyBlockPoint island;
        public final File worldDir;
        public final WorldServer world;
        public int ticksLeft;
        public DeleteTask(SkyBlockPoint island, File worldDir, WorldServer world, int delayTicks) {
            this.island = island;
            this.worldDir = worldDir;
            this.world = world;
            this.ticksLeft = delayTicks;
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
                    player.sendChatToPlayer(createFormattedMessage("commands.island.create.success",
                            EnumChatFormatting.GREEN, false, false, false, player.username));

                    player.sendChatToPlayer(createFormattedMessage("commands.island.tp_wait",
                            EnumChatFormatting.YELLOW, false, false, false, player.username));
                    if (everCreated) {
                        String[] tauntKeys = {
                                "commands.island.taunt.1", "commands.island.taunt.2", "commands.island.taunt.3",
                                "commands.island.taunt.4", "commands.island.taunt.5", "commands.island.taunt.6",
                                "commands.island.taunt.7", "commands.island.taunt.8", "commands.island.taunt.9",
                                "commands.island.taunt.10"
                        };
                        String tauntKey = tauntKeys[(int) (Math.random() * tauntKeys.length)];
                        String tauntMsg = tauntKey;
                        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                            EntityPlayerMP target = (EntityPlayerMP) obj;
                            target.sendChatToPlayer(createFormattedMessage(tauntMsg,
                                    EnumChatFormatting.LIGHT_PURPLE, false, false, false, player.username));
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
                        player.sendChatToPlayer(createFormattedMessage("commands.island.tp_success",
                                EnumChatFormatting.GREEN, false, false, false, player.username));
                    } else if (island == null) {
                        player.sendChatToPlayer(createFormattedMessage("commands.island.notfound",
                                EnumChatFormatting.RED, false, false, false, player.username));
                    } else {
                        player.sendChatToPlayer(createFormattedMessage("commands.island.setspawn_dim_mismatch",
                                EnumChatFormatting.RED, false, false, false, player.username));
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
                    player.sendChatToPlayer(createFormattedMessage("commands.island.delete.timeout",
                            EnumChatFormatting.RED, false, false, false, player.username));
                }
                itDelete.remove();
            }
        }

        Iterator<Map.Entry<String, DeleteTask>> itDel = pendingIslandDeletes.entrySet().iterator();
        while (itDel.hasNext()) {
            Map.Entry<String, DeleteTask> entry = itDel.next();
            DeleteTask task = entry.getValue();
            task.ticksLeft--;
            if (task.ticksLeft <= 0) {
                SkyBlockManager.deleteIslandRegionFile(task.island, task.worldDir, task.world);
                itDel.remove();
            }
        }

        // Handle leave requests timeouts
        Iterator<Map.Entry<String, Long>> itLeave = pendingLeaveRequests.entrySet().iterator();
        while (itLeave.hasNext()) {
            Map.Entry<String, Long> entry = itLeave.next();
            if (System.currentTimeMillis() - entry.getValue() > 60_000) {
                EntityPlayerMP player = getOnlinePlayer(entry.getKey());
                if (player != null) {
                    player.sendChatToPlayer(createFormattedMessage("commands.island.leave.timeout",
                            EnumChatFormatting.RED, false, false, false, player.username));
                }
                itLeave.remove();
            }
        }

        // Handle pending teleports
        Iterator<Map.Entry<String, PendingTeleport>> itTP = pendingTeleports.entrySet().iterator();
        while (itTP.hasNext()) {
            PendingTeleport pt = itTP.next().getValue();
            pt.ticksLeft--;
            if (pt.ticksLeft <= 0) {
                pt.from.setPositionAndUpdate(pt.targetIsland.spawnX, pt.targetIsland.spawnY, pt.targetIsland.spawnZ);
                pt.from.sendChatToPlayer(createFormattedMessage("commands.island.tpa.tp_success",
                        EnumChatFormatting.GREEN, false, false, false, pt.targetIsland.owner));
                EntityPlayerMP targetPlayer = getOnlinePlayer(pt.targetIsland.owner);
                if (targetPlayer != null) {
                    targetPlayer.sendChatToPlayer(createFormattedMessage("commands.island.tpa.tp_successed",
                            EnumChatFormatting.GREEN, false, false, false, pt.from.username));
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
                    requester.sendChatToPlayer(createFormattedMessage("commands.island.tpa.timeout",
                            EnumChatFormatting.RED, false, false, false, entry.getKey()));
                }
                if (target != null) {
                    target.sendChatToPlayer(createFormattedMessage("commands.island.tpa.timeout_notice",
                            EnumChatFormatting.RED, false, false, false, entry.getValue().requester));
                }
                pendingTPARequests.remove(entry.getKey());
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
                    member.sendChatToPlayer(createFormattedMessage("commands.island.join.timeout",
                            EnumChatFormatting.RED, false, false, false, pj.owner));
                }
                if (owner != null) {
                    owner.sendChatToPlayer(createFormattedMessage("commands.island.join.timeout_notice",
                            EnumChatFormatting.RED, false, false, false, pj.member));
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

    private void syncAllMembers(SkyBlockPoint island) {
        SkyBlockDataManager.setIsland(island.owner, island);
    }

    public static ChatMessageComponent createMessage(String key, EnumChatFormatting color, boolean bold, boolean italic, boolean underline) {
        ChatMessageComponent message = new ChatMessageComponent();
        message.addKey(key);
        message.setColor(color);
        message.setBold(bold);
        message.setItalic(italic);
        message.setUnderline(underline);
        return message;
    }

    public static ChatMessageComponent createFormattedMessage(String key, EnumChatFormatting color, boolean bold, boolean italic, boolean underline, Object... args) {
        ChatMessageComponent message = new ChatMessageComponent();
        message.addFormatted(key, args);
        message.setColor(color);
        message.setBold(bold);
        message.setItalic(italic);
        message.setUnderline(underline);
        return message;
    }

}
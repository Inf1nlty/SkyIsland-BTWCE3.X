package btw.community.skyblock;

import btw.BTWAddon;
import btw.AddonHandler;
import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.command.*;
import com.inf1nlty.skyblock.network.VoidWorldSyncNet;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NetServerHandler;

public class SkyBlockAddon extends BTWAddon {

    @Override
    public void initialize() {
        AddonHandler.logMessage(getName() + " v" + getVersionString() + " Initializing...");
        AddonHandler.registerCommand(new SkyBlockCommand(), false);
        AddonHandler.registerCommand(new SkyBlockAliasCommand(), false);

        SkyblockConfig.reload();

        VoidWorldSyncNet.register(this);

    }

    @Override
    public void serverPlayerConnectionInitialized(NetServerHandler serverHandler, EntityPlayerMP playerMP) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            VoidWorldSyncNet.sendGeneratorOptionsTo(playerMP);
        }
    }
}
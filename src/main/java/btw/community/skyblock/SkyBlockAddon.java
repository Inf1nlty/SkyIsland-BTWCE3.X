package btw.community.skyblock;

import btw.BTWAddon;
import btw.AddonHandler;
import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.command.*;

public class SkyBlockAddon extends BTWAddon {
    @Override
    public void initialize() {
        AddonHandler.logMessage(getName() + " v" + getVersionString() + " Initializing...");
        AddonHandler.registerCommand(new SkyBlockCommand(), false);
        AddonHandler.registerCommand(new SkyBlockAliasCommand(), false);

        SkyblockConfig.reload();

    }
}
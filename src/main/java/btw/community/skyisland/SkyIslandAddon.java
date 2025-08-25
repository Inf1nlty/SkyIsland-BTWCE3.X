package btw.community.skyisland;

import btw.BTWAddon;
import btw.AddonHandler;
import com.inf1nlty.skyisland.command.*;

public class SkyIslandAddon extends BTWAddon {
    @Override
    public void initialize() {
        AddonHandler.logMessage(getName() + " v" + getVersionString() + " Initializing...");
        AddonHandler.registerCommand(new IslandCommand(), false);
        AddonHandler.registerCommand(new IslandAliasCommand(), false);
    }
}
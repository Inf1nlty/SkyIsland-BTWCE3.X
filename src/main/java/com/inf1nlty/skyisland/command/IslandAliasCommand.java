package com.inf1nlty.skyisland.command;

import com.inf1nlty.skyisland.command.IslandCommand;

/**
 * Alias for /island, supports all subcommands.
 * Usage: /is [i|n|d|s| ...
 */
public class IslandAliasCommand extends IslandCommand {
    @Override
    public String getCommandName() {
        return "is";
    }
}
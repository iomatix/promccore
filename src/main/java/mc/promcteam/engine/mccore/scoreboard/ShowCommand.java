/**
 * MCCore
 * com.rit.sucy.scoreboard.ShowCommand
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Steven Sucy
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software") to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package mc.promcteam.engine.mccore.scoreboard;

import mc.promcteam.engine.mccore.commands.CommandHandler;
import mc.promcteam.engine.mccore.commands.ICommand;
import mc.promcteam.engine.mccore.commands.SenderType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Shows a desired scoreboard for the player
 */
public class ShowCommand implements ICommand {

    /**
     * Executes the command
     *
     * @param handler command handler
     * @param plugin  plugin reference
     * @param sender  sender of the command
     * @param args    command arguments
     */
    @Override
    public void execute(CommandHandler handler, Plugin plugin, CommandSender sender, String[] args) {
        if (args.length > 0) {
            String name = args[0];
            for (int i = 1; i < args.length; i++) {
                name += " " + args[i];
            }
            PlayerBoards board = BoardManager.getPlayerBoards(sender.getName());
            if (board.showBoard(name))
                sender.sendMessage(ChatColor.DARK_GREEN + "Your scoreboard has been changed");
            else
                sender.sendMessage(ChatColor.DARK_RED + "You do not have a scoreboard with that name");
        } else handler.displayUsage(sender);
    }

    /**
     * @return permission needed for this command
     */
    @Override
    public String getPermissionNode() {
        return ScoreboardNodes.SHOW.getNode();
    }

    /**
     * @return args string
     */
    @Override
    public String getArgsString() {
        return "<boardName>";
    }

    /**
     * @return description
     */
    @Override
    public String getDescription() {
        return "Shows the scoreboard";
    }

    /**
     * Sender required for the command
     */
    @Override
    public SenderType getSenderType() {
        return SenderType.PLAYER_ONLY;
    }
}

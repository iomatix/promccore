/**
 * MCCore
 * com.rit.sucy.scoreboard.Board
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

import mc.promcteam.engine.NexEngine;
import mc.promcteam.engine.mccore.util.VersionManager;
import mc.promcteam.engine.utils.Reflex;
import mc.promcteam.engine.utils.reflection.ReflectionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * A manager for a scoreboard
 */
public abstract class Board {
    private static Constructor objConstructor;
    private static Constructor packetConstructor;
    private static Constructor displayConstructor;

    private static Method getPackets;
    private static Method getScore;
    private static Method setScore;

    private static  Object      scoreboardServer;
    private static  Object      sidebarCriteria;
    private static  Class<?>    enumScoreboard;
    private static  Class<?>    enumDisplaySlot;
    private static  Constructor chatCtor;
    private static  Method      chatMethod;
    protected final String      plugin;
    private final   String      title;
    private final   Scoreboard  scoreboard;
    private final   Object      objective;
    private         Player      player;

    /**
     * Constructs a new scoreboard manager with a desired type
     *
     * @param title title for the scoreboard
     * @param type  type of the scoreboard
     * @deprecated use Board(String, String) instead
     */
    @Deprecated
    public Board(String title, String type, String plugin) {
        this(title, plugin);
    }

    /**
     * @param title  title of the scoreboard
     * @param plugin plugin owning the scoreboard
     */
    public Board(String title, String plugin) {
        this.plugin = plugin;
        this.title = title;

        Scoreboard scoreboard = null;
        Object     objective  = null;
        if (packetConstructor != null) {
            try {
                if (ReflectionManager.MINOR_VERSION >= 17) {
                    objective = objConstructor.newInstance(scoreboardServer, title, sidebarCriteria,
                            ReflectionManager.MINOR_VERSION >= 19 || (chatCtor == null && chatMethod != null)
                                    ? Reflex.invokeMethod(chatMethod, null, title)
                                    : Reflex.invokeConstructor(chatCtor, title),
                            Reflex.getEnum(enumScoreboard, "HEARTS"));
                } else
                    objective = objConstructor.newInstance(scoreboardServer, title, sidebarCriteria);
            } catch (Exception ex) {
                NexEngine.get()
                        .getLogger()
                        .warning("Failed to create objective for scoreboard - resorting to slow method");
            }
        }
        if (objective == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = scoreboard.registerNewObjective(title, "dummy");
            ((Objective) objective).setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        this.scoreboard = scoreboard;
        this.objective = objective;
    }

    /**
     * Initializes reflection usage for boards
     *
     * @param scoreboard BoardManager's scoreboard
     */
    public static void init(Scoreboard scoreboard) {
        if (getScore != null)
            return;

        try {
            String pkg = Reflex.getNMSPackage() + ".";
            Class<?> criteria = ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria")
                    : Class.forName(pkg + "IScoreboardCriteria");
            Class<?> objective = ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.world.scores.ScoreboardObjective")
                    : Class.forName(pkg + "ScoreboardObjective");
            Class<?> baseComp = ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.network.chat.IChatBaseComponent")
                    : Class.forName(pkg + "IChatBaseComponent");
            if (ReflectionManager.MINOR_VERSION >= 19)
                chatMethod = Reflex.getMethod(baseComp, "b", String.class);
            else {
                Class<?> chatComp;
                if (ReflectionManager.MINOR_VERSION >= 17)
                    chatComp = Class.forName("net.minecraft.network.chat.ChatComponentText");
                else
                    chatComp = Class.forName(pkg + "ChatComponentText");
                chatCtor = Reflex.getConstructor(chatComp, String.class);
            }

            enumScoreboard = ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria")
                    : Class.forName(pkg + "IScoreboardCriteria");
            Class<?> tempEnum = enumScoreboard;
            enumScoreboard = Arrays.stream(enumScoreboard.getDeclaredClasses())
                    .filter(clazz -> clazz.getSimpleName().equals("EnumScoreboardHealthDisplay")).findFirst()
                    .orElseThrow(() -> new ClassNotFoundException(
                            "Could not find class '" + tempEnum.getPackage().getName()
                                    + "IScoreboardCriteria.EnumScoreboardHealthDisplay'"));

            getScore = (ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.world.scores.Scoreboard")
                    : Class.forName(pkg + "Scoreboard"))
                    .getDeclaredMethod(ReflectionManager.MINOR_VERSION >= 18
                            ? "c"
                            : "getPlayerScoreForObjective", String.class, objective);
            setScore = (ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.world.scores.ScoreboardScore")
                    : Class.forName(pkg + "ScoreboardScore"))
                    .getDeclaredMethod(ReflectionManager.MINOR_VERSION >= 18
                            ? "b"
                            : "setScore", int.class);
            getPackets = (ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.server.ScoreboardServer")
                    : Class.forName(pkg + "ScoreboardServer"))
                    .getDeclaredMethod(ReflectionManager.MINOR_VERSION >= 18
                            ? "d"
                            : "getScoreboardScorePacketsForObjective", objective);
            sidebarCriteria = criteria.getDeclaredField(ReflectionManager.MINOR_VERSION >= 17
                    ? "b" : "TRIGGER").get(null);

            objConstructor = VersionManager.isVersionAtLeast(VersionManager.V1_13)
                    ? objective.getConstructor(
                    Class.forName(ReflectionManager.MINOR_VERSION >= 17
                            ? "net.minecraft.world.scores.Scoreboard"
                            : pkg + "Scoreboard"
                    ), String.class, criteria, baseComp, enumScoreboard)
                    : objective.getConstructor(Class.forName(pkg + "Scoreboard"), String.class, criteria);
            scoreboardServer = scoreboard.getClass().getDeclaredMethod("getHandle").invoke(scoreboard);
            displayConstructor = (ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective")
                    : Class.forName(pkg + "PacketPlayOutScoreboardDisplayObjective"))
                    .getConstructor(int.class, objective);
            packetConstructor = (ReflectionManager.MINOR_VERSION >= 17
                    ? Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective")
                    : Class.forName(pkg + "PacketPlayOutScoreboardObjective"))
                    .getConstructor(objective, int.class);
        } catch (Exception ex) {
            NexEngine.get().getLogger().info("Failed to set up reflection for scoreboards - restoring to slow method");
            NexEngine.get().getLogger().info("Please send this stacktrace to a developer.");
            ex.printStackTrace();
        }
    }

    /**
     * Sets the player for the board
     *
     * @param player owning player
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Gets the name of the scoreboard
     *
     * @return scoreboard name
     */
    public String getName() {
        return title;
    }

    /**
     * Sets a score to the scoreboard
     *
     * @param label label to use
     * @param score score to show
     */
    protected void set(String label, int score) {
        if (scoreboard == null) {
            try {
                setScore.invoke(getScore.invoke(scoreboardServer, label, objective), score);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to set a score", ex);
            }
        } else {
            ((Objective) objective).getScoreboard().getEntries()
                    .stream().filter(str -> ((Objective) objective).getScore(str).getScore() == score)
                    .forEach(((Objective) objective).getScoreboard()::resetScores);
            ((Objective) objective).getScore(label).setScore(score);
        }
    }

    /**
     * Shows the board to it's player
     */
    @SuppressWarnings("unchecked")
    public boolean showPlayer() {
        if (player == null)
            return false;

        if (scoreboard == null) {
            try {
                clearDisplay();
                List<Object> packets = (List) getPackets.invoke(scoreboardServer, objective);
                packets.add(1, displayConstructor.newInstance(1, objective));
                ReflectionManager.getReflectionUtil().sendPackets(player, packets);
                return true;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create packets", ex);
            }
        } else {
            player.setScoreboard(scoreboard);
            return true;
        }
    }

    /**
     * Sets the health objective for the scoreboard
     * - Recommended not to use this method -
     * - Use PlayerBoard or BoardManager instead -
     *
     * @param label scoreboard label
     * @deprecated use setTextBelowNames in BoardManager instead
     */
    @Deprecated
    public void setHealthLabel(String label) {
        BoardManager.setTextBelowNames(label);
    }

    /**
     * Clears the side board display
     */
    public void clearDisplay() {
        if (player == null)
            return;

        if (scoreboard == null) {
            try {
                ReflectionManager.getReflectionUtil().sendPacket(player, packetConstructor.newInstance(objective, 1));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to send clear packet", ex);
            }
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * Hashes by name
     *
     * @return name hash
     */
    @Override
    public int hashCode() {
        return title.hashCode();
    }

    /**
     * Equates by name
     *
     * @param other other board to equate to
     * @return true if titles are equal
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Board && title.equals(((Board) other).title);
    }
}

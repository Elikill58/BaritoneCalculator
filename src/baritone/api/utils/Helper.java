package baritone.api.utils;

import java.util.Arrays;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import baritone.api.BaritoneAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public interface Helper {

    /**
     * Instance of {@link Helper}. Used for static-context reference.
     */
    Helper HELPER = new Helper() {};

    static TextComponent getPrefix() {
        return new TextComponent("[NegBaritone]");
    }

    /**
     * Send a message to display as a toast popup
     *
     * @param title   The title to display in the popup
     * @param message The message to display in the popup
     */
    default void logToast(TextComponent title, TextComponent message) {
    	title.addExtra(message);
    	Bukkit.getOnlinePlayers().stream().map(Player::spigot).forEach((p) -> p.sendMessage(title));
    }

    /**
     * Send a message to chat only if chatDebug is on
     *
     * @param message The message to display in chat
     */
    default void logDebug(String message) {
    	BaritoneAPI.debug(message);
    }

    /**
     * Send components to chat with the [Baritone] prefix
     *
     * @param logAsToast Whether to log as a toast notification
     * @param components The components to send
     */
    default void logDirect(boolean logAsToast, TextComponent... components) {
    	TextComponent component = new TextComponent("");
        if (!logAsToast) {
            // If we are not logging as a Toast
            // Append the prefix to the base component line
            component.addExtra(getPrefix());
            component.addExtra(new TextComponent(" "));
        }
        Arrays.asList(components).forEach(component::addExtra);
        if (logAsToast) {
            logToast(getPrefix(), component);
        } else {
        	Bukkit.getOnlinePlayers().stream().map(Player::spigot).forEach((p) -> p.sendMessage(component));
        }
    }

    /**
     * Send a message to chat regardless of chatDebug (should only be used for critically important messages, or as a
     * direct response to a chat command)
     *
     * @param message    The message to display in chat
     * @param color      The color to print that message in
     * @param logAsToast Whether to log as a toast notification
     */
    default void logDirect(String message, ChatColor color, boolean logAsToast) {
        Stream.of(message.split("\n")).forEach(line -> {
        	TextComponent component = new TextComponent(line.replace("\t", "    "));
            component.setColor(color);
            logDirect(logAsToast, component);
        });
    }

    /**
     * Send a message to chat regardless of chatDebug (should only be used for critically important messages, or as a
     * direct response to a chat command)
     *
     * @param message    The message to display in chat
     * @param logAsToast Whether to log as a toast notification
     */
    default void logDirect(String message, boolean logAsToast) {
        logDirect(message, ChatColor.GRAY, logAsToast);
    }

    /**
     * Send a message to chat regardless of chatDebug (should only be used for critically important messages, or as a
     * direct response to a chat command)
     *
     * @param message The message to display in chat
     */
    default void logDirect(String message) {
        logDirect(message, BaritoneAPI.getSettings().logAsToast.value);
    }
}

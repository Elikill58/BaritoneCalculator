package eli.com.elikill58.plugin;

import java.util.ArrayList;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import eli.baritone.api.events.TickEvent;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.pathing.calc.IPath;
import eli.com.elikill58.cmd.TestCommand;

public class EliPlugin extends JavaPlugin implements Listener {
	
	public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "BaritoneCheck" + ChatColor.GRAY + "-" + ChatColor.GREEN + "Negativity" + ChatColor.GRAY + "] ";
	public static boolean debug = false;
	
	private static EliPlugin instance;
	public static EliPlugin getInstance() {
		return instance;
	}
	private int amount = 0, amountNot = 0;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getCommand("test").setExecutor(new TestCommand());
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		TickEvent tick = new TickEvent(p);
		Bukkit.getServer().getPluginManager().callEvent(tick);
		if(tick.getExpectedPosition() == null || tick.getPathExecutor() == null) {
			return;
		}
		Location from = e.getFrom(), to = e.getTo();
		if(from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getZ()) // detect if change block
			return;
		Optional<IPath> optPath = tick.getInProgress() == null ? Optional.empty() : tick.getInProgress().bestPathSoFar();
		if(!optPath.isPresent())
			optPath = Optional.of(tick.getPathExecutor().getPath());
		if(optPath.isPresent()) {
			IPath path = optPath.get();
			BlockPos toPos = new BlockPos(to.getBlockX(), to.getBlockY(), to.getBlockZ());
			long amount = path.positions().stream().filter((pos) -> pos.equals(toPos) && !pos.equals(path.getSrc())).count();
			if(amount > 0) {
				//p.sendMessage(PREFIX + ChatColor.DARK_RED + "You" + ChatColor.RED + " seems to use " + ChatColor.YELLOW + "Baritone" + ChatColor.RED + ".");
				addFlag();
			} else {
				removeFlag();
			}
		} else {
			Location must = tick.getExpectedPosition().toBukkitLocation(p.getWorld());
			//double distance = must.distance(to);
			if(must.getBlockX() == to.getBlockX() && must.getBlockY() == to.getBlockY() && must.getBlockZ() == to.getBlockZ() && !tick.isFar()) {
				//p.sendMessage(PREFIX + ChatColor.DARK_RED + "You" + ChatColor.RED + " seems to use " + ChatColor.YELLOW + "Baritone" + ChatColor.RED + "." + ChatColor.AQUA + " Distance: " + distance);
				addFlag();
			} else {
				removeFlag();
			}
		}
	}
	
	public void addFlag() {
		amount++;
		if (amount > 10) {
			Bukkit.broadcastMessage(PREFIX + ChatColor.DARK_RED + "You" + ChatColor.RED + " seems to use " + ChatColor.YELLOW + "Baritone" + ChatColor.RED + ".");
		}
	}
	
	public void removeFlag() {
		amountNot++;
		if(amountNot >= 3) {
			amount = 0;
			amountNot = 0;
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		ArrayList<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
		online.remove(e.getPlayer());
		if(!online.isEmpty())
			e.getPlayer().sendMessage(PREFIX + ChatColor.DARK_RED + "Be more than one create issue. Please, be alone while testing baritone check.");
	}

	public void debug(String message) {
		if(debug)
			getLogger().info("[Debug-Baritone] " + message);
	}
}

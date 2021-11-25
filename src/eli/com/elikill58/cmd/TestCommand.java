package eli.com.elikill58.cmd;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import eli.baritone.api.BaritoneAPI;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.pathing.calc.IPath;
import eli.baritone.api.pathing.goals.GoalBlock;
import eli.baritone.api.pathing.movement.IMovement;
import eli.baritone.pathing.calc.PathNode;
import eli.baritone.process.CustomGoalProcess;
import eli.com.elikill58.events.TickEvent;
import eli.com.elikill58.plugin.EliPlugin;

public class TestCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String label, String[] args) {
		if(!(sender instanceof Player))
			return false;
		Player p = (Player) sender;
		if(args.length == 0) {
			/*if(EliPlugin.player != null) {
				EliPlugin.player = null;
				p.sendMessage(ChatColor.GREEN + "Stopped");
			} else {*/
				p.sendMessage(ChatColor.RED + "Non démarré. Aide:");
				p.sendMessage(ChatColor.GREEN + "/test debug: " + ChatColor.GOLD + "Commencer/Finir le débuggage.");
				p.sendMessage(ChatColor.GREEN + "/test dist: " + ChatColor.GOLD + "Nombre de block restant.");
				p.sendMessage(ChatColor.GREEN + "/test <x> <y> <z>: " + ChatColor.GOLD + "Démarrer un test.");
			//}
		} else if(args[0].equalsIgnoreCase("debug")) {
			EliPlugin.debug = !EliPlugin.debug;
			if(EliPlugin.debug)
				p.sendMessage(ChatColor.GREEN + "Debug mode enabled.");
			else
				p.sendMessage(ChatColor.GREEN + "Debug mode disabled.");
		} else if(args[0].equalsIgnoreCase("dist")) {
			showDist(p);
		} else if(args.length < 3) {
			p.sendMessage(ChatColor.GREEN + "/test <x> <y> <z> : Begin Baritone check to the given coords.");
		} else {
			BaritoneAPI.getSettings().allowSprint.value = true;
			BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;
			CustomGoalProcess process = BaritoneAPI.getProvider().getBaritone(p).getCustomGoalProcess();
			//process.setGoalAndPath(new GoalXZ(0, 0));
			process.setGoalAndPath(new GoalBlock(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2])));
			Bukkit.getServer().getPluginManager().callEvent(new TickEvent(p));
			p.sendMessage(ChatColor.GREEN + "Begin baritone check direction");
			showDist(p);
		}
		return false;
	}


	private void showDist(Player p) {
		Optional<IPath> optPath = BaritoneAPI.getProvider().getBaritone(p).getPathingBehavior().getPath();
		if(optPath.isPresent()) {
			IPath path = optPath.get();
			p.sendMessage(ChatColor.RED + "Values :" + ChatColor.YELLOW + " NumNodes: " + path.getNumNodesConsidered());
			p.sendMessage(ChatColor.YELLOW + "Movements : " + path.movements().size());
			p.sendMessage(ChatColor.YELLOW + "Nodes : " + path.getNodes().size());
			EliPlugin pl = EliPlugin.getInstance();
			pl.saveDefaultConfig();
			FileConfiguration config = pl.getConfig();
			int i = 0;
			World w = p.getWorld();
			config.set("blocks", null);
			DustOptions dustOptions = new DustOptions(Color.fromRGB(0, 127, 255), 1.0F);
			int distance = 0;
			Location old = null;
			for(BlockPos b : path.positions()) {
				Location loc = b.toBukkitLocation(w);
				config.set("blocks." + (i++), String.valueOf(b.getX() + " " + b.getY() + " " + b.getZ()));
				w.spawnParticle(Particle.REDSTONE, loc, 50, dustOptions);
				if(old != null) {
					distance += old.distance(loc);
				}
				old = loc;
			}
			distance += old.distance(((GoalBlock) path.getGoal()).getGoalPos().toBukkitLocation(w));
			i = 0;
			config.set("nodes", null);
			for(PathNode node : path.getNodes()) {
				config.set("nodes." + (i++), node.toString());
			}
			i = 0;
			config.set("mov", null);
			for(IMovement mov : path.movements()) {
				config.set("mov." + (i++), mov.getCost() + ":" + mov.toString());
			}
			p.sendMessage(ChatColor.YELLOW + "FullDistance : " + distance);
			pl.saveConfig();
		} else
			p.sendMessage(ChatColor.RED + "Aucun path en cours.");
	}
}

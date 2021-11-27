package com.elikill58.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.elikill58.cmd.BaritoneCalculatorCommand;

import baritone.api.BaritoneAPI;
import baritone.api.events.PathCalculatedEvent;
import baritone.api.nms.block.BlockPos;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.IMovement;
import baritone.pathing.calc.PathNode;

public class EliPlugin extends JavaPlugin implements Listener {
	
	private static EliPlugin instance;
	public static EliPlugin getInstance() {
		return instance;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		
		saveDefaultConfig();
		
		getCommand("bc").setExecutor(new BaritoneCalculatorCommand());
		
		getServer().getPluginManager().registerEvents(this, this);
		
		BaritoneAPI.setPlugin(this);
	}
	
	@EventHandler
	public void onPathCal(PathCalculatedEvent e) {
		IPath path = e.getPath();
		Player p = e.getContext().getPlayer();
		p.sendMessage(ChatColor.RED + "Informations :" + ChatColor.YELLOW);
		p.sendMessage(ChatColor.YELLOW  + "NumNodes: " + path.getNumNodesConsidered() + ", movements : " + path.movements().size() + ", nodes : " + path.getNodes().size());
		EliPlugin pl = EliPlugin.getInstance();
		pl.saveDefaultConfig();
		FileConfiguration config = pl.getConfig();
		int i = 0;
		World w = p.getWorld();
		config.set("blocks", null);
		DustOptions dustOptions = new DustOptions(Color.fromRGB(0, 127, 255), 1.0F);
		for(BlockPos b : path.positions()) {
			Location loc = b.toBukkitLocation(w);
			config.set("blocks." + (i++), String.valueOf(b.getX() + " " + b.getY() + " " + b.getZ()));
			w.spawnParticle(Particle.REDSTONE, loc, 200, dustOptions);
		}
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
		saveConfig();
		p.sendMessage(ChatColor.GRAY + "All informations saved in the config file.");
		BaritoneAPI.getProvider().removeBaritone(p);
	}
}

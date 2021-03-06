package ca.xshade.bukkit.towny.war;

import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.TownyTimerTask;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.WorldCoord;

public class WarTimerTask extends TownyTimerTask {
	War warEvent;
	
	public WarTimerTask(War warEvent) {
		super(warEvent.getTownyUniverse());
		this.warEvent = warEvent;
	}

	@Override
	public void run() {
		//TODO: check if war has ended and end gracefully
		if (!warEvent.isWarTime()) {
			warEvent.end();
			universe.clearWarEvent();
			universe.getPlugin().updateCache();
			universe.getPlugin().sendDebugMsg("War ended.");
			return;
		}
		
		int numPlayers = 0;
		for (Player player : universe.getOnlinePlayers()) {
			numPlayers += 1;
			plugin.sendDebugMsg("[War] "+player.getName()+": ");
			try {
				Resident resident = universe.getResident(player.getName());
				if (resident.hasNation()) {
					Nation nation = resident.getTown().getNation();
					plugin.sendDebugMsg("[War]   hasNation");
					if (nation.isNeutral())
						continue;
					plugin.sendDebugMsg("[War]   notNeutral");
					if (!warEvent.isWarringNation(nation))
						continue;
					plugin.sendDebugMsg("[War]   warringNation");
					//TODO: Cache player coord & townblock
					
					WorldCoord worldCoord = new WorldCoord(universe.getWorld(player.getWorld().getName()), Coord.parseCoord(player));
					if (!warEvent.isWarZone(worldCoord))
						continue;
					plugin.sendDebugMsg("[War]   warZone");
					if (player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
						continue;
					plugin.sendDebugMsg("[War]   aboveMinHeight");
					TownBlock townBlock = worldCoord.getTownBlock(); //universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
					if (nation == townBlock.getTown().getNation() || townBlock.getTown().getNation().hasAlly(nation))
						continue;
					plugin.sendDebugMsg("[War]   notAlly");
					//Enemy nation
					warEvent.damage(resident.getTown(), townBlock);
					System.out.println("[War]   damaged");
				}
			} catch(NotRegisteredException e) {
				continue;
			}
		}
		
		plugin.sendDebugMsg("[War] # Players: " + numPlayers);
	}
}

package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nijikokun.bukkit.iConomy.iConomy;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;
import com.shade.util.MemMgmt;

/**
 * Handle events for all Player related events
 * @author Shade
 */
public class TownyPlayerListener extends PlayerListener {
    private final Towny plugin;

    public TownyPlayerListener(Towny instance) {
        plugin = instance;
    }
    
    public void onPlayerChat(PlayerChatEvent event) {
    	Player player = event.getPlayer();
    	
    	if (plugin.getTownyUniverse().getSettings().isUsingChatPrefix()) {
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				String colour, formatedName = "";
				if (resident.isKing())
					colour = Colors.Gold;
				else if (resident.isMayor())
					colour = Colors.LightBlue;
				else
					colour = Colors.White;
				formatedName = colour + plugin.getTownyUniverse().getFormatter().getFormattedName(resident) + Colors.White;
		    	player.setDisplayName(formatedName);
			} catch (NotRegisteredException e) {
			}
    	}
    }
    
    
    @Override
    public void onPlayerJoin(PlayerEvent event) {
    	Player player = event.getPlayer();
    	try {
    		plugin.getTownyUniverse().onLogin(player);
    	} catch (TownyException x) {
    		plugin.sendErrorMsg(player, x.getError());
    	}
    }

    @Override
    public void onPlayerQuit(PlayerEvent event) {
    	plugin.getTownyUniverse().onLogout(event.getPlayer());
    }
    
    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
    	Player player = event.getPlayer();
    	Location from = event.getFrom();
    	Location to = event.getTo();
    	
    	Coord fromCoord = Coord.parseCoord(from);
    	Coord toCoord = Coord.parseCoord(to);
    	if (!(fromCoord.equals(toCoord)))
    		onPlayerMoveChunk(player, fromCoord, toCoord, from, to);
    }
    
    public void onPlayerTeleport(PlayerMoveEvent event) {
    	onPlayerMove(event);
    }
    
    public void onPlayerMoveChunk(Player player, Coord from, Coord to, Location fromLoc, Location toLoc) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
    	
    	//TODO: Cache build/destroy permissions
    	//TODO: Player mode
    	// map: send the map
    	// claim: attempt to claim area
    	// claim remove: remove area from town
    	
    	//Check if player has entered a new town/wilderness
    	if (settings.getShowTownNotifications()) {
    		boolean fromWild = false, toWild = false;
    		TownBlock fromTownBlock, toTownBlock;
    		Town fromTown = null, toTown = null;
			try {
				fromTownBlock = universe.getWorld(fromLoc.getWorld().getName()).getTownBlock(from);
				try {
					fromTown = fromTownBlock.getTown();
				} catch(NotRegisteredException e) {}
			} catch (NotRegisteredException e) {
				fromWild = true;
			}
			
			try {
				toTownBlock = universe.getWorld(toLoc.getWorld().getName()).getTownBlock(to);
				try {
					toTown = toTownBlock.getTown();
				} catch(NotRegisteredException e) {}
			} catch (NotRegisteredException e) {
				toWild = true;
			}
			if (fromWild ^ toWild || (!fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown)) {
				if (toWild) {
					player.sendMessage(Colors.Gold + " ~ " + Colors.Green + settings.getUnclaimedZoneName());
				} else {
					player.sendMessage(Colors.Gold + " ~ "+ universe.getFormatter().getFormattedName(toTown));
				}
			}
			if (settings.getDebug())
				System.out.println("[Towny] Debug: onPlayerMoveChunk: "+fromWild + " ^ " + toWild + " " + fromTown + " = " + toTown);
    	}
    }

    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        if (event.isCancelled())
        	return;
        
        String[] split = event.getMessage().split(" ");
        Player player = event.getPlayer();
        
        String[] newSplit = new String[split.length-1];
    	System.arraycopy(split, 1, newSplit, 0, split.length-1);
    	
        
        if (split[0].equalsIgnoreCase("/resident") || split[0].equalsIgnoreCase("/player")) {
        	parseResidentCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/town")) {
        	parseTownCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/nation")) {
        	parseNationCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/towny")) {
        	parseTownyCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/townyadmin")) {
        	parseTownyAdminCommand(player, newSplit);
        	event.setCancelled(true);
        }
    }
    
    public void parseResidentCommand(Player player, String[] split) {
    	/*
    	 * /resident
    	 * /resident ?
    	 * /resident [resident]
    	 * /resident list
    	 *TODO: /resident delete [resident] *Admin
    	 */
    	
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showResidentHelp(player);
	        } else if (split[0].equalsIgnoreCase("list")) {
	    		listResidents(player);
	        } else {
	        	try {
		    		Resident resident = plugin.getTownyUniverse().getResident(split[0]);
		    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
	    		} catch (NotRegisteredException x) {
	    			plugin.sendErrorMsg(player, split[0] + " is not registered");
	    		}
	        }
    	}
    }
 
    /**
     * Send a list of all resident commands to player
     * Command: /resident ?
     * @param player
     */
    
    public void showResidentHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("/resident"));
    	player.sendMessage(ChatTools.formatCommand("", "/resident", "", "Your status"));
    	player.sendMessage(ChatTools.formatCommand("", "/resident", "[resident]", "Target player's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/resident", "list", "List all active players"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/resident", "delete [resident]", ""));
    }
    
    /**
     * Send a list of all active residents in the universe to player
     * Command: /resident list
     * @param player
     */
    
    public void listResidents(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Residents"));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isKing())
					colour = Colors.Gold;
			else if (resident.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    public void parseTownCommand(Player player, String[] split) {
    	/*
    	 * /town
		 *TODO: /town ?
		 * /town list
		 *TODO: /town leave
		 * /town here
		 * /town spawn
		 * /town claim ...
		 * /town new [town] [mayor] *Admin
		 *TODO: /town givebonus [town] [bonus] *Admin
		 *TODO: /town delete [town] *Admin
		 * /town add [resident] .. [resident] *Mayor
		 *TODO: /town add+ [resident] *Mayor (For inviting offline residents)
		 * /town kick [resident] .. [resident] *Mayor
		 *TODO: /town kick+ [resident] *Mayor (For kicking offline residents)
		 *TODO: /town wall
		 * /town set [] ... [] *Mayor *Admin 
    	 */
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		Town town = resident.getTown();
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You don't belong to a town.");
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
	    		showTownHelp(player);
	        } else if (split[0].equalsIgnoreCase("here")) {
	        	showTownStatusHere(player);
	        }  else if (split[0].equalsIgnoreCase("list")) {
	    		listTowns(player);
	        } else if (split[0].equalsIgnoreCase("new")) {
	        	if (split.length == 1) {
	        		plugin.sendErrorMsg(player, "Specify town name");
	        	} else if (split.length == 2) {
	        		newTown(player, split[1], player.getName());
	        	} else {
	        		//TODO: Check if player is an admin
	        		newTown(player, split[1], split[2]);
	        	}
	        } else if (split[0].equalsIgnoreCase("add")) {
	        	String[] newSplit = new String[split.length-1];
	        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
	        	townAdd(player, newSplit);
	        } else if (split[0].equalsIgnoreCase("kick")) {
	        	String[] newSplit = new String[split.length-1];
	        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
	        	townKick(player, newSplit);
	        } else if (split[0].equalsIgnoreCase("spawn")) {
	        	townSpawn(player, false);
	        } else if (split[0].equalsIgnoreCase("claim")) {
	        	String[] newSplit = new String[split.length-1];
	        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
	        	parseTownClaimCommand(player, newSplit);
	        } else if (split[0].equalsIgnoreCase("set")) {
	        	String[] newSplit = new String[split.length-1];
	        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
	        	townSet(player, newSplit);
	        } else {
	        	try {
		    		Town town = plugin.getTownyUniverse().getTown(split[0]);
		    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
	    		} catch (NotRegisteredException x) {
	    			plugin.sendErrorMsg(player, split[0]+ " is not registered.");
	    		}
	        }
    	}
    }

	/**
     * Send a list of all town commands to player
     * Command: /town ?
     * @param player
     */
    
    public void showTownHelp(Player player) {
    	String newTownReq = plugin.getTownyUniverse().getSettings().isTownCreationAdminOnly() ? "Admin" : "";
    	
    	player.sendMessage(ChatTools.formatTitle("/town"));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "", "Your town's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "[town]", "Selected town's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "here", "Shortcut to the town's status of your location."));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "list", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "leave", ""));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "claim", "'/town claim ?' for help"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "unclaim", "'/town unclaim ?' for help"));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "spawn", "Teleport to town's spawn."));
    	player.sendMessage(ChatTools.formatCommand(newTownReq, "/town", "new [town] *[mayor]", "Create a new town."));
    	player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "add [resident] .. []", "Add online residents."));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "add+ [resident]", "Add resident")); 
    	player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "kick [resident] .. []", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "kick+ [resident]", "Kick resident"));
    	player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "set [] .. []", "'/town set' for help"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "assistant [+/-] [player]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall [type] [height]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall remove", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "givebonus [town] [bonus]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "delete [town]", ""));
    }
    
    /**
     * Send a the status of the town the player is physically at to him
     * @param player
     */
    
    public void showTownStatusHere(Player player) {
		try {
			TownyWorld world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			Coord coord = Coord.parseCoord(player);
	    	showTownStatusAtCoord(player, world, coord);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
    }
    
    /**
     * Send a the status of the town at the target coordinates to the player
     * @param player
     * @param world
     * @param coord
     * @throws TownyException 
     */
    public void showTownStatusAtCoord(Player player, TownyWorld world, Coord coord) throws TownyException {
    	if (!world.hasTownBlock(coord))
			throw new TownyException("This area ("+coord+") hasn't been claimed.");
		
		Town town = world.getTownBlock(coord).getTown();
		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
    }
    
    /**
     * Send a list of all towns in the universe to player
     * Command: /town list
     * @param player
     */
    
    public void listTowns(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Towns"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Town town : plugin.getTownyUniverse().getTowns())
			formatedList.add(Colors.LightBlue + town.getName() + Colors.Blue + " [" + town.getNumResidents() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    /**
     * Create a new town.
     * Command: /town new [town] *[mayor]
     * @param player
     */
    
    public void newTown(Player player, String name, String mayorName) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
		try {
    		Resident resident = universe.getResident(mayorName);
    		if (resident.hasTown())
    			throw new TownyException(resident.getName() + " already belongs to a town.");
    		
    		TownyWorld world = universe.getWorld(player.getWorld().getName());
    		Coord key = Coord.parseCoord(player);
    		if (world.hasTownBlock(key))
    			throw new TownyException("This area ("+key+") already belongs to someone.");
    		
    		if (settings.isUsingIConomy() && resident.pay(settings.getNewTownPrice()))
    			throw new TownyException("You can't afford to settle a new town here.");
    		
    		world.newTownBlock(key);
    		universe.newTown(name);
			Town town = universe.getTown(name);
			town.addResident(resident);
			town.setMayor(resident);
			TownBlock townBlock = world.getTownBlock(key);
			townBlock.setTown(town);
			town.setHomeBlock(townBlock);
			town.setSpawn(player.getLocation());
			world.addTown(town);
			
			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveWorld(world);
			universe.getDataSource().saveTownList();
			
			universe.sendGlobalMessage(settings.getNewTownMsg(player.getName(), town.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			//TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
    }
    
    /**
     * Confirm player is a mayor or assistant, then
     * get list of filter names with online players and invite them to town.
     * Command: /town add [resident] .. [resident]
     * @param player
     * @param names
     */
    
    public void townAdd(Player player, String[] names) {
    	Resident resident;
		Town town;
    	try {
    		resident = plugin.getTownyUniverse().getResident(player.getName());
    		town = resident.getTown();
    		if (!resident.isMayor()) {
    			if (!town.hasAssistant(resident)) {
    				throw new TownyException("You are not the mayor or an assistant.");
    			}
    		}
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		townAddResidents(player, town, getOnlineResidents(player, names));
    }
    
    
    //TODO: Move somewhere more useful
    public List<Resident> getOnlineResidents(Player player, String[] names) {
    	List<Resident> invited = new ArrayList<Resident>();
		for (String name : names) {
			List<Player> matches = plugin.getServer().matchPlayer(name);
			if (matches.size() > 1) {
				String line = "Multiple players selected";
				for (Player p : matches)
					line += ", " + p.getName();
				plugin.sendErrorMsg(player, line);
			} else if (matches.size() == 1) {
				try {
		    		Resident target = plugin.getTownyUniverse().getResident(matches.get(0).getName());
		    		invited.add(target);
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
			}	
		}
		return invited;
    }
    
    public void townAddResidents(Player player, Town town, List<Resident> invited) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited) {
			try {
				town.addResident(newMember);
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch(AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		}
		for (Resident newMember : remove)
			invited.remove(newMember);
		
		if (invited.size() > 0) {
			String msg = player.getName() + " invited ";
			for (Resident newMember : invited) {
				msg += newMember.getName()+", ";
				
			}
			msg += "to town.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else {
			plugin.sendErrorMsg(player, "None of those names were valid.");
		}
    }
    
    /**
     * Confirm player is a mayor or assistant, then
     * get list of filter names with online players and kick them from town.
     * Command: /town kick [resident] .. [resident]
     * @param player
     * @param names
     */
    
    public void townKick(Player player, String[] names) {
    	Resident resident;
		Town town;
    	try {
    		resident = plugin.getTownyUniverse().getResident(player.getName());
    		town = resident.getTown();
    		if (!resident.isMayor()) {
    			if (!town.hasAssistant(resident)) {
    				throw new TownyException("You are not the mayor or an assistant.");
    			}
    		}
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		townKickResidents(player, resident, town, getOnlineResidents(player, names));
    }
    
    public void townKickResidents(Player player, Resident resident, Town town, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking) {
			if (resident == member || member.isMayor() && town.hasAssistant(resident)) {
				remove.add(member);
			} else {
				try {
					town.removeResident(member);
					plugin.getTownyUniverse().getDataSource().saveResident(member);
				} catch (NotRegisteredException e) {
					remove.add(member);
				} catch (EmptyTownException e) {
					// You can't kick yourself and only the mayor can kick assistants
					// so there will always be at least one resident.
				}
			}
		}
		for (Resident member : remove)
			kicking.remove(member);
		
		if (kicking.size() > 0) {
			String msg = player.getName() + " kicked ";
			for (Resident member : kicking) {
				msg += member.getName()+", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage("You were kicked from town by "+player.getName());
			}
			msg += "from town.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else {
			plugin.sendErrorMsg(player, "Non of those names were valid.");
		}
    }
    
    public void parseTownClaimCommand(Player player, String[] split) {
    	if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
    		player.sendMessage(ChatTools.formatCommand("", "/town claim", "", "Claim this town block"));
    		//TODO: player.sendMessage(ChatTools.formatCommand("", "/town claim", "auto", "Automatically expand town area till max"));
    		player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect [radius]", "Attempt to claim around you."));
    		player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect auto", "Detemine the maximum radius"));
    	} else {
    		Resident resident;
    		Town town;
    		TownyWorld world;
        	try {
        		resident = plugin.getTownyUniverse().getResident(player.getName());
        		town = resident.getTown();
        		if (!resident.isMayor()) {
        			if (!town.hasAssistant(resident)) {
        				throw new TownyException("You are not the mayor or an assistant.");
        			}
        		}
        		world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
    		} catch (TownyException x) {
    			plugin.sendErrorMsg(player, x.getError());
    			return;
    		}
    		
    		
        	
    		if (split.length == 0) {
    			try {
    				int available = plugin.getTownyUniverse().getSettings().getMaxTownBlocks(town) - town.getTownBlocks().size();
    				
    				if (available - 1 < 0)
    					throw new TownyException("You've already claimed your maximum amount of town blocks.");
    				
    				townClaim(town, world, Coord.parseCoord(player), true);
    				
    				plugin.getTownyUniverse().getDataSource().saveTown(town);
    				plugin.getTownyUniverse().getDataSource().saveWorld(world);
    			} catch (TownyException x) {
    				plugin.sendErrorMsg(player, x.getError());
				}
    		} else {
    			if (split[0].equalsIgnoreCase("rect")) {
    				String[] newSplit = new String[split.length-1];
    		    	System.arraycopy(split, 1, newSplit, 0, split.length-1);
    				townClaimRect(player, newSplit, resident, town, world);
    				
    			} else if (split[0].equalsIgnoreCase("auto")) {
    				//TODO: Attempt to claim edge blocks recursively.
    			}
    		} 
    	}
    }
    
    public void townClaimRect(Player player, String[] split, Resident resident, Town town, TownyWorld world) {
    	int available = plugin.getTownyUniverse().getSettings().getMaxTownBlocks(town) - town.getTownBlocks().size();
    	
    	if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
    		player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim rect", "[radius]", "Claim around you."));
    		player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim rect", "auto", "Detemine the maximum radius"));
    	} else {
    		int r = 0;
    		Coord pos = Coord.parseCoord(player);
    		
    		if (split[0].equalsIgnoreCase("auto")) {
	        	// Automatically determine the maximum area to claim
	        	// by taking the radius and getting the diameter and
	        	// checking if the town has enough available town blocks
	        	r = 0;
	        	while (available - Math.pow((r+1)*2-1, 2) >= 0)
	        		r++;
	        } else {
	        	try {
	        		r = Integer.parseInt(split[0]);
	        	} catch (NumberFormatException e) {
	        		plugin.sendErrorMsg(player, "Invalid radius. Use an integer or 'auto'.");
	        		return;
	        	}
	        }
    		
    		// A radius of 1 requires expanding the selected position by 0,
    		// or the target radius - 1.
    		r--; 
    		
    		
        	List<Coord> coords = new ArrayList<Coord>();
        	for (int z = pos.getZ() - r; z <= pos.getZ() + r; z++) {
        		for (int x = pos.getX() - r; x <= pos.getX() + r; x++) {
        			coords.add(new Coord(x, z));
        		}
        	}
    		
        	player.sendMessage(String.format("Claiming %d town blocks within the radius of %d.", Math.pow((r+1)*2-1, 2), r));
    		try {
        		int n = townClaim(town, world, coords.toArray(new Coord[0]));
        		if (n > 0) {
					plugin.getTownyUniverse().getDataSource().saveTown(town);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
					
					player.sendMessage("Successfully claimed " + n + " town blocks.");
        		} else {
        			plugin.sendErrorMsg(player, "None of the selected townblocks were valid to claim.");
        		}
	        } catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
			}
    	}
    }
    
    public int townClaim(Town town, TownyWorld world, Coord[] coords) throws TownyException {
    	if (!isTownEdge(town, world, coords))
    		throw new TownyException("Selected area is not connected to town.");
    	
    	int n = 0;
    	for (Coord coord : coords) {
    		try {
	    		if (townClaim(town, world, coord, false))
	    			n++;
    		} catch (TownyException e) {
    			//Ignore complaints
    		}
    	}
    	return n;
    }
    
    public boolean townClaim(Town town, TownyWorld world, Coord coord, boolean checkEdge) throws TownyException {
    	try {
			TownBlock townBlock = world.getTownBlock(coord);
			try {
				throw new AlreadyRegisteredException("This area has already been claimed by: " + townBlock.getTown().getName());
			} catch (NotRegisteredException e) {
				throw new AlreadyRegisteredException("This area has already been claimed.");
			}
		} catch (NotRegisteredException e) {
			if (checkEdge && town.getTownBlocks().size() > 0) {
				if (!isTownEdge(town, world, coord))
					throw new TownyException("Selected area is not connected to town.");
			}
			TownBlock townBlock = world.newTownBlock(coord);
			townBlock.setTown(town);
			return true;
		}		
    }
    
    public boolean isTownEdge(Town town, TownyWorld world, Coord[] coords) {
    	//TODO: Better algorithm that doesn't duplicates checks.
    	
    	for (Coord coord : coords)
    		if (isTownEdge(town, world, coord))
    			return true;
    	return false;
    }
    
    public boolean isTownEdge(Town town, TownyWorld world, Coord coord) {
    	
    		System.out.print("[Towny] Debug: isTownEdge("+coord.toString()+") = ");
		int[][] offset = {{-1,0},{1,0},{0,-1},{0,1}};
		for (int i = 0; i < 4; i++) {
			try {
				TownBlock edgeTownBlock = world.getTownBlock(new Coord(coord.getX() + offset[i][0], coord.getZ() + offset[i][1]));
				if (edgeTownBlock.getTown() == town) {
					if (plugin.getTownyUniverse().getSettings().getDebug())
						System.out.println("true");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		}
		if (plugin.getTownyUniverse().getSettings().getDebug())
			System.out.println("false");
		return false;
	}
    
    /**
     * 
     * Command: /town set [] ... []
     * @param player
     * @param split
     */
    
	/*  
	 * board [message ... ]
	 * mayor [mayor] *[town]
	 * homeblock 
	 * spawn
	 * perm [resident/outsider] [build/destroy] [on/off]
	 * pvp [on/off]
	 * taxes [$]
	 */
    
    public void townSet(Player player, String[] split) {
    	if (split.length == 0) {
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor [mayor] *[town]", ""));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn", ""));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' for help"));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "pvp [on/off]", ""));
    		player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
    	} else {
    		Resident resident;
    		Town town;
        	try {
        		resident = plugin.getTownyUniverse().getResident(player.getName());
        		town = resident.getTown();
        		if (!resident.isMayor()) {
        			if (!town.hasAssistant(resident)) {
        				throw new TownyException("You are not the mayor or an assistant.");
        			}
        		}
    		} catch (TownyException x) {
    			plugin.sendErrorMsg(player, x.getError());
    			return;
    		}
    		
    		if (split[0].equalsIgnoreCase("board")) {
    			if (split.length < 2) {
    				plugin.sendErrorMsg(player, "Eg: /town set board Today's the day!");
    			} else {
	    			String line = split[1];
	    			for (int i = 2; i < split.length; i++)
	    				line += " " + split[i];
	    			town.setTownBoard(line);
	    			plugin.sendMsg(player, "Successfully changed the town board.");
	    			plugin.getTownyUniverse().sendTownBoard(player, town);
    			}
    		} else if (split[0].equalsIgnoreCase("mayor")) {
    			if (split.length < 2) {
    				plugin.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
    			} else {
    				try {
	    				Resident newMayor = plugin.getTownyUniverse().getResident(split[1]);
	    				town.setMayor(newMayor);
	    				plugin.getTownyUniverse().sendTownMessage(town, plugin.getTownyUniverse().getSettings().getNewMayorMsg(newMayor.getName()));
	    			} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
					}
    			}
    		} else if (split[0].equalsIgnoreCase("taxes")) {
    			if (split.length < 2) {
    				plugin.sendErrorMsg(player, "Eg: /town set taxes 7");
    			} else {
	    			try {
	    				town.setTaxes(Integer.parseInt(split[1]));
	    			} catch (NumberFormatException e) {
	    				plugin.sendErrorMsg(player, "Taxes must be an interger.");
	    			}
    			}
    		} else if (split[0].equalsIgnoreCase("name")) {
    			if (split.length < 2) {
    				plugin.sendErrorMsg(player, "Eg: /town set name BillyBob");
    			} else {
    				townRename(player, town, split[1]);
    			}
    		} else if (split[0].equalsIgnoreCase("homeblock")) {
    			Coord coord = Coord.parseCoord(player);
    			TownBlock townBlock;
				try {
					townBlock = plugin.getTownyUniverse().getWorld(player.getWorld().getName()).getTownBlock(coord);
					town.setHomeBlock(townBlock);
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
				}
    		} else if (split[0].equalsIgnoreCase("spawn")) {
				try {
					town.setSpawn(player.getLocation());
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
				}
    		} else if (split[0].equalsIgnoreCase("perm")) {
    			//TODO: switches
    			if (split.length < 2) {
    				player.sendMessage(ChatTools.formatCommand("", "/town set perm", "", ""));
    				player.sendMessage(ChatTools.formatCommand("", "", "[on/off]", "Toggle all permissions"));
    				player.sendMessage(ChatTools.formatCommand("", "", "[resident/outsider/allies] [on/off]", "Toggle specifics"));
    				player.sendMessage(ChatTools.formatCommand("", "", "[resident/outsider] [build/destroy] [on/off]", ""));
    			} else {
    				TownyPermission perm = town.getPermissions();
    				if (split.length == 2) {
    					try {
    						perm.setAll(parseOnOff(split[1]));
        				} catch (Exception e) {} 
    				} else if (split.length == 3) {
    					try {
    						boolean b = parseOnOff(split[2]);
    						if (split[1].equalsIgnoreCase("resident")) {
	    						perm.residentBuild = b;
	    						perm.residentDestroy = b;
    						} else if (split[1].equalsIgnoreCase("outsider")) {
	    						perm.outsiderBuild = b;
	    						perm.outsiderDestroy = b;
    						} else if (split[1].equalsIgnoreCase("allies")) {
	    						perm.allies = b;
    						}
        				} catch (Exception e) {} 
    				} else if (split.length == 4) {
    					try {
    						boolean b = parseOnOff(split[3]);
    						String s = "";
    						if ((split[1].equalsIgnoreCase("resident") || split[1].equalsIgnoreCase("outsider"))
    								&& (split[2].equalsIgnoreCase("build") || split[2].equalsIgnoreCase("destroy"))) {
    							s = split[1]+split[2];
    						}
    						perm.set(s, b);
    					} catch (Exception e) {} 
    				}
    			}
    		} else if (split[0].equalsIgnoreCase("pvp")) {
    			if (split.length < 2) {
    				plugin.sendErrorMsg(player, "Eg: /town set pvp [on/off]");
    			} else {
    				try {
    					town.setPVP(parseOnOff(split[1]));
    				} catch (Exception e) {}  
    			}
    		} else {
    			plugin.sendErrorMsg(player, "Invalid town property.");
    			return;
    		}
    		
    		plugin.getTownyUniverse().getDataSource().saveTown(town);
    	}
    }
    
    public boolean parseOnOff(String s) throws Exception {
    	if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception();
    }
    
    public void townRename(Player player, Town town, String newName) {
    	try {
			plugin.getTownyUniverse().renameTown(town, newName);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
    	
    }
    
    /**
     * Teleports the player to his town's spawn location. If town doesn't have a spawn
     * or player has no town, and teleport is forced, then player is sent to the world's
     * spawn location.
     * @param player
     * @param forceTeleport
     */
    
    public void townSpawn(Player player, boolean forceTeleport) {
    	try {
    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
    		Town town = resident.getTown();
    		player.teleportTo(town.getSpawn());
		} catch (TownyException x) {
			if (forceTeleport) {
				//TODO: When API supports: player.teleportTo(player.getWorld().getSpawnLocation());
			} else {
				plugin.sendErrorMsg(player, x.getError());
			}
		}
    }
    
    public void parseNationCommand(Player player, String[] split) {
    	/*
    	 * /nation
    	 * /nation list
    	 * /nation [nation]
    	 *TODO: /nation leave *Mayor
    	 * /nation new [nation] [capital] *Admin
    	 *TODO: /nation delete [nation] *Admin
    	 *TODO: /nation add [town] *King
    	 *TODO: /nation kick [town] *King
    	 */
    	
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		Town town = resident.getTown();
	    		Nation nation = town.getNation();
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You don't belong to a nation.");
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showNationHelp(player);
	        } else if (split[0].equalsIgnoreCase("list")) {
	    		listNations(player);
	        } else if (split[0].equalsIgnoreCase("new")) {
	        	//TODO: Make an overloaded function newNation(Player,String,Town) 
	        	if (split.length == 1) {
	        		plugin.sendErrorMsg(player, "Specify nation name.");
	        	} else if (split.length == 2) {
	        		try { //TODO: Make sure of the error catching
	        			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	        			newNation(player, split[1], resident.getTown().getName());
	        		} catch (TownyException x) {
	         			plugin.sendErrorMsg(player, x.getError());
	         		}
	        	} else {
	        		//TODO: Check if player is an admin
	        		newNation(player, split[1], split[2]);
	        	}
	        } else {
	        	try {
		    		Nation nation = plugin.getTownyUniverse().getNation(split[0]);
		    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
	    		} catch (NotRegisteredException x) {
	    			plugin.sendErrorMsg(player, split[0]+ " is not registered.");
	    		}
	        }
    	}
    }
    
    /**
     * Send a list of all nation commands to player
     * Command: /nation ?
     * @param player
     */
    
    public void showNationHelp(Player player) {
    	String newTownReq = plugin.getTownyUniverse().getSettings().isTownCreationAdminOnly() ? "Admin" : "";
    	
    	player.sendMessage(ChatTools.formatTitle("/nation"));
    	player.sendMessage(ChatTools.formatCommand("", "/nation", "", "Your nation's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/nation", "[nation]", "Target nation's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/nation", "list", "List all nations"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/nation", "delete [nation]", ""));
    	player.sendMessage(ChatTools.formatCommand(newTownReq, "/nation", "new [nation] *[capital]", "Create a new nation"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/nation", "ally [+/n/-] [nation]", "Set you alliance."));
    	
    }
    
    /**
     * Send a list of all nations in the universe to player
     * Command: /nation list
     * @param player
     */
    
    public void listNations(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Nations"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : plugin.getTownyUniverse().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    /**
     * Create a new nation.
     * Command: /nation new [nation] *[capital]
     * @param player
     */
    
    public void newNation(Player player, String name, String capitalName) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
		try {
    		Town town = universe.getTown(capitalName);
    		if (town.hasNation())
    			throw new TownyException("Target already belongs to a nation.");
    		
    		universe.newNation(name);
			Nation nation = universe.getNation(name);
			nation.addTown(town);
			nation.setCapital(town);
			
			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveNation(nation);
			universe.getDataSource().saveNationList();
			
			universe.sendGlobalMessage(settings.getNewNationMsg(player.getName(), nation.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			//TODO: delete town data that might have been done
		}
    }
    
    public void parseTownyCommand(Player player, String[] split) {
    	/*
    	 * /towny
    	 * /towny ?
    	 * /towny map
    	 * /towny version
    	 * /towny universe
    	 * 
    	 * /towny seed
    	 * /towny tree
    	 */
    	
    	if (split.length == 0) {
    		showHelp(player);
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showTownyHelp(player);
	        } else if (split[0].equalsIgnoreCase("map")) {
	        	showMap(player);
	        } else if (split[0].equalsIgnoreCase("version")) {
	        	showTownyVersion(player);
	        } else if (split[0].equalsIgnoreCase("universe")) {
	        	showUniverseStats(player);
	        } else if (split[0].equalsIgnoreCase("tree")) {
	        	showUniverseTree();
	        } else if (split[0].equalsIgnoreCase("seed")) {
	        	seedTowny();
	        }
    	}
    }
    
    /**
     * Show a general help and list other help commands to player
     * Command: /towny
     * @param player
     */
    
    public void showHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("General Towny Help"));
    	player.sendMessage("Try the following commands to learn more about towny.");
    	player.sendMessage(
    			ChatTools.formatCommand("", "/resident", "?", "") + ", " +
    			ChatTools.formatCommand("", "/town", "?", "") + ", " +
				ChatTools.formatCommand("", "/nation", "?", "") + ", " +
				ChatTools.formatCommand("", "/towny", "?", ""));
    	player.sendMessage(ChatTools.formatCommand("Admin", "/townyadmin", "?", ""));
    }
    
    /**
     * Send a list of all towny commands to player
     * Command: /towny ?
     * @param player
     */
    
    public void showTownyHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("/towny"));
    	player.sendMessage(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
    	player.sendMessage(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
    	player.sendMessage(ChatTools.formatCommand("", "/towny", "version", "Displays the version of Towny"));
    	player.sendMessage(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
    }
    
    /**
     * Send a map of the nearby townblocks status to player
     * Command: /towny map
     * @param player
     */
    
    public void showMap(Player player) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	boolean hasTown = false;
    	Resident resident;
    	int lineCount = 0;
    	
    	try {
    		resident = universe.getResident(player.getName());
    		if (resident.hasTown())
    			hasTown = true;
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		TownyWorld world;
		try {
			world = universe.getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "You are not in a registered world.");
			return;
		}
		Coord pos = Coord.parseCoord(player);
		
		player.sendMessage(ChatTools.formatTitle("Towny Map " + Colors.White + "("+pos.toString()+")"));
		
		String[][] townyMap = new String[31][7];
		int x, y = 0;
		for (int tby = pos.getZ()-15; tby <= pos.getZ()+15; tby++) {
			x = 0;
			for (int tbx = pos.getX()-3; tbx <= pos.getX()+3; tbx++) {
				try {
					TownBlock townblock = world.getTownBlock(tbx, tby);
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == 3 && y == 15) { //Center of map is player's location
						townyMap[y][x] = Colors.Gold;
					} else if (hasTown) {
						if (resident.getTown() == townblock.getTown()) { //Player's own town
							townyMap[y][x] = Colors.LightGreen;
						} else {
							if (resident.hasNation()) {
								if (resident.getTown().getNation().hasTown(townblock.getTown())) { //Allied towns
									townyMap[y][x] = Colors.Green;
								} else {
									if (townblock.getTown().hasNation()) {
										Nation nation = resident.getTown().getNation();
										if (nation.hasAlly(townblock.getTown().getNation())) { //Allied towns
											townyMap[y][x] = Colors.Green;
										} else if (nation.hasEnemy(townblock.getTown().getNation())) { //Enemy towns
											townyMap[y][x] = Colors.Red;
										} else {
											townyMap[y][x] = Colors.White;
										}
									} else {
										townyMap[y][x] = Colors.White;
									}
								}//
							} else {
								townyMap[y][x] = Colors.White;
							}
						}
					} else {
						townyMap[y][x] = Colors.White;
					}
					
					//Registered town block
					townyMap[y][x] += "+";
				} catch(TownyException e) {
					if (x == 3 && y == 15) {
						townyMap[y][x] = Colors.Gold;
					} else {
						townyMap[y][x] = Colors.Gray;
					}
					
					// Unregistered town block
					townyMap[y][x] += "-";
				}
				x++;
			}
			y++;
		}
		
		String[] compass = {
			Colors.Black + "  -----  ",
			Colors.Black + "  --" + Colors.White + "N" + Colors.Black + "--  ",
			Colors.Black + "  -" + Colors.White + "W+E" + Colors.Black + "-  ",
			Colors.Black + "  --" + Colors.White + "S" + Colors.Black + "--  "
		};
		
		String line;
		//Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < 7; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];
		
				
			for (int mx = 30; mx >= 0; mx--) {
				line += townyMap[mx][my];
			}
			player.sendMessage(line);
			lineCount++;
		}
		
		//Current town block data
		try {
			TownBlock townblock = world.getTownBlock(pos);
			player.sendMessage(
					"Town: " + (townblock.hasTown() ? townblock.getTown().getName() : "None") + " : " +
					"Owner: " + (townblock.hasResident() ? townblock.getResident().getName() : "None"));
		} catch(TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
    }
    
    /**
     * Send the version of towny to player
     * Command: /towny version
     * @param player
     */
    
    public void showTownyVersion(Player player) {
    	player.sendMessage("Towny version: " + plugin.getVersion());
    }
    
    /**
     * Send some stats about the towny universe to the player
     * Command: /towny universe
     * @param player
     */
    
    public void showUniverseStats(Player player) {
    	player.sendMessage("�0-�4###�0---�4###�0-");
		player.sendMessage("�4#�c###�4#�0-�4#�c###�4#�0   �6[�eTowny "+plugin.getVersion()+"�6]");
		player.sendMessage("�4#�c####�4#�c####�4#   �3By: �bChris H (Shade)");
		player.sendMessage("�0-�4#�c#######�4#�0-");
		player.sendMessage("�0--�4##�c###�4##�0-- �3Residents: �b" + Integer.toString(plugin.getTownyUniverse().getResidents().size()));
		player.sendMessage("�0----�4#�c#�4#�0---- �3Towns: �b" + Integer.toString(plugin.getTownyUniverse().getTowns().size()));
		player.sendMessage("�0-----�4#�0----- �3Nations: �b" + Integer.toString(plugin.getTownyUniverse().getNations().size()));
    }
    
    /**
     * Show the current universe in the console.
     * Command: /towny tree
     */
    
    public void showUniverseTree() {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	System.out.println("|-Universe");
    	for (TownyWorld world : universe.getWorlds()) {
    		System.out.println("|---World: "+world.getName());
    		for (TownBlock townBlock : world.getTownBlocks()) {
    			try {
    				System.out.println(
    						"|------TownBlock: "+townBlock.getX()+","+townBlock.getZ()+" "+
    						"Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : " +
    						"Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
    			} catch(TownyException e) {}
    		}
    		for (Resident resident : universe.getResidents()) {
    			try {
	    			System.out.println(
	    					"|---Resident: "+resident.getName()+" "+
	    					(resident.hasTown() ? resident.getTown().getName() : "")+
	    					(resident.hasNation() ? resident.getTown().getNation().getName() : ""));
	    		} catch(TownyException e) {}
	    		for (TownBlock townBlock : resident.getTownBlocks()) {
	    			try {
	    				System.out.println(
	    						"|------TownBlock: "+townBlock.getX()+","+townBlock.getZ()+" "+
	    						"Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : " +
	    						"Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
	    			} catch(TownyException e) {}
	    		}
    		}
    		for (Town town : universe.getTowns()) {
    			try {
	    			System.out.println(
	    					"|---Town: "+town.getName()+" "+
	    					(town.hasNation() ? town.getNation().getName() : ""));
	    		} catch(TownyException e) {}
	    		for (TownBlock townBlock : town.getTownBlocks()) {
	    			try {
	    				System.out.println(
	    						"|------TownBlock: "+townBlock.getX()+","+townBlock.getZ()+" "+
	    						"Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : " +
	    						"Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
	    			} catch(TownyException e) {}
	    		}
    		}
    	}
    }
    
    public void seedTowny() {
    	TownyUniverse townyUniverse = plugin.getTownyUniverse();
    	Random r = new Random();
	    for (int i = 0; i < 1000; i++) {
			
			
			try { townyUniverse.newNation(Integer.toString(r.nextInt()));} catch (TownyException e) {}
			try { townyUniverse.newTown(Integer.toString(r.nextInt()));} catch (TownyException e) {}
			try { townyUniverse.newResident(Integer.toString(r.nextInt()));} catch (TownyException e) {}
		}
    }
    
    public void parseTownyAdminCommand(Player player, String[] split) {
    	/*
    	 * /townyadmin
    	 * /townyadmin ?
    	 * /townyadmin set [] .. []
    	 * /townyadmin
    	 * /townyadmin
    	 * 
    	 * /towny tree
    	 */
    	
    	if (split.length == 0) {
    		showAdminPanel(player);
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showTownyAdminHelp(player);
	        } else if (split[0].equalsIgnoreCase("set")) {
	        	String[] newSplit = new String[split.length-1];
	        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
	        	adminSet(player, newSplit);
	        }
    	}
    }
    
    @SuppressWarnings("static-access")
	public void showAdminPanel(Player player) {
    	Runtime run = Runtime.getRuntime();
    	player.sendMessage(ChatTools.formatTitle("Towny Admin Panel"));
    	player.sendMessage(
    			Colors.Blue + "[Towny] " +
    			Colors.Green + " WarTime: " + Colors.LightGreen + plugin.getTownyUniverse().isWarTime());
    	try {
    		TownyIConomyObject.checkIConomy();
    		player.sendMessage(
    				Colors.Blue + "[iConomy] " +
    				Colors.Green + " Economy: " + Colors.LightGreen + getTotalEconomy() + " " + iConomy.currency +
    				Colors.Gray + " | " +
    				Colors.Green + "Bank Accounts: " + Colors.LightGreen + iConomy.db.accounts.returnMap().size());
    	} catch (Exception e) {}
    	player.sendMessage(
    			Colors.Blue + "[Server] " +
    			Colors.Green + " Memory: " + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) +
    			Colors.Gray + " | " +
    			Colors.Green + "Threads: " + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() +
    			Colors.Gray + " | " +
    			Colors.Green + "Time: " + Colors.LightGreen + plugin.getTownyUniverse().getFormatter().getTime());
    	player.sendMessage(Colors.Yellow + MemMgmt.getMemoryBar(50, run));
    	
    }
    
	
    public int getTotalEconomy() {
    	int total = 0;
    	try {
	    	@SuppressWarnings("static-access")
			Map<String,String> map = iConomy.db.accounts.returnMap();
	    	Set<String> keys = map.keySet();
	    	
	    	for (String key : keys)
	    		total += iConomy.db.get_balance(key);
    	} catch (Exception e) {}
    	return total;
    }
    
    /**
     * Send a list of all towny admin commands to player
     * Command: /townyadmin ?
     * @param player
     */
    
    public void showTownyAdminHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("/townyadmin"));
    	player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "", "Admin panel"));
    	player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' for help"));
    }
    
    public void adminSet(Player player, String[] split) {
    	
    }
}
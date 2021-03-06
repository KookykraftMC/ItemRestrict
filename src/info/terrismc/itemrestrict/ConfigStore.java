package info.terrismc.itemrestrict;

import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class ConfigStore {
	private ItemRestrict plugin;
	private FileConfiguration config;
	
	// Cache config values
	private List<String> worldList;
	private List<String> usageBans;
	private List<String> equipBans;
	private List<String> craftingBans;
	private List<String> ownershipBans;
	private List<String> worldBans;
	public ConfigStore( ItemRestrict plugin ) {
		this.plugin = plugin;
		
		// Force reload plugin
		reloadConfig();
	}
	
	public void reloadConfig() {
		// Config operations
		plugin.saveDefaultConfig();
		plugin.reloadConfig();
		
		// Config variables
		config = plugin.getConfig();
		worldList = config.getStringList( "Worlds" );
		usageBans = config.getStringList( "Bans.Usage" );
		equipBans = usageBans;
		craftingBans = usageBans;
		ownershipBans = usageBans;
		worldBans = config.getStringList( "Bans.World" );
	}
	
	public boolean hasPermission( CommandSender sender, String node, boolean allowConsole ) {
		if( sender instanceof Player ) {
			if( !sender.hasPermission( node ) ) {
				sender.sendMessage( "Insufficient permissions" );
				return false;
			}
		}
		else {
			if( !allowConsole )
				sender.sendMessage( "This is only a player command" );
				return false;
		}
			
		return true;
	}

	public boolean isEnabledWorld( World world ) {
		return worldList.contains( "All" ) || worldList.contains( world.getName() );
	}
	
	public boolean isBanned( Block block, ActionType actionType ) {
		boolean banned = isBanned( getConfigString( block ), actionType );
		if( !banned )
			banned = isBanned( getConfigStringParent( block ), actionType );
		return banned; 
	}
	
	public boolean isBanned( ItemStack item, ActionType actionType ) {
		boolean banned = isBanned( getConfigString( item ), actionType );
		if( !banned )
			banned = isBanned( getConfigStringParent( item ), actionType );
		return banned;
	}
	
	private boolean isBanned( String configString, ActionType actionType ) {
		// Select proper HashMap
		switch( actionType ) {
		case Usage:
			return usageBans.contains( configString );
		case Equip:
			return equipBans.contains( configString );
		case Crafting:
			return craftingBans.contains( configString );
		case Ownership:
			return ownershipBans.contains( configString );
		case World:
			return worldBans.contains( configString );
		default:
			// Should never reach here if all enum cases covered
			ItemRestrict.logger.warning( "Unknown ActionType detected: " + actionType.toString() );
			return false;
		}
	}
	
	public boolean isBannable( Player player, ItemStack item, ActionType actionType ) {
		// Check null
		if( item == null ) return false;

		// Player checks
		if( player != null ) {
			// Check world
			if( !isEnabledWorld( player.getWorld() ) ) return false;
			
			// Check exclude permission
			if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigString( item ) ) ) return false;
			
			// Check exclude parent permission
			if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigStringParent( item ) ) ) return false;
		}
		
		// Check ban list
		return isBanned( item, actionType );
	}
	
	public boolean isBannable( Player player, Block block, ActionType actionType ) {
		// Check null
		if( block == null ) return false;

		// Player checks
		if( player != null ) {
			// Check world
			if( !isEnabledWorld( player.getWorld() ) ) return false;
			
			// Check exclude permission
			if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigString( block ) ) ) return false;
			
			// Check exclude parent permission
			if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigStringParent( block ) ) ) return false;
		}
		
		// Check ban list
		return isBanned( block, actionType );
	}
	
	public String getLabel( Block block ) {
		String label = config.getString( "Messages.labels." + getConfigString( block ) );
		if( label != null )
			return label.replace( "&", "�" );
		label = config.getString( "Messages.labels." + getConfigStringParent( block ) );
		if( label != null )
			return label.replace( "&", "�" );
		return block.getType().name() + " (" + getConfigString( block ) + ")";
	}
	
	public String getLabel( ItemStack item ) {
		String label = config.getString( "Messages.labels." + getConfigString( item ) );
		if( label != null )
			return label.replace( "&", "�" );
		label = config.getString( "Messages.labels." + getConfigStringParent( item ) );
		if( label != null )
			return label.replace( "&", "�" );
		return item.getType().name() + " (" + getConfigString( item ) + ")";
	}
	
	public String getReason( Block block ) {
		String reason = config.getString( "Messages.reasons." + getConfigString( block ) );
		if( reason != null )
			return reason.replace( "&", "�" );
		reason = config.getString( "Messages.reasons." + getConfigStringParent( block ) );
		if( reason != null )
			return reason.replace( "&", "�" );
		return "Check the list for info: http://kkmc.info/21MSqpn";
	}
	
	public String getReason( ItemStack item ) {
		String reason = config.getString( "Messages.reasons." + getConfigString( item ) );
		if( reason != null )
			return reason.replace( "&", "�" );
		reason = config.getString( "Messages.reasons." + getConfigStringParent( item ) );
		if( reason != null )
			return reason.replace( "&", "�" );
		return "Check the list for info: http://kkmc.info/21MSqpn";
	}
	
	private String getActionTypeString( ActionType actionType ) {
		// Select proper string
		switch( actionType ) {
		case Usage:
			return "Usage";
		case Equip:
			return "Equip";
		case Crafting:
			return "Crafting";
		case Ownership:
			return "Ownership";
		case World:
			return "World";
		default:
			// Should never reach here if all enum cases covered
			ItemRestrict.logger.warning( "Unknown ActionType detected: " + actionType.toString() );
			return "";
		}
	}

	private boolean isConfigString( String configString ) {
		String[] notMagicNumbersAnymore = configString.split( "-" );
		
		// Check partition amount
		if( notMagicNumbersAnymore.length > 2 )
			return false;
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private String getConfigString( Block block ) {
		// Config version string of block id and data value 
		return "" + block.getType().toString() + "-" + block.getData();
	}
	
	private String getConfigStringParent( Block block ) {
		// Config version string of block id 
		return "" + block.getType().toString();
	}
	
	@SuppressWarnings("deprecation")
	private String getConfigString( ItemStack item ) {
		// Config version string of item id and data value
		MaterialData matData = item.getData();
		return "" + item.getType().toString() + "-" + matData.getData();
	}
	
	private String getConfigStringParent( ItemStack item ) {
		// Config version string of item id and data value
		return "" + item.getType().toString();
	}
	
	public double getScanFrequencyOnPlayerJoin() {
		return config.getDouble("Scanner.event.onPlayerJoin");
	}
	
	public double getScanFrequencyOnChunkLoad() {
		return config.getDouble("Scanner.event.onChunkLoad");
	}

	public int getBanListSize( ActionType actionType ) {
		// Select proper HashMap
		switch( actionType ) {
		case Usage:
			return usageBans.size();
		case Equip:
			return equipBans.size();
		case Crafting:
			return craftingBans.size();
		case Ownership:
			return ownershipBans.size();
		case World:
			return worldBans.size();
		}
		// Should never reach here if all enum cases covered
		ItemRestrict.logger.warning( "Unknown ActionType detected: " + actionType.toString() );
		return 0;
	}

	public void addBan( CommandSender sender, ActionType actionType, String configString) {
		// Check valid actionType
		if( actionType == null ) {
			sender.sendMessage( "Invalid ban type. Valid ban types: Usage, Equip, Crafting, Ownership, World" );
			return;
		}
		
		// Check valid config string
		if( !isConfigString( configString ) ) {
			sender.sendMessage( configString + "  is not a valid item" );
			return;
		}
		
		switch( actionType ) {
		case Usage:
			usageBans.add( configString );
			config.set( "Bans.Usage", usageBans );
			break;
		case Equip:
			equipBans.add( configString );
			config.set( "Bans.Equip", equipBans );
			break;
		case Crafting:
			craftingBans.add( configString );
			config.set( "Bans.Crafting", craftingBans );
			break;
		case Ownership:
			ownershipBans.add( configString );
			config.set( "Bans.Ownership", ownershipBans );
			break;
		case World:
			worldBans.add( configString );
			config.set( "Bans.World", worldBans );
			break;
		default:
			// Should never reach here if all enum cases covered
			ItemRestrict.logger.warning( "Unknown ActionType detected: " + actionType.toString() );
			return;
		}
		plugin.saveConfig();
		sender.sendMessage( "Item Banned" );
	}

	public void removeBan(CommandSender sender, ActionType actionType, String configString) {
			// Check valid actionType
			if( actionType == null ) {
				sender.sendMessage( "Invalid ban type. Valid ban types: Usage, Equip, Crafting, Ownership, World" );
				return;
			}
			
			// Check valid config string
			if( !isConfigString( configString ) ) {
				sender.sendMessage( configString + " is not a valid item" );
				return;
			}
			
			switch( actionType ) {
			case Usage:
				usageBans.remove( configString );
				config.set( "Bans.Usage", usageBans );
				break;
			case Equip:
				equipBans.remove( configString );
				config.set( "Bans.Equip", equipBans );
				break;
			case Crafting:
				craftingBans.remove( configString );
				config.set( "Bans.Crafting", craftingBans );
				break;
			case Ownership:
				ownershipBans.remove( configString );
				config.set( "Bans.Ownership", ownershipBans );
				break;
			case World:
				worldBans.remove( configString );
				config.set( "Bans.World", worldBans );
				break;
			default:
				// Should never reach here if all enum cases covered
				ItemRestrict.logger.warning( "Unknown ActionType detected: " + actionType.toString() );
				return;
			}
			plugin.saveConfig();
			sender.sendMessage( "Item Unbanned" );
	}
}

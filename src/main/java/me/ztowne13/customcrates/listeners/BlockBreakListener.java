package me.ztowne13.customcrates.listeners;

import me.ztowne13.customcrates.Messages;
import me.ztowne13.customcrates.SettingsValue;
import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.crates.Crate;
import me.ztowne13.customcrates.crates.PlacedCrate;
import me.ztowne13.customcrates.crates.options.ObtainType;
import me.ztowne13.customcrates.players.PlayerManager;
import me.ztowne13.customcrates.utils.CrateUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    SpecializedCrates cc;

    public BlockBreakListener(SpecializedCrates cc) {
        this.cc = cc;
    }

    @EventHandler
    public void onBreakPlacedCrate(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Location l = e.getBlock().getLocation();

        if (PlacedCrate.crateExistsAt(cc, l)) {
            PlacedCrate cm = PlacedCrate.get(cc, l);
            Crate crates = cm.getCrate();

            if (crates.getSettings().getObtainType().isStatic()) {
                if (!p.hasPermission("customcrates.admin") && !p.hasPermission("specializedcrates.admin")) {
                    e.setCancelled(true);
                    Messages.FAILED_BREAK_CRATE.msgSpecified(cc, p, new String[]{"%crate%", "%reason%"},
                            new String[]{crates.getDisplayName(), "static"});
                }
                return;
            }

            cm.delete();
            Messages.BROKEN_CRATE.msgSpecified(cc, p, new String[]{"%crate%"}, new String[]{crates.getDisplayName()});
        }
        // LUCKY CHEST / MINE CRATES
        else {
            // Event isn't already cancelled
            if (!e.isCancelled()) {
                // Not in creative mode or creative mode is allowed
                if (!p.getGameMode().equals(GameMode.CREATIVE) || (Boolean) SettingsValue.LUCKYCHEST_CREATIVE.getValue(cc)) {
                    // Luckychests enabled
                    if (PlayerManager.get(cc, p).getPdm().isActivatedLuckyChests()) {
                        // Cycle through all potential crates
                        for (Crate crates : Crate.getLoadedCrates().values()) {
                            // Check if the crate is a lucky chest and if it is enabled
                            if (CrateUtils.isCrateUsable(crates) && crates.getSettings().luckyChestSettingsExists() &&
                                    crates.getSettings().getObtainType().equals(ObtainType.LUCKYCHEST)) {
                                // Check if the lucky chesty should be placed at the location
                                if (crates.getSettings().getLuckyChestSettings().checkRun(e.getBlock())) {
                                    // Check if this block is a placed block or not and whether or not that's okay
                                    if ((!e.getBlock().hasMetadata("PLACED") ||
                                            e.getBlock().getMetadata("PLACED") == null) ||
                                            (Boolean) SettingsValue.LUCKYCHEST_ALLOW_PLACED_BLOCKS.getValue(cc)) {
                                        // Check to make sure the player has the permission or doesn't need the permission
                                        if (!crates.getSettings().getLuckyChestSettings().isRequirePermission() ||
                                                e.getPlayer().hasPermission(crates.getSettings().getPermission())) {
                                            PlacedCrate cm = PlacedCrate.get(cc, e.getBlock().getLocation());
                                            cm.setup(crates, true);
                                            Messages.FOUND_LUCKY_CHEST.msgSpecified(cc, p);
                                            e.setCancelled(true);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

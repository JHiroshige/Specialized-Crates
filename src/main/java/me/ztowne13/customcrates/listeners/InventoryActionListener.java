package me.ztowne13.customcrates.listeners;

import me.ztowne13.customcrates.Messages;
import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.crates.Crate;
import me.ztowne13.customcrates.crates.types.animations.AnimationDataHolder;
import me.ztowne13.customcrates.crates.types.animations.CrateAnimation;
import me.ztowne13.customcrates.interfaces.igc.crates.IGCMultiCrateMain;
import me.ztowne13.customcrates.interfaces.igc.crates.previeweditor.IGCCratePreviewEditor;
import me.ztowne13.customcrates.interfaces.igc.fileconfigs.rewards.IGCDragAndDrop;
import me.ztowne13.customcrates.interfaces.items.ItemBuilder;
import me.ztowne13.customcrates.players.PlayerManager;
import me.ztowne13.customcrates.utils.ChatUtils;
import me.ztowne13.customcrates.utils.CrateUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class InventoryActionListener implements Listener
{
    SpecializedCrates cc;

    public InventoryActionListener(SpecializedCrates cc)
    {
        this.cc = cc;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e)
    {
        cc.getDu().log("onInventoryDrag - CALL", getClass());

        Player player = (Player) e.getWhoClicked();
        PlayerManager playerManager = PlayerManager.get(cc, player);

        if (playerManager.isInCrate() || playerManager.isInRewardMenu())
        {
            cc.getDu().log("onInventoryDrag - CANCELLED");
            e.setCancelled(true);
        }
        else if (playerManager.isInOpenMenu())
        {
            if (e.getView().getTopInventory() != null)
            {
                if (playerManager.getOpenMenu() instanceof IGCCratePreviewEditor &&
                        e.getRawSlots().equals(e.getInventorySlots()))
                {
                    try
                    {
                        for (int slot : e.getInventorySlots())
                            ((IGCCratePreviewEditor) playerManager.getOpenMenu())
                                    .manageClick(slot, true, e.getNewItems().values().iterator().next());
                        e.setCancelled(true);
                    }
                    catch (Exception exc)
                    {
                        exc.printStackTrace();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e)
    {
        Player p = (Player) e.getWhoClicked();
         PlayerManager pm = PlayerManager.get(cc, p);

        int slot = e.getSlot();

        if (pm.isInCrate() || pm.isInRewardMenu())
        {
            e.setCancelled(true);

            if (isntPlayerInventory(e, pm))
            {

                if (pm.isInRewardMenu() && pm.getLastPage() != null)
                {
                    pm.getLastPage().handleInput(p, e.getSlot());
                }

                // Handle multicrate click
                if (pm.isInCrate() && pm.getOpenCrate().isMultiCrate())
                {
                    Crate crate = pm.getOpenCrate();
                    crate.getSettings().getMultiCrateSettings().checkClick(pm, slot, e.getClick());
                    e.setCancelled(true);
                }
                // Handle discover animation click
                else if (pm.isInCrate())
                {
                    pm.getOpenCrate().getSettings().getAnimation().handleClick(pm.getCurrentAnimation(), slot);
                }
            }
        }

        /*if (pm.isWaitingForClose())
        {
            e.setCancelled(true);
            pm.closeCrate();
            for (Reward r : pm.getWaitingForClose())
            {
                r.runCommands(p);
            }
            pm.setWaitingForClose(null);
            p.closeInventory();
        }*/
    }

    /**
     * Handles anvil crafting - mainly to prevent key/crate renaming.
     *
     * @param e The event passed by the server
     */
    @EventHandler
    public void onAnvilClick(InventoryClickEvent e)
    {
        if(e.getInventory().getType().equals(InventoryType.ANVIL))
        {
            if(e.getInventory().getItem(2) != null)
            {
                ItemBuilder builder = new ItemBuilder(e.getInventory().getItem(2));
                if(builder.hasDisplayName())
                {
                    if(CrateUtils.searchByCrate(builder.get()) != null || CrateUtils.searchByKey(builder.get()) != null)
                    {
                        e.setCancelled(true);
                        e.getWhoClicked().closeInventory();
                        Messages.CANT_CRAFT_KEYS.msgSpecified(cc, (Player) e.getWhoClicked());
                    }
                }
            }
        }
    }

    /**
     * Handles inventory clicks intended in the in-game config inventory editors.
     *
     * @param e The event passed by the server
     */
    @EventHandler
    public void onIGCClick(InventoryClickEvent e)
    {
        Player p = (Player) e.getWhoClicked();
        PlayerManager pm = PlayerManager.get(cc, p);

        if (p.hasPermission("customcrates.admin") && pm.isInOpenMenu())
        {
            if (!(e.getClickedInventory() == null || e.getView().getTopInventory() == null))
            {
                if (e.getClickedInventory().equals(e.getView().getTopInventory()) &&
                        !e.getView().getTitle().equalsIgnoreCase(ChatUtils.toChatColor("&c&lClose to save")))
                {
                    if (!(pm.getOpenMenu() instanceof IGCDragAndDrop) || e.getSlot() == 52 || e.getSlot() == 53)
                        e.setCancelled(true);

                    try
                    {
                        pm.getOpenMenu().manageClick(e.getSlot());
                    }
                    catch (Exception exc)
                    {
                        exc.printStackTrace();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCrateAnimationESCKey(InventoryCloseEvent e)
    {
        Player player = (Player) e.getPlayer();
        PlayerManager pm = PlayerManager.get(cc, player);

        if(pm.isInCrateAnimation())
        {
            AnimationDataHolder dataHolder = pm.getCurrentAnimation();
            dataHolder.getCrateAnimation().handleKeyPress(dataHolder, CrateAnimation.KeyType.ESC);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e)
    {
        final Player p = (Player) e.getPlayer();
        final PlayerManager pm = PlayerManager.get(cc, p);

//        pm.setInRewardMenu(false);
        pm.setLastPage(null);

        if (pm.isInOpenMenu() && !pm.getOpenMenu().isInInputMenu())
        {

            if (e.getView().getTitle().equalsIgnoreCase(ChatUtils.toChatColor("&7&l> &6&lClose to save")))
            {

                pm.getOpenMenu().manageClick(-1);
                ChatUtils.msgSuccess(p,
                        "Successfully saved all rewards. Please go through and update all of their commands as well as their chance values.");
                Bukkit.getScheduler().scheduleSyncDelayedTask(cc, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        pm.getOpenMenu().up();
                    }
                }, 1);
            }
            else if (e.getView().getTitle().equalsIgnoreCase(ChatUtils.toChatColor("&c&lClose to save")))
            {
                ChatUtils.msgInfo(p, "There are unsaved changes, please remember to save.");

                Bukkit.getScheduler().scheduleSyncDelayedTask(cc, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        pm.getOpenMenu().open();
                    }
                }, 1);
            }
            else if (pm.getOpenMenu() instanceof IGCCratePreviewEditor)
            {
                ((IGCCratePreviewEditor) pm.getOpenMenu()).getCustomRewardDisplayer().saveAllPages();
                Bukkit.getScheduler().scheduleSyncDelayedTask(cc, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (pm.getOpenMenu() instanceof IGCCratePreviewEditor)
                            pm.getOpenMenu().up();
                    }
                }, 1);
            }
            else if (!(pm.getOpenMenu() instanceof IGCMultiCrateMain))
            {
                pm.setOpenMenu(null);
                Bukkit.getScheduler().scheduleSyncDelayedTask(cc, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (!pm.isInOpenMenu())
                        {
                            ChatUtils.msg(p, "&9&lNOTE: &bType &f'/scrates !' &bto reopen to your last config menu!");
                        }
                    }
                }, 1);
            }
        }

        if(pm.isInRewardMenu())
        {
            if(pm.getNextPageInventoryCloseGrace() <= cc.getTotalTicks())
            {
                pm.setInRewardMenu(false);
                final Inventory inv = e.getInventory();
                Bukkit.getScheduler().runTaskLater(cc, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(inv.getType().equals(InventoryType.CRAFTING))
                        {
                            return;
                        }

                        p.openInventory(inv);
                        p.closeInventory();
                    }
                }, 1);
            }
        }


        if (pm.isInCrate() || pm.isInRewardMenu())
        {
            if (pm.getOpenCrate() != null && pm.getOpenCrate().isMultiCrate())
            {
                pm.closeCrate();
            }
        }
    }

    public boolean isntPlayerInventory(InventoryClickEvent e, PlayerManager pm)
    {
        cc.getDu().log("onInventoryClick - In crate or reward menu (" + pm.isInCrate() + " : " + pm.isInRewardMenu() +
                ")", getClass());
        if (!(e.getClickedInventory() == null || e.getWhoClicked().getInventory() == null))
        {
            cc.getDu().log("onInventoryClick - Clicked inventory and clicker aren't null.");
            if (!e.getClickedInventory().equals(e.getWhoClicked().getInventory()))
            {
                return true;
            }
        }
        return false;
    }
}


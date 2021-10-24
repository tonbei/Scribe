package com.tonbei.scribe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scribe extends JavaPlugin implements Listener {

    static final boolean debug = false;

    static Logger logger = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        if(logger == null) logger = this.getLogger();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPlaced(PrepareAnvilEvent e) {
        Player p = (Player) e.getView().getPlayer();

        if(p.hasPermission("scribe.use")) {
            AnvilInventory ai = e.getInventory();
            ItemStack first = ai.getItem(0);
            ItemStack second = ai.getItem(1);
            ItemStack result = ai.getItem(2);

            if(first == null || second == null) return;

            if(first.getType() == Material.WRITABLE_BOOK && !second.getEnchantments().isEmpty()){
                if(e.getResult() != null || result != null){
                    logger.log(Level.SEVERE, "Plugin conflicts");
                    p.sendMessage("[" + ChatColor.GREEN + "Scribe" + ChatColor.RESET + "] " + ChatColor.RED + "Plugin conflicts");
                    return;
                }

                /* コストが0以下だとスロットからアイテムを取り出せない */
                int cost = 1;
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                for(Map.Entry<Enchantment, Integer> entry : second.getEnchantments().entrySet()){
                    if(entry.getKey().equals(Enchantment.BINDING_CURSE) || entry.getKey().equals(Enchantment.VANISHING_CURSE)){
                        cost += 10;
                    }else{
                        meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
                if(meta.hasStoredEnchants()){
                    book.setItemMeta(meta);

                    ai.setRepairCost(ai.getRepairCost() + cost);
                    e.setResult(book);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilInvOpened(InventoryOpenEvent e) {
        if(e.getInventory() instanceof AnvilInventory){
            AnvilInventory ai = (AnvilInventory) e.getInventory();
            ai.setMaximumRepairCost(1024);

            if(debug){
                e.getPlayer().sendMessage("[" + ChatColor.GREEN + "Scribe" + ChatColor.RESET + "] " + "MaximumRepairCost : " + ai.getMaximumRepairCost());
                logger.log(Level.INFO, "MaximumRepairCost : " + ai.getMaximumRepairCost());
            }
        }
    }
}

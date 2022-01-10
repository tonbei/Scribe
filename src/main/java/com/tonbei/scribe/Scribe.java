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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scribe extends JavaPlugin implements Listener {

    static final boolean debug = false;

    static Logger logger = null;

    private static final int MAX_ENCHANT_LEVEL = 10;
    private static final int MAX_ANVIL_USE_COUNT = 4;

    private static final Map<Enchantment, Integer> ENCHANTMENT_COST;

    static {
        Map<Enchantment, Integer> temp = new HashMap<>();
        temp.put(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        temp.put(Enchantment.PROTECTION_FIRE, 1);
        temp.put(Enchantment.PROTECTION_FALL, 1);
        temp.put(Enchantment.PROTECTION_EXPLOSIONS, 2);
        temp.put(Enchantment.PROTECTION_PROJECTILE, 1);
        temp.put(Enchantment.OXYGEN, 2);
        temp.put(Enchantment.WATER_WORKER, 2);
        temp.put(Enchantment.THORNS, 1);
        temp.put(Enchantment.DEPTH_STRIDER, 2);
        temp.put(Enchantment.FROST_WALKER, 2);
        temp.put(Enchantment.BINDING_CURSE, 4);
        temp.put(Enchantment.DAMAGE_ALL, 1);
        temp.put(Enchantment.DAMAGE_UNDEAD, 1);
        temp.put(Enchantment.DAMAGE_ARTHROPODS, 1);
        temp.put(Enchantment.KNOCKBACK, 1);
        temp.put(Enchantment.FIRE_ASPECT, 2);
        temp.put(Enchantment.LOOT_BONUS_MOBS, 2);
        temp.put(Enchantment.SWEEPING_EDGE, 2);
        temp.put(Enchantment.DIG_SPEED, 1);
        temp.put(Enchantment.SILK_TOUCH, 4);
        temp.put(Enchantment.DURABILITY, 1);
        temp.put(Enchantment.LOOT_BONUS_BLOCKS, 2);
        temp.put(Enchantment.ARROW_DAMAGE, 1);
        temp.put(Enchantment.ARROW_KNOCKBACK, 2);
        temp.put(Enchantment.ARROW_FIRE, 2);
        temp.put(Enchantment.ARROW_INFINITE, 4);
        temp.put(Enchantment.LUCK, 2);
        temp.put(Enchantment.LURE, 2);
        temp.put(Enchantment.LOYALTY, 1);
        temp.put(Enchantment.IMPALING, 2);
        temp.put(Enchantment.RIPTIDE, 2);
        temp.put(Enchantment.CHANNELING, 4);
        temp.put(Enchantment.MULTISHOT, 2);
        temp.put(Enchantment.QUICK_CHARGE, 1);
        temp.put(Enchantment.PIERCING, 1);
        temp.put(Enchantment.MENDING, 2);
        temp.put(Enchantment.VANISHING_CURSE, 4);
        temp.put(Enchantment.SOUL_SPEED, 4);
        ENCHANTMENT_COST = Collections.unmodifiableMap(temp);
    }

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

                /* - コストが0以下だとスロットからアイテムを取り出せない */
                /* + Paper 1.17.1 #387で修正*/
                int cost = 0;
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

            EnchantmentStorageMeta secondMeta;

            if (second.getType() == Material.ENCHANTED_BOOK && (secondMeta = (EnchantmentStorageMeta) second.getItemMeta()).hasStoredEnchants()) {
                if (e.getResult() != null || result != null) {
                    ai.setItem(2, null);
                    e.setResult(null);
                }

                boolean isEnchantedBook = (first.getType() == Material.ENCHANTED_BOOK);
                Map<Enchantment, Integer> enchantments = new HashMap<>();
                (isEnchantedBook ? ((EnchantmentStorageMeta) first.getItemMeta()).getStoredEnchants() : first.getEnchantments()).forEach(
                        (enchantment, level) -> enchantments.put(enchantment, (enchantment.getMaxLevel() <= 1) ? 1 : Math.min(MAX_ENCHANT_LEVEL, level)));

                int cost = 0;
                int repairCost = 0;

                for (Map.Entry<Enchantment, Integer> ench : secondMeta.getStoredEnchants().entrySet()) {
                    Enchantment enchant = ench.getKey();
                    int addLevel = ench.getValue();
                    boolean levelFlag = (enchant.getMaxLevel() <= 1);

                    if (enchantments.containsKey(enchant)) {
                        if (levelFlag) continue;

                        int originalCost = enchantments.get(enchant);
                        enchantments.put(enchant, addLevel = Math.min(MAX_ENCHANT_LEVEL, originalCost + addLevel));
                        cost += ENCHANTMENT_COST.getOrDefault(enchant, 1) * Math.max(addLevel - originalCost, 0);
                    } else {
                        if (isEnchantedBook || enchant.canEnchantItem(first)) {
                            enchantments.put(enchant, addLevel = (levelFlag ? 1 : Math.min(MAX_ENCHANT_LEVEL, addLevel)));
                            cost += ENCHANTMENT_COST.getOrDefault(enchant, 1) * addLevel;
                        }
                    }
                }

                if (cost <= 0) return;

                if (!isEnchantedBook) {
                    if (first.getItemMeta() instanceof Repairable) {
                        repairCost = ((Repairable) first.getItemMeta()).getRepairCost();
                        cost += repairCost;
                        repairCost = (int) Math.round(Math.pow(2, Math.min((int) Math.round((Math.log(repairCost + 1) / Math.log(2))) + 1, MAX_ANVIL_USE_COUNT))) - 1;
                    }
                }

                ItemStack resultItem = first.clone();
                if (isEnchantedBook) {
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) resultItem.getItemMeta();
                    enchantments.forEach((enchantment, level) -> bookMeta.addStoredEnchant(enchantment, level, true));
                    resultItem.setItemMeta(bookMeta);
                } else {
                    if (resultItem.getItemMeta() instanceof Repairable) {
                        ItemMeta repairableMeta = resultItem.getItemMeta();
                        ((Repairable) repairableMeta).setRepairCost(repairCost);
                        resultItem.setItemMeta(repairableMeta);
                    }
                    resultItem.addUnsafeEnchantments(enchantments);
                }

                e.setResult(resultItem);
                e.getInventory().setRepairCost(cost);
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

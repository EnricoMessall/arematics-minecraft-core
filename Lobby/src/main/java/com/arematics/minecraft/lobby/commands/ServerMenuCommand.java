package com.arematics.minecraft.lobby.commands;

import com.arematics.minecraft.core.command.CoreCommand;
import com.arematics.minecraft.core.inventories.helper.InventoryPlaceholder;
import com.arematics.minecraft.core.items.CoreItem;
import com.arematics.minecraft.core.server.entities.player.CorePlayer;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.springframework.stereotype.Component;

@Component
public class ServerMenuCommand extends CoreCommand {

    public ServerMenuCommand(){
        super("servermenu");
    }

    @Override
    public void onDefaultExecute(CommandSender sender) {
        if(! (sender instanceof Player)) return;
        CorePlayer player = CorePlayer.get((Player) sender);
        Inventory inv = Bukkit.createInventory(null, 3*9, "§bSelect Server");
        player.openTotalBlockedInventory(inv);
        InventoryPlaceholder.fillOuterLine(inv, DyeColor.BLUE);
        inv.setItem(9 + 3, CoreItem.generate(Material.DIAMOND_SWORD)
                .bindCommand("server pvp")
                .setName("§bPVP"));
        inv.setItem(9 + 5, CoreItem.generate(Material.GOLD_AXE)
                .bindCommand("server guns")
                .setName("§6GUNS"));
    }
}

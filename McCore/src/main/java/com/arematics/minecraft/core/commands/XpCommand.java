package com.arematics.minecraft.core.commands;

import com.arematics.minecraft.core.annotations.Perm;
import com.arematics.minecraft.core.annotations.SubCommand;
import com.arematics.minecraft.core.command.CoreCommand;
import com.arematics.minecraft.core.server.CorePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

@Component
@Perm(permission = "xp", description = "send xp to a player")
public class XpCommand extends CoreCommand {
    public XpCommand() {
        super("xp", "xppay","payxp");
    }

    @Override
    public void onDefaultExecute(CommandSender sender) {
        CorePlayer player = CorePlayer.get((Player) sender);

        player.info("You have " + player.getPlayer().getTotalExperience() + " total experience!").handle();
    }

    @SubCommand("{target} {amount}")
    public void sendXp(CorePlayer player, CorePlayer target, Integer amount) {
        sendExperience(player, target, amount);
    }

    private static void sendExperience(CorePlayer player, CorePlayer target, int amount) {
        if(player.getPlayer().getTotalExperience() >= amount) {
            player.getPlayer().setExp(player.getPlayer().getTotalExperience() - amount);
        }
    }

}

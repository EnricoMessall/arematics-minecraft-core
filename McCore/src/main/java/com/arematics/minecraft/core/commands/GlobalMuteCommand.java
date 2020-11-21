package com.arematics.minecraft.core.commands;

import com.arematics.minecraft.core.annotations.Perm;
import com.arematics.minecraft.core.annotations.SubCommand;
import com.arematics.minecraft.core.command.CoreCommand;
import com.arematics.minecraft.core.server.CorePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

@Component
@Perm(permission = "globalmute", description = "mute the global chat")
public class GlobalMuteCommand extends CoreCommand {

    private static boolean isGlobalMuteActive = false;

    public GlobalMuteCommand() { super("globalmute", "glm"); }

    @Override
    protected boolean onDefaultCLI(CommandSender sender) {
        enableGlobalMute(sender);
        return true;
    }

    @SubCommand("info")
    public void getInfo(CorePlayer player) {
        player.info("Globalmute: " + isGlobalMuteActive).handle();
    }

    private void enableGlobalMute(CommandSender sender) {
        isGlobalMuteActive = !isGlobalMuteActive;
        String globalMuteStatus = isGlobalMuteActive ? "muted" : "demuted";

        Bukkit.getOnlinePlayers().stream()
                .map(CorePlayer::get)
                .forEach(player -> player
                        .info("The chat was " + globalMuteStatus +" by " + ((Player) sender).getDisplayName()).handle());

    }

    public static boolean getGlobalMuteStatus() {
        return isGlobalMuteActive;
    }
}

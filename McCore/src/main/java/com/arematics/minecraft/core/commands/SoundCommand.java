package com.arematics.minecraft.core.commands;

import com.arematics.minecraft.core.command.*;
import com.arematics.minecraft.core.command.processor.Processor;
import com.arematics.minecraft.core.command.processor.parser.sender.PlayerOnly;
import com.arematics.minecraft.core.language.LanguageAPI;
import com.arematics.minecraft.core.utils.ListUtils;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

@CMD
public class SoundCommand extends CoreCommand {

    @Override
    public String[] getCommandNames() {
        return new String[]{"sound"};
    }

    @Override
    public Processor[] defineExecutingProcessors() {
        return new Processor[0];
    }

    @Override
    public boolean matchAnyAccess() {
        return true;
    }

    @Default
    public boolean sendInfo(CommandSender sender){
        LanguageAPI.injectable("cmd_not_valid")
                .inject("%cmd_usage%", () -> "\n/sound list\n/sound list <startsWith>\n/sound <Name>")
                .send((Player)sender);
        return true;
    }

    @PlayerOnly
    @Sub("list")
    public boolean list(Player player){
        return listSelected(player, "");
    }

    @PlayerOnly
    @Sub("list {startsWith}")
    public boolean listSelected(Player player, String startsWith){
        LanguageAPI.injectable("listing")
                .inject(() -> "Sound")
                .inject(() -> ListUtils.getNameListStartsWith(Sound.class, startsWith))
                .send(player);
        return true;
    }

    @Sub("list date {date}")
    public boolean executeDate(CommandSender sender, LocalDateTime date){
        sender.sendMessage(date.toString());
        return true;
    }

    @Sub("list add {message}")
    public boolean addSelected(CommandSender sender, String message){
        sender.sendMessage(message);
        return true;
    }

    @Sub("{sound}")
    public boolean executeSound(CommandSender sender, Sound sound){
        Player player = (Player) sender;
        player.playSound(player.getLocation(), sound, 1, 1);
        return true;
    }
}

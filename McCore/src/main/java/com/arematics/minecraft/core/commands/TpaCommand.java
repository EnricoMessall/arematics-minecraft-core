package com.arematics.minecraft.core.commands;

import com.arematics.minecraft.core.annotations.Perm;
import com.arematics.minecraft.core.annotations.SubCommand;
import com.arematics.minecraft.core.annotations.Validator;
import com.arematics.minecraft.core.command.CoreCommand;
import com.arematics.minecraft.core.command.processor.validator.CombatValidator;
import com.arematics.minecraft.core.server.CorePlayer;
import com.arematics.minecraft.core.server.controller.PlayerTeleportController;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
@Perm(permission = "tpa", description = "Teleport to a player")
public class TpaCommand extends CoreCommand {

    private final PlayerTeleportController teleportController;

    @Autowired
    public TpaCommand(PlayerTeleportController teleportController) {
        super("tpa");
        this.teleportController = teleportController;
    }

    @SubCommand("{player}")
    public void sendTpa(@Validator(validators = CombatValidator.class) CorePlayer player, CorePlayer target) {
        if(player.equals(target)) {
            player.warn("You must not send yourself a request").handle();
            return;
        }
           getTeleportController().sendTpaRequest(player, target);
           player.info("You have sent a teleport request to the player " + target.getPlayer().getDisplayName()).handle();
           target.info("You have received a teleport request from " + player.getPlayer().getDisplayName() + ". Use /tpa accept or deny").handle();

    }

    @SubCommand("accept {tpaSender}")
    public void acceptTpa(@Validator(validators = CombatValidator.class) CorePlayer tpaSender, CorePlayer receiver) {

        if(getTeleportController().accept(tpaSender, receiver)) {
            tpaSender.info("You teleported yourself to " + receiver.getPlayer().getDisplayName()).handle();
            receiver.info(receiver.getPlayer().getDisplayName() + "was teleported to you").handle();
        } else {
            receiver.warn("no request to accept").handle();
        }

    }

    @SubCommand("deny {tpaSender}")
    public void denyTpa(@Validator(validators = CombatValidator.class) CorePlayer tpaSender, CorePlayer receiver) {


        if(getTeleportController().deny(tpaSender, receiver)) {
            tpaSender.info(receiver.getPlayer().getDisplayName() + " hat deine Anfrage abgelehnt").handle();
            receiver.info("Du hast " + receiver.getPlayer().getDisplayName() + " Anfrage abgelehnt").handle();
        } else {
            receiver.warn("keine request zum ablehnen").handle();
        }

    }

}

package com.arematics.minecraft.clans.commands;

import com.arematics.minecraft.clans.commands.validator.InClanValidator;
import com.arematics.minecraft.clans.commands.validator.NoClanValidator;
import com.arematics.minecraft.clans.commands.validator.SameClanValidator;
import com.arematics.minecraft.clans.utils.ClanPermissionArray;
import com.arematics.minecraft.clans.utils.ClanPermissions;
import com.arematics.minecraft.core.annotations.SubCommand;
import com.arematics.minecraft.core.annotations.Validator;
import com.arematics.minecraft.core.command.CoreCommand;
import com.arematics.minecraft.core.command.processor.parser.CommandProcessException;
import com.arematics.minecraft.core.command.processor.validator.BalanceValidator;
import com.arematics.minecraft.core.command.processor.validator.PositiveNumberValidator;
import com.arematics.minecraft.core.command.processor.validator.RequestValidator;
import com.arematics.minecraft.core.events.CurrencyEventType;
import com.arematics.minecraft.core.messaging.advanced.*;
import com.arematics.minecraft.core.messaging.injector.advanced.AdvancedMessageInjector;
import com.arematics.minecraft.core.server.Server;
import com.arematics.minecraft.core.server.entities.player.CorePlayer;
import com.arematics.minecraft.core.utils.ArematicsExecutor;
import com.arematics.minecraft.core.utils.CommandUtils;
import com.arematics.minecraft.data.mode.model.*;
import com.arematics.minecraft.data.service.ClanMemberService;
import com.arematics.minecraft.data.service.ClanRankService;
import com.arematics.minecraft.data.service.ClanService;
import com.arematics.minecraft.data.service.UserService;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ClanCommand extends CoreCommand {

    private final Map<ClanInvite, CorePlayer> clanInvites = new HashMap<>();

    private final ClanService clanService;
    private final ClanMemberService clanMemberService;
    private final ClanRankService clanRankService;
    private final UserService userService;
    private final Server server;

    @Autowired
    public ClanCommand(ClanService clanService,
                       ClanMemberService clanMemberService,
                       ClanRankService clanRankService,
                       UserService userService,
                       Server server){
        super("clan", true, "clans", "c");
        this.clanService = clanService;
        this.clanMemberService = clanMemberService;
        this.clanRankService = clanRankService;
        this.userService = userService;
        this.server = server;
    }

    @SubCommand("create {name} {tag}")
    public boolean createClan(@Validator(validators = NoClanValidator.class) CorePlayer player, String name, String tag)
            throws CommandProcessException {
        final String clanExists = "Clan with %typ% %value% already exists";
        if(player.getMoney() < 20000)
            throw new CommandProcessException("You need 20.000 Coins to create a clan");
        try{
            clanService.findClanByName(name);
            player.warn(clanExists)
                    .DEFAULT()
                    .replace("typ", "name")
                    .replace("value", name)
                    .handle();
        }catch (RuntimeException re){
            try{
                clanService.findClanByTag(tag);
                player.warn(clanExists)
                        .DEFAULT()
                        .replace("typ", "tag")
                        .replace("value", tag)
                        .handle();
            }catch (RuntimeException re2){
                boolean success = server.currencyController()
                        .createEvent(player)
                        .setAmount(20000)
                        .setEventType(CurrencyEventType.WASTE)
                        .setTarget("create-clan")
                        .onSuccess(() -> this.createNewClan(player, name, tag));
                if(success)
                    player.removeMoney(25000);
                else
                    throw new CommandProcessException("Clan creation payment could not be made. Please read security info");
            }
        }
        return true;
    }
    
    @SubCommand("delete")
    public void deleteClan(ClanMember member)
            throws CommandProcessException {
        if(!ClanPermissions.isAdmin(member)){
            member.online().get().warn("Not permitted to perform this command for your clan").handle();
            return;
        }
        Clan clan = member.getClan(clanService);
        clan.getAllOnline().forEach(clanPlayer -> clanPlayer.warn("Your clan has been deleted by an admin").handle());
        member.online().get().addMoney(clan.getCoins());
        clanService.delete(clan);
    }

    @SubCommand("invite {name}")
    public void invitePlayer(@Validator(validators = InClanValidator.class)
                                         CorePlayer player,
                             @Validator(validators = {NoClanValidator.class, RequestValidator.class})
                                     CorePlayer target) throws CommandProcessException {
        ClanMember member = clanMemberService.getMember(player);
        if(!ClanPermissions.canInvite(member))
            throw new CommandProcessException("Not permitted to perform this command for your clan");
        Clan clan = member.getClan(clanService);
        if(clan.getSlots() <= clan.getMembers().size()) throw new CommandProcessException("Your clan is full");
        String clanName = clan.getName();
        target.info("You have been invited to join clan %clan%. %accept% | %deny%")
                .setInjector(AdvancedMessageInjector.class)
                .replace("clan", new MSG(clanName))
                .replace("accept", PartBuilder.createHoverAndRun("ACCEPT", "§aAccept clan request",
                        "/clan accept " + clanName).setBaseColor(JsonColor.GREEN))
                .replace("deny", PartBuilder.createHoverAndRun("DENY", "§cDeny clan request",
                        "/clan deny " + clanName).setBaseColor(JsonColor.RED))
                .handle();
        player.info("Clan request send to " + target.getPlayer().getName()).handle();
        target.requests().addTimeout(player.getPlayer().getName());
        ClanInvite inviteKey = new ClanInvite(player, clan);
        clanInvites.put(inviteKey, target);
        ArematicsExecutor.asyncDelayed(() -> clanInvites.remove(inviteKey, target), 2, TimeUnit.MINUTES);

    }

    @SubCommand("accept {clan}")
    public void acceptClanRequest(@Validator(validators = NoClanValidator.class) CorePlayer player, Clan clan) {
        if(clanInvites.containsValue(player)){
            ClanInvite invation = clanInvites.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(player))
                    .findFirst()
                    .filter(entry -> entry.getKey().getClan().getName().equals(clan.getName()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if(invation == null){
                player.warn("You got no invite for clan " + clan.getName()).handle();
            }else{
                ClanRank rank = clanRankService.getClanRank(new ClanRankId(clan.getId(), "Member"));
                ClanMember member = new ClanMember(player.getUUID(), rank, 0, 0);
                clanMemberService.update(member);
                clan.getMembers().add(member);
                clanService.update(clan);
                clan.getAllOnline().forEach(online -> online.info("Player " + player.getPlayer().getName() + " joined the clan")
                        .handle());
                clanInvites.remove(invation, player);
            }
        }
    }

    @SubCommand("deny {clan}")
    public void denyClanRequest(CorePlayer player, Clan clan) {
        clanInvites.entrySet().stream()
                .filter(entry -> entry.getValue().equals(player))
                .findFirst()
                .filter(entry -> entry.getKey().getClan().equals(clan))
                .map(Map.Entry::getKey).ifPresent(invation -> denyInvation(invation, player));
    }

    private void denyInvation(ClanInvite invite, CorePlayer player){
        clanInvites.remove(invite, player);
        player.info("Clan invite denied").handle();
    }

    @SubCommand("remove {user}")
    public void removeClanMember(ClanMember member,
                                 @Validator(validators = SameClanValidator.class) ClanMember target) {
        if(!ClanPermissions.canKick(member)){
            member.online().get().warn("Not permitted to perform this command for your clan").handle();
            return;
        }
        ClanMember targetMember = clanMemberService.getMember(target.getUuid());
        Clan clan = member.getClan(clanService);
        if(!targetMember.getRank().getClanRankId().getClanId().equals(clan.getId()))
            throw new CommandProcessException("Not same clan");
        if(!ClanPermissions.rankLevelCorrect(member, targetMember))
            throw new CommandProcessException("Not allowed to kick this player");
        clan.getAllOnline().forEach(clanPlayer -> clanPlayer.info("Player got kicked").handle());
        clan.getMembers().remove(targetMember);
        clanService.update(clan);
        clanMemberService.delete(targetMember);
    }

    @SubCommand("leave")
    public void leaveClan(ClanMember member) {
        if(ClanPermissions.isAdmin(member))
            throw new CommandProcessException("As admin you can not leave, you need to use /clan delete.");
        else{
            Clan clan = member.getClan(clanService);
            clan.getAllOnline().forEach(clanPlayer -> clanPlayer.info("Player " + member.online().get().getPlayer().getName() + " has left the clan").handle());
            clan.getMembers().remove(member);
            clanService.update(clan);
            clanMemberService.delete(member);
            member.online().get().info("You have left the clan").handle();
        }
    }

    @SubCommand("rang {user} {rank}")
    public void setClanRang(ClanMember member,
                            @Validator(validators = SameClanValidator.class) ClanMember target,
                            String rawRank) {
        if(!ClanPermissions.isAdmin(member))
            throw new CommandProcessException("This command could only be performed by an administrator");
        Clan clan = member.getClan(clanService);
        ClanRank result = clan.getRanks().stream().filter(rank -> {
            try{
                return rank.getRankLevel() == Integer.parseInt(rawRank);
            }catch (NumberFormatException nfe){
                return rank.getClanRankId().getName().equals(rawRank);
            }
        }).findFirst().orElseThrow(() -> new CommandProcessException("Rank could not be found"));
        if(result.getRankLevel() == 0) member.setRank(target.getRank());
        target.setRank(result);
        clanMemberService.update(member);
        clanMemberService.update(target);
        member.online().get().info("Changed rank of %player% to %rank%")
                .DEFAULT()
                .replace("player", userService.getUserByUUID(target.getUuid()).getLastName())
                .replace("rank", result.getClanRankId().getName())
                .handle();
    }

    @SubCommand("stats {clan}")
    public void getClanStats(CorePlayer player, Clan clan) {
        player.info(CommandUtils.prettyHeader("Clan", clan.getName())).handle();
        ClanMember owner = clan.findOwner();
        String name = Bukkit.getOfflinePlayer(owner.getUuid()).getName();
        player.info(clan.readInformation())
                .setInjector(AdvancedMessageInjector.class)
                .replace("clan_tag", new Part(clan.getName()))
                .replace("clan_owner", new Part(name))
                .replace("clan_kills", new Part("" + clan.getKills()))
                .replace("clan_deaths", new Part("" + clan.getDeaths()))
                .replace("clan_coins", new Part(CommandUtils.prettyDecimal(clan.getCoins()) + " Coins"))
                .replace("clan_slots", new Part(clan.getMembers().size() + "/" + clan.getSlots()))
                .replace("members_list", MSGBuilder.join(clan.getMembers().stream()
                        .map(ClanMember::prettyPrint)
                        .collect(Collectors.toList()), ','))
                .disableServerPrefix()
                .handle();
    }

    @SubCommand("stats")
    public void getSelfClanStats(ClanMember member){
        getClanStats(member.online().get(), member.getClan(clanService));
    }

    @SubCommand("money add {amount}")
    public void addClanMoney(ClanMember member,
                             @Validator(validators = {BalanceValidator.class, PositiveNumberValidator.class})
                                     Double amount) {
        if(member.getMoney() < amount)
            throw new CommandProcessException("You dont have enough money to afford this");
        boolean success = this.server.currencyController()
                .createEvent(member.online().get())
                .setAmount(amount)
                .setEventType(CurrencyEventType.TRANSFER)
                .setTarget("clan")
                .onSuccess(() -> addMoneyToClan(member, amount));
        if(success){
            member.removeMoney(amount);
            member.online().get().info("Send " + amount + " coins to clan bank").handle();
        }
        else
            throw new CommandProcessException("Your payment to the clan bank could not be made");

    }

    private void addMoneyToClan(ClanMember member, double amount){
        Clan clan = member.getClan(clanService);
        clan.setCoins(clan.getCoins() + amount);
        clanService.update(clan);
        member.online().get().info("You have send %amount% coins to your clan")
                .DEFAULT()
                .replace("amount", String.valueOf(amount))
                .handle();
    }

    @SubCommand("money rem {amount}")
    public void removeClanMoney(ClanMember member,
                                @Validator(validators = PositiveNumberValidator.class) Double amount) {
        if(!ClanPermissions.isAdmin(member)) throw new CommandProcessException("Not allowed to perform this");
        if(member.getClan(clanService).getCoins() < amount) throw new CommandProcessException("Clan does not have enough coins");
        boolean success = this.server.currencyController()
                .createEvent(member.online().get())
                .setAmount(amount)
                .setEventType(CurrencyEventType.TRANSFER)
                .setTarget("clan-to-player")
                .onSuccess(() -> removeMoneyFromClan(member, amount));
        if(success) member.addMoney(amount);
        else
            throw new CommandProcessException("Your payment from the clan bank could not be made");
    }

    private void removeMoneyFromClan(ClanMember member, double amount){
        Clan clan = member.getClan(clanService);
        if(clan.getCoins() < amount) throw new CommandProcessException("Clan has not enough coins");
        clan.setCoins(clan.getCoins() - amount);
        clanService.update(clan);
        member.online().get().info("You have removed %amount% coins from your clan")
                .DEFAULT()
                .replace("amount", String.valueOf(amount))
                .handle();
    }

    @SubCommand("shop")
    public void clanShop(@Validator(validators = InClanValidator.class) CorePlayer player) {
        player.warn("Not implemented yet").handle();
    }

    private void createNewClan(CorePlayer player, String name, String tag) {
        try{
            Clan clan = clanService.createClan(name, tag);
            List<ClanRank> ranks = generateDefaultClanRanks(clan);
            clan.setRanks(new HashSet<>(ranks));
            ClanMember member = new ClanMember(player.getUUID(), ranks.get(0), 0, 0);
            clanMemberService.update(member);
            clan.getMembers().add(member);
            clanService.update(clan);
            player.info("Clan created successfully").handle();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private List<ClanRank> generateDefaultClanRanks(Clan clan){
        ClanRank owner = generateRank(clan.getId(), "Owner", "§4", 0, "admin");
        ClanRank moderator = generateRank(clan.getId(), "Moderator", "§c", 1,
                "invite", "remove", "use-money");
        ClanRank member = generateRank(clan.getId(), "Member", "§f", 2, "invite");
        return Lists.newArrayList(clanRankService.save(owner), clanRankService.save(moderator),
                clanRankService.save(member));
    }

    private ClanRank generateRank(long clanId, String name, String colorCode, int rank, String... permissions){
        return new ClanRank(ClanRankId.of(clanId, name), colorCode, rank, ClanPermissionArray.of(permissions));
    }
}

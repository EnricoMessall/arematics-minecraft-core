package com.arematics.minecraft.core.chat.controller;

import com.arematics.minecraft.core.Boots;
import com.arematics.minecraft.core.CoreBoot;
import com.arematics.minecraft.core.chat.ChatAPI;
import com.arematics.minecraft.core.chat.model.PlaceholderActionInput;
import com.arematics.minecraft.core.data.model.message.ChatClickAction;
import com.arematics.minecraft.core.data.model.message.ChatHoverAction;
import com.arematics.minecraft.core.data.model.placeholder.DynamicPlaceholder;
import com.arematics.minecraft.core.data.model.placeholder.ThemePlaceholder;
import com.arematics.minecraft.core.data.model.theme.ChatTheme;
import com.arematics.minecraft.core.data.model.theme.ChatThemeUser;
import com.arematics.minecraft.core.data.service.chat.ChatThemeService;
import com.arematics.minecraft.core.data.service.chat.ChatThemeUserService;
import com.arematics.minecraft.core.messaging.advanced.ClickAction;
import com.arematics.minecraft.core.messaging.advanced.HoverAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@Component
public class ChatThemeController {

    private final Map<UUID, ChatThemeUser> users = new HashMap<>();
    private final Map<String, ChatTheme> themes = new HashMap<>();

    public boolean loadThemes() {
        ChatThemeService service = Boots.getBoot(CoreBoot.class).getContext().getBean(ChatThemeService.class);
        List<ChatTheme> savedThemes = service.getAll();
        if(null == savedThemes || savedThemes.size() < 1) {
            return false;
        }
        savedThemes.forEach(theme -> {
            ChatAPI.registerTheme(theme.getThemeKey(), theme);
        });
        return true;

    }

    public void createAndSaveDefaults() {
        List<PlaceholderActionInput> defaultDynamicAndActions = new ArrayList<PlaceholderActionInput>() {{
            add(new PlaceholderActionInput("rank", null, null));
            add(new PlaceholderActionInput("name", null, null));
            add(new PlaceholderActionInput("chatMessage", null, null));
            add(new PlaceholderActionInput("arematics", new ChatHoverAction(HoverAction.SHOW_TEXT, "Unser Discord"), new ChatClickAction(ClickAction.OPEN_URL, "https://discordapp.com/invite/AAXk9Jb")));
        }};
        List<PlaceholderActionInput> debugDynamicAndActions = new ArrayList<PlaceholderActionInput>() {{
            add(new PlaceholderActionInput("rank", new ChatHoverAction(HoverAction.SHOW_TEXT, "%key% to %value%"), null));
            add(new PlaceholderActionInput("name", new ChatHoverAction(HoverAction.SHOW_TEXT, "%key% to %value%"), null));
            add(new PlaceholderActionInput("chatMessage", new ChatHoverAction(HoverAction.SHOW_TEXT, "%key% to %value%"), null));
        }};

        Set<ThemePlaceholder> themePlaceholders = new HashSet<ThemePlaceholder>() {{
            ThemePlaceholder chatDelim = new ThemePlaceholder();
            chatDelim.setBelongingThemeKey("default");
            chatDelim.setPlaceholderMatch("%chatDelim%");
            chatDelim.setPlaceholderKey("chatDelim");
            chatDelim.setValue(":");
            add(chatDelim);
        }};
        Set<ThemePlaceholder> themePlaceholdersDebug = new HashSet<ThemePlaceholder>() {{
            ThemePlaceholder chatDelim = new ThemePlaceholder();
            chatDelim.setBelongingThemeKey("debug");
            chatDelim.setPlaceholderMatch("%chatDelim%");
            chatDelim.setPlaceholderKey("chatDelim");
            chatDelim.setValue(":");
            ThemePlaceholder debug = new ThemePlaceholder();
            debug.setBelongingThemeKey("debug");
            debug.setPlaceholderMatch("%debug%");
            debug.setPlaceholderKey("debug");
            debug.setValue("[Debug]");
            add(debug);
            add(chatDelim);
        }};
        ChatTheme defaultTheme = ChatAPI.createTheme("default", defaultDynamicAndActions, themePlaceholders, "%arematics% %rank% %name%%chatDelim% %chatMessage%");
        ChatTheme debugTheme = ChatAPI.createTheme("debug", debugDynamicAndActions, themePlaceholdersDebug, "%rank% %name%%chatDelim% %chatMessage%");
        loadThemes();
    }

    /**
     *
     * @param themeKey internal name of theme
     * @param dynamicPlaceholderActions create these through api to assign hover/click actions, contains placeholder key and actions
     * @param themePlaceholders list of theme internal placeholders
     * @param format % encoded raw chat format
     * @return
     */
    public ChatTheme createTheme(String themeKey, List<PlaceholderActionInput> dynamicPlaceholderActions, Set<ThemePlaceholder> themePlaceholders, String format) {
        ChatTheme chatTheme = new ChatTheme();
        chatTheme.setThemeKey(themeKey);
        // chatTheme.setDynamicPlaceholders(dynamicPlaceholders);
        chatTheme.setThemePlaceholders(themePlaceholders);
        chatTheme.setFormat(format);
        dynamicPlaceholderActions.forEach(placeholderActionInput -> {
            String key = placeholderActionInput.getPlaceholderName();
            chatTheme.getDynamicPlaceholderKeys().add(key);
            chatTheme.getClickAction().put(key, placeholderActionInput.getClickAction());
            chatTheme.getHoverAction().put(key, placeholderActionInput.getHoverAction());
        });
        ChatThemeService service = Boots.getBoot(CoreBoot.class).getContext().getBean(ChatThemeService.class);
        ChatTheme saved = service.save(chatTheme);
        return saved;
    }

    public ChatTheme getTheme(String name) {
        return getThemes().get(name);
    }
    public void registerTheme(String name, ChatTheme theme) {
        getThemes().put(name, theme);
    }

    /**
     * registers player as chattheme user, is called on player join
     * @param player to register
     * @return
     */
    public void register(Player player) {
        ChatThemeUser user = Boots.getBoot(CoreBoot.class).getContext().getBean(ChatThemeUserService.class).getOrCreate(player.getUniqueId());
        ChatAPI.getTheme(user.getActiveTheme().getThemeKey()).getActiveUsers().add(user);
        getUsers().put(player.getUniqueId(), user);
    }

    public void logout(Player player) {
        ChatThemeUserService service = Boots.getBoot(CoreBoot.class).getContext().getBean(ChatThemeUserService.class);
        ChatThemeUser user = getUser(player);
        service.save(user);
        user.getActiveTheme().getActiveUsers().remove(user);
        ChatAPI.getTheme(user.getActiveTheme().getThemeKey()).getActiveUsers().remove(user);
        getUsers().remove(player.getUniqueId());
    }

    public ChatThemeUser getUser(Player player) {
        return getUsers().get(player.getUniqueId());
    }

    /**
     * sets active theme for chatthemeuser and adds to senders chattheme
     * @param player who is affected
     * @param theme which is used
     */
    public boolean setTheme(Player player, String theme) {
        ChatTheme newTheme = ChatAPI.getTheme(theme);
        if(null == newTheme) {
            return false;
        }
        ChatThemeUser user = getUser(player);
        ChatTheme old = ChatAPI.getTheme(user.getActiveTheme().getThemeKey());
        old.getActiveUsers().remove(user);
        user.setActiveTheme(newTheme);
        newTheme.getActiveUsers().add(user);
        return true;
    }
}

package com.arematics.minecraft.core.chat.controller;

import com.arematics.minecraft.core.chat.ChatAPI;
import com.arematics.minecraft.data.global.model.GlobalPlaceholder;
import com.arematics.minecraft.data.global.model.User;
import com.arematics.minecraft.data.service.PlaceholderService;
import com.arematics.minecraft.data.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Setter
@Getter
@Component
public class PlaceholderController {

    public static final String PLACEHOLDER_DELIMITER = "%";
    private final Map<String, GlobalPlaceholder> placeholders = new HashMap<>();
    private final List<String> reservedPlaceholderKeys = new ArrayList<>();
    private final PlaceholderService service;
    private final UserService userService;
    private ChatAPI chatAPI;

    @Autowired
    public PlaceholderController(PlaceholderService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    public void initPlaceholders() {
        registerPlaceholder(createPlaceholder("rank"));
        registerPlaceholder(createPlaceholder("name"));
        registerPlaceholder(createPlaceholder("chatMessage"));
        registerPlaceholder(createPlaceholder("arematics"));
        getReservedPlaceholderKeys().add("placeholderName");
        getReservedPlaceholderKeys().add("placeholderMatch");
        getReservedPlaceholderKeys().add("value");
    }


    public static String applyDelimiter(String placeholderKey) {
        return PLACEHOLDER_DELIMITER + placeholderKey + PLACEHOLDER_DELIMITER;
    }

    public static String stripPlaceholderDelimiter(String placeholderMatch) {
        return placeholderMatch.replaceAll(PLACEHOLDER_DELIMITER, "");
    }

    public GlobalPlaceholder getPlaceholder(String placeholderKey) {
       return getPlaceholders().get(placeholderKey);
    }

    private void validatePlaceholderKey(String placeholderKey) {
        if (getPlaceholders().containsKey(placeholderKey) || getReservedPlaceholderKeys().contains(placeholderKey)) {
            throw new UnsupportedOperationException("Der Placeholder: " + placeholderKey + " ist bereits registriert!");
        } else {
            getReservedPlaceholderKeys().add(placeholderKey);
        }
    }

    /**
     * saves and registers placeholder
     * @param placeholder created by createPlaceholder
     */
    public void registerPlaceholder(GlobalPlaceholder placeholder) {
        validatePlaceholderKey(placeholder.getPlaceholderKey());
        GlobalPlaceholder saved = service.save(placeholder);
        getPlaceholders().put(placeholder.getPlaceholderKey(), saved);
    }

    /**
     * creates placeholder object doesnt save or register
     * @param placeholderKey key
     * @return placeholder object
     */
    public GlobalPlaceholder createPlaceholder(String placeholderKey) {
        GlobalPlaceholder placeholder = new GlobalPlaceholder();
        placeholder.setPlaceholderKey(placeholderKey);
        placeholder.setPlaceholderMatch(applyDelimiter(placeholderKey));
        return placeholder;
    }

    /**
     * loads and inits placeholders
     * @return if database contains placeholders, otherwise load defaults
     */
    public boolean loadGlobalPlaceholders() {
        List<GlobalPlaceholder> placeholders = service.loadGlobals();
        if (null == placeholders || placeholders.size() < 3) {
            return false;
        }
        placeholders.forEach(this::registerPlaceholder);
        return true;
    }

    public void supply(Player player) {
        User user = userService.getUserByUUID(player.getUniqueId());
        supplyPlaceholder("rank", player, () -> user.getDisplayRank() != null ? user.getDisplayRank().getName() : user.getRank().getName());
        supplyPlaceholder("name", player, player::getDisplayName);
        supplyPlaceholder("chatMessage", player, chatAPI.getChatController()::getChatMessage);
        supplyPlaceholder("arematics", player, () -> "§0[§1Arem§9atics§0]§r");
    }

    public void supplyPlaceholder(String placeholderName, Player player, Supplier<String> supplier) {
        getPlaceholder(placeholderName).getValues().put(player, supplier);
    }

}

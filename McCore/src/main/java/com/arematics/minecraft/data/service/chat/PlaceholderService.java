package com.arematics.minecraft.data.service.chat;

import com.arematics.minecraft.data.chat.placeholder.GlobalPlaceholder;
import com.arematics.minecraft.data.chat.placeholder.ThemePlaceholder;
import com.arematics.minecraft.data.chat.DynamicPlaceholderRepository;
import com.arematics.minecraft.data.chat.ThemePlaceholderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceholderService {

    private final DynamicPlaceholderRepository dynamicRepository;
    private final ThemePlaceholderRepository themeRepository;

    @Autowired
    public PlaceholderService(DynamicPlaceholderRepository dynamicRepository, ThemePlaceholderRepository themeRepository) {
        this.dynamicRepository = dynamicRepository;
        this.themeRepository = themeRepository;
    }

    public List<GlobalPlaceholder> loadGlobals(){
        return dynamicRepository.findAll();
    }

    public GlobalPlaceholder getGlobalPlaceholder(String placeholderKey) {
        return dynamicRepository.findById(placeholderKey).orElse(null);
    }

    public GlobalPlaceholder save(GlobalPlaceholder globalPlaceholder){
        return dynamicRepository.save(globalPlaceholder);
    }

    public ThemePlaceholder getThemePlaceholder(String placeholderKey) {
        return themeRepository.findById(placeholderKey).orElse(null);
    }

    public ThemePlaceholder save(ThemePlaceholder themePlaceholder) {
        return themeRepository.save(themePlaceholder);
    }

}

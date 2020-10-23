package com.arematics.minecraft.core.command.processor.parser;

import org.springframework.stereotype.Component;

@Component
public class StringParser extends CommandParameterParser<String> {

    @Override
    public String doParse(String value) {
        return value;
    }
}

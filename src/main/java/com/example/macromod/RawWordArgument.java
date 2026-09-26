package com.example.macromod;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public final class RawWordArgument implements ArgumentType<String> {

    private static final RawWordArgument INSTANCE = new RawWordArgument();

    public static RawWordArgument rawWord() {
        return INSTANCE;
    }

    private RawWordArgument() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek()) && reader.peek() != '"') {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }
}

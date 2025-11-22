package wtf.dettex.implement.features.commands;

import wtf.dettex.api.other.command.ICommandSystem;
import wtf.dettex.api.other.command.argparser.IArgParserManager;
import wtf.dettex.implement.features.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}


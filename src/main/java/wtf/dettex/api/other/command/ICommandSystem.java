package wtf.dettex.api.other.command;

import wtf.dettex.api.other.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}

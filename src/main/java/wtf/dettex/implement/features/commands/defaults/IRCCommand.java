package wtf.dettex.implement.features.commands.defaults;

import net.minecraft.util.Formatting;

import wtf.dettex.api.other.command.Command;
import wtf.dettex.api.other.command.argument.IArgConsumer;
import wtf.dettex.api.other.command.exception.CommandException;
import wtf.dettex.api.other.command.helpers.TabCompleteHelper;
import wtf.dettex.Main;
import wtf.dettex.modules.impl.misc.IRC;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class IRCCommand extends Command {

    public IRCCommand() {
        super("irc");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            error();
            return;
        }

        String subCmd = args.getString().toLowerCase(Locale.US);
        var irc = Main.getInstance().getIrcManager();

        switch (subCmd) {
            case "ignore" -> {
                if (args.hasAny()) {
                    String nick = args.getString();
                    irc.ignoreNick(nick);
                    logDirect(nick + " добавлен в игнор", Formatting.GREEN);
                } else {
                    error();
                }
            }
            case "unignore" -> {
                if (args.hasAny()) {
                    String nick = args.getString();
                    irc.unignoreNick(nick);
                    logDirect(nick + " удалён из игнора", Formatting.GREEN);
                } else {
                    error();
                }
            }
            case "ignorelist" -> {
                if (irc.getIgnoredNicks().isEmpty()) {
                    logDirect("Список игнора пуст", Formatting.GRAY);
                } else {
                    logDirect("Игнор-лист: " + String.join(", ", irc.getIgnoredNicks()));
                }
            }
            default -> {
                String message = subCmd + (args.hasAny() ? " " + String.join(" ", args.getString()) : "");

                if (message.matches("(https?://|www\\.)\\S+")) {
                    logDirect("Ваше сообщение содержит ссылку.", Formatting.RED);
                    return;
                }

                if (IRC.getInstance().state) {
                    irc.messageHost(message);
                } else {
                    logDirect("Пожалуйста включите модуль IRC", Formatting.RED);
                }
            }
        }
    }

    private void error() {
        logDirect(Formatting.RED + "Ошибка в использовании" + Formatting.WHITE + ":");
        logDirect(Formatting.WHITE + ".irc " + Formatting.GRAY + "<" + Formatting.RED + "сообщение" + Formatting.GRAY + "> - отправить сообщение");
        logDirect(Formatting.WHITE + ".irc ignore " + Formatting.GRAY + "<" + Formatting.RED + "name" + Formatting.GRAY + "> - игнорировать сообщения юзера");
        logDirect(Formatting.WHITE + ".irc unignore " + Formatting.GRAY + "<" + Formatting.RED + "name" + Formatting.GRAY + "> - удалить из игнора юзера");
        logDirect(Formatting.WHITE + ".irc ignorelist" + Formatting.GRAY + " - список игнорируемых");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                return new TabCompleteHelper()
                        .prepend("ignore", "unignore", "ignorelist")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление IRC чатом";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для работы с IRC чатом",
                "",
                "Использование:",
                "> irc <сообщение> - отправить сообщение в IRC",
                "> irc ignore <nick> - игнорировать пользователя",
                "> irc unignore <nick> - снять игнор с пользователя",
                "> irc ignorelist - показать список игнорируемых"
        );
    }
}

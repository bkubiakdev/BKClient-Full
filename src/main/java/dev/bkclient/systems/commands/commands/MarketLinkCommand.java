package dev.bkclient.systems.commands.commands;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.utility.game.AutoMarketRemote;
public class MarketLinkCommand {
    public Command command() {
        return CommandBuilder.begin("marketlink", b -> b.desc("Connect AutoMarket to the website")
            .param("key", p -> p.validator(ValidationResult::ok))
            .handler(ctx -> AutoMarketRemote.command((String)ctx.arguments().get(0)))).build();
    }
}

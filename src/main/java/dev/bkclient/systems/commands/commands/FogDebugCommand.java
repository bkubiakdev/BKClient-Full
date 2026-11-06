package dev.bkclient.systems.commands.commands;

import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.utility.render.ambience.FogDebugCapture;

public final class FogDebugCommand {
    public Command command() {
        return CommandBuilder.begin("fogdebug")
            .desc("Capture volumetric fog diagnostics locally")
            .param("action", p -> p.optional().literal("on", "off", "capture", "status"))
            .handler(ctx -> FogDebugCapture.command((String) ctx.arguments().get(0)))
            .build();
    }
}

package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.commands.ParameterValidator;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.utility.game.MessageUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;

/**
 * Rescues HUD elements that are switched on but nowhere to be seen.
 *
 * <p>The element picker only offers what is currently hidden, so an element that thinks it is
 * visible while being stranded, stacked under another one or scaled to nothing cannot be brought
 * back through the interface at all. This gives a way out from chat, and a way to find out what
 * the element actually believes about itself.
 */
public class HudCommand {

    /** Where elements land when reset: a readable column down the left, clear of the hotbar. */
    private static final float RESET_X = 10.0F;
    private static final float RESET_Y = 10.0F;
    private static final float RESET_STEP = 22.0F;

    public Command command() {
        return CommandBuilder.begin(
                "hud",
                b -> b.desc("commands.hud.description")
                        .param("action", p -> p.optional().literal("list", "reset", "show"))
                        .param("name", p -> p.optional().validator(NAME_VALIDATOR))
                        .handler(this::handle)
        ).build();
    }

    /**
     * Accepts any name, and offers the element names that exist right now while typing.
     *
     * <p>Read from the live HUD rather than a fixed list, so what tab-completion offers can never
     * drift away from what {@link #find} will actually match.
     */
    private static final ParameterValidator<Object> NAME_VALIDATOR = new ParameterValidator<>() {
        @Override
        public ValidationResult validate(String text) {
            return ValidationResult.ok(text);
        }

        @Override
        public List<String> suggestions(String partial) {
            if (BKClientInit.getInstance() == null || BKClientInit.getInstance().getHud() == null) {
                return List.of();
            }
            String needle = partial == null ? "" : partial.toLowerCase();
            List<String> names = new ArrayList<>();
            for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
                if (element.getName().toLowerCase().contains(needle)) {
                    names.add(element.getName());
                }
            }
            return names;
        }
    };

    private void handle(CommandContext ctx) {
        String action = (String) ctx.arguments().get(0);
        String name = (String) ctx.arguments().get(1);

        if (BKClientInit.getInstance().getHud() == null) {
            MessageUtility.info(Text.of("HUD is not ready yet."));
            return;
        }

        switch (action == null ? "list" : action.toLowerCase()) {
            case "reset" -> reset(name);
            case "show" -> show(name);
            default -> list();
        }
    }

    /**
     * Prints what every element thinks its state is.
     *
     * <p>Position and size are the useful part: an element sitting at a sane place with a width
     * of a few pixels is being drawn, just with nothing in it, which looks identical to missing.
     */
    private void list() {
        MessageUtility.info(Text.of("HUD elements:"));
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            MessageUtility.info(Text.of(String.format(
                    "  %s - %s at %.0f, %.0f  size %.0fx%.0f  scale %.2f",
                    element.getName(),
                    element.isShowing() ? "on" : "off",
                    element.getX(), element.getY(),
                    element.getWidth(), element.getHeight(),
                    element.getScale()
            )));
        }
    }

    /** Turns an element on without moving it. */
    private void show(String name) {
        HudElement element = find(name);
        if (element == null) {
            return;
        }
        element.setShowing(true);
        MessageUtility.info(Text.of(element.getName() + " is now shown."));
    }

    /**
     * Puts elements back somewhere they can be seen.
     *
     * <p>With no name every visible element is stacked down the left edge, which is deliberately
     * blunt: the point is to end up with a layout you can see and drag, not a pretty one.
     */
    private void reset(String name) {
        if (name != null && !name.isBlank()) {
            HudElement element = find(name);
            if (element == null) {
                return;
            }
            element.setShowing(true);
            element.pos(RESET_X, RESET_Y);
            MessageUtility.info(Text.of(element.getName() + " moved to the top left."));
            return;
        }

        float y = RESET_Y;
        int moved = 0;
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (!element.isShowing()) {
                continue;
            }
            element.pos(RESET_X, y);
            y += RESET_STEP;
            moved++;
        }
        MessageUtility.info(Text.of("Stacked " + moved + " visible elements down the left edge."));
    }

    private HudElement find(String name) {
        if (name == null || name.isBlank()) {
            MessageUtility.info(Text.of("Give an element name, for example: hud reset hud.player"));
            return null;
        }
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (element.getName().equalsIgnoreCase(name) || element.getName().toLowerCase().endsWith(name.toLowerCase())) {
                return element;
            }
        }
        MessageUtility.info(Text.of("No HUD element called \"" + name + "\". Run hud list to see the names."));
        return null;
    }
}

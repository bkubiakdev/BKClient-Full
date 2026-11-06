package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.ParameterValidator;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.systems.modules.modules.player.MineHelper;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Chat interface for MineHelper area selection. */
public final class MineSelectCommand implements IMinecraft {
    private static final String[] ACTIONS = {
            "pos1", "pos2", "pos3", "pos4", "pos5", "pos6", "pos7", "pos8", "pos9", "pos10",
            "pos11", "pos12", "pos13", "pos14", "pos15", "pos16", "pos17", "pos18", "pos19", "pos20",
            "clear", "info"
    };

    public Command command() {
        return CommandBuilder.begin("mine", builder -> builder.aliases("ms")
                .desc("MineHelper area commands")
                .<String>param("action", parameter -> parameter.validator(actionValidator()))
                .handler(context -> {
                    MineHelper helper = getHelper();
                    if (helper == null) {
                        MessageUtility.error(Text.of("MineHelper module is unavailable."));
                        return;
                    }

                    String action = ((String) context.arguments().getFirst()).toLowerCase();
                    

                    if (action.startsWith("pos")) {
                        setPosition(helper, parsePositionId(action));
                    } else if ("clear".equals(action)) {
                        helper.clearSelection();
                        MessageUtility.info(Text.of("MineHelper selection cleared."));
                    } else if ("info".equals(action)) {
                        printInfo(helper);
                    }
                }))
                .build();
    }

    private static ParameterValidator<String> actionValidator() {
        return new ParameterValidator<>() {
            @Override
            public ValidationResult validate(String text) {
                return contains(ACTIONS, text)
                        ? ValidationResult.ok(text)
                        : ValidationResult.error("Unknown MineHelper action");
            }

            @Override
            public List<String> suggestions(String partial) {
                ArrayList<String> result = new ArrayList<>(Arrays.asList(ACTIONS));
                MineHelper helper = getHelper();
                
                String normalized = partial.toLowerCase();
                return result.stream().filter(value -> value.startsWith(normalized)).toList();
            }
        };
    }

    private static MineHelper getHelper() {
        try {
            return BKClientInit.getInstance().getModuleManager().getModule(MineHelper.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean contains(String[] actions, String candidate) {
        return Arrays.stream(actions).anyMatch(action -> action.equalsIgnoreCase(candidate));
    }



    private static int parsePositionId(String action) {
        try {
            return Integer.parseInt(action.substring(3));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void setPosition(MineHelper helper, int id) {
        if (mc.player == null) {
            return;
        }
        if (id < 1 || id > helper.getMaxSelectionPositions()) {
            MessageUtility.error(Text.of("Invalid MineHelper position: pos" + id));
            return;
        }
        BlockPos position = helper.getTargetedOrFeetPos();
        if (position == null) {
            MessageUtility.error(Text.of("Could not determine a position."));
            return;
        }
        helper.setSelectionPos(id, position);
        MessageUtility.info(Text.of("pos" + id + " set to " + position.toShortString()));
    }

    private static void printInfo(MineHelper helper) {
        StringBuilder message = new StringBuilder("MineHelper selection: ");
        boolean any = false;
        for (int i = 1; i <= helper.getMaxSelectionPositions(); i++) {
            BlockPos position = helper.getSelectionPos(i);
            if (position == null) {
                continue;
            }
            if (any) {
                message.append(", ");
            }
            message.append("pos").append(i).append("=(").append(position.toShortString()).append(')');
            any = true;
        }
        if (!any) {
            message.append("none");
        }
        MessageUtility.info(Text.of(message.toString()));
    }
}

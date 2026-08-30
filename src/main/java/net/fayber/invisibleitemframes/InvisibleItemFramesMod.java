package net.fayber.invisibleitemframes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Shift right-click an item frame or sign with an empty hand to toggle its
// visibility. Actual interaction logic lives in ItemFrameInteractionHandler
// and SignInteractionHandler; config surface is InvisibleItemFramesConfig.
public class InvisibleItemFramesMod implements ModInitializer {
    public static final String MOD_ID = "invisibleitemframes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        InvisibleItemFramesConfig.load();
        InvisibleItemFramesNetworking.register();

        UseEntityCallback.EVENT.register(ItemFrameInteractionHandler::onUseEntity);
        UseBlockCallback.EVENT.register(SignInteractionHandler::onUseBlock);

        CommandRegistrationCallback.EVENT.register(this::registerCommands);

        LOGGER.info("[InvisibleItemFrames] Initialized. Config: {}", InvisibleItemFramesConfig.get());
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                   CommandBuildContext buildContext,
                                   Commands.CommandSelection selection) {
        dispatcher.register(
            Commands.literal("invisibleitemframes")
                .then(
                    Commands.literal("config")
                        .requires(InvisibleItemFramesMod::isOperator)
                        .executes(context -> showConfig(context.getSource()))
                        .then(
                            Commands.literal("get")
                                .executes(context -> showConfig(context.getSource()))
                        )
                        .then(
                            Commands.literal("set")
                                .then(
                                    Commands.argument("key", StringArgumentType.word())
                                        .then(
                                            Commands.argument("value", StringArgumentType.word())
                                                .executes(context -> setConfig(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "key"),
                                                    StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static boolean isOperator(CommandSourceStack source) {
        if (source.getServer().isSingleplayer()) {
            return true;
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private int showConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[InvisibleItemFrames] Config: " + InvisibleItemFramesConfig.get()), false);
        return 1;
    }

    private int setConfig(CommandSourceStack source, String key, String value) {
        if (InvisibleItemFramesConfig.set(key, value)) {
            source.sendSuccess(() -> Component.literal("[InvisibleItemFrames] Set " + key
                    + " to " + value + ". New config: " + InvisibleItemFramesConfig.get()), true);
            return 1;
        }
        source.sendFailure(Component.literal("[InvisibleItemFrames] Unknown config key '" + key
                + "'. Use /invisibleitemframes config get to list valid keys."));
        return 0;
    }
}

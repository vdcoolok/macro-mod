package com.example.macromod;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.cache.CachedChunk;
import com.example.macromod.path.cache.CachedWorld;
import com.example.macromod.path.goal.GoalBlock;
import com.example.macromod.path.goal.GoalXZ;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MacroCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommands.literal("macro")
                .executes(ctx -> showHelp(ctx.getSource()))

                .then(ClientCommands.literal("load")
                    .then(ClientCommands.argument("filename", StringArgumentType.word())
                        .suggests(macroNameSuggestions())
                        .executes(ctx -> runMacro(ctx.getSource(), StringArgumentType.getString(ctx, "filename")))))

                .then(ClientCommands.literal("stop")
                    .executes(ctx -> {
                        MacroExecutor.stop();
                        ctx.getSource().sendFeedback(Component.literal("§aMacro stopped."));
                        return 1;
                    }))

                .then(ClientCommands.literal("create")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(ctx -> createMacro(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))

                .then(ClientCommands.literal("edit")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests(macroNameSuggestions())
                        .executes(ctx -> editMacro(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))

                .then(ClientCommands.literal("exit")
                    .executes(ctx -> exitEdit(ctx.getSource())))

                .then(ClientCommands.literal("save")
                    .executes(ctx -> saveEdit(ctx.getSource())))

                .then(ClientCommands.literal("set")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro set <smoothlook|botview> <true|false>"));
                        return 0;
                    })
                    .then(ClientCommands.literal("smoothlook")
                        .then(ClientCommands.literal("true")
                            .executes(ctx -> setSmoothLook(ctx.getSource(), true)))
                        .then(ClientCommands.literal("false")
                            .executes(ctx -> setSmoothLook(ctx.getSource(), false)))
                    )
                    .then(ClientCommands.literal("botview")
                        .then(ClientCommands.literal("true")
                            .executes(ctx -> setBotView(ctx.getSource(), true)))
                        .then(ClientCommands.literal("false")
                            .executes(ctx -> setBotView(ctx.getSource(), false)))
                    )
                )

                .then(ClientCommands.literal("action")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro action <add|remove|list|move>"));
                        return 0;
                    })
                    .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                            .suggests(actionSuggestions())
                            .executes(ctx -> addAction(ctx.getSource(), StringArgumentType.getString(ctx, "text")))))
                    .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("index", IntegerArgumentType.integer(1))
                            .suggests(lineNumberSuggestions())
                            .executes(ctx -> removeAction(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))))
                    .then(ClientCommands.literal("list")
                        .executes(ctx -> listActions(ctx.getSource())))
                    .then(buildMoveCommand("move"))
                    .then(buildMoveCommand("moveaction"))
                )

                .then(ClientCommands.literal("pathdebug")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro pathdebug <status|here|cache|walk|pathto|pathxz|stop>"));
                        return 0;
                    })
                    .then(ClientCommands.literal("block")
                        .then(ClientCommands.argument("dx", IntegerArgumentType.integer())
                            .then(ClientCommands.argument("dy", IntegerArgumentType.integer())
                                .then(ClientCommands.argument("dz", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        Minecraft mc = Minecraft.getInstance();
                                        if (mc.player == null) return 0;
                                        int x = mc.player.blockPosition().getX() + IntegerArgumentType.getInteger(ctx, "dx");
                                        int y = mc.player.blockPosition().getY() + IntegerArgumentType.getInteger(ctx, "dy");
                                        int z = mc.player.blockPosition().getZ() + IntegerArgumentType.getInteger(ctx, "dz");
                                        com.example.macromod.path.cache.PathingBlockType type =
                                            PathingBehavior.get().isPathing()
                                                ? null : null;
                                        var packed = com.example.macromod.path.cache.CachedWorld.get().getChunk(x >> 4, z >> 4);
                                        if (packed == null || !packed.isLoaded()) {
                                            ctx.getSource().sendFeedback(Component.literal(
                                                "§6[Path] §cChunk not packed yet"));
                                            return 0;
                                        }
                                        var t = packed.get(x & 15, y, z & 15);
                                        var state = mc.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                                        ctx.getSource().sendFeedback(Component.literal(
                                            "§6[Path] §fBlock §e" + x + " " + y + " " + z + "§7: §e" + t
                                            + " §7(§f" + state.getBlock() + "§7)"));
                                        return 1;
                                    })))))
                    .then(ClientCommands.literal("status")
                        .executes(ctx -> {
                            var pb = PathingBehavior.get();
                            ctx.getSource().sendFeedback(Component.literal(
                                "§6[Path] §fPathing: §e" + pb.isPathing()
                                + " §7| Calculating: §e" + pb.isCalculating()
                                + " §7| Goal: §e" + (pb.getGoal() != null
                                    ? pb.getGoal().getClass().getSimpleName() : "none")
                            ));
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("here")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player == null) return 0;
                            ctx.getSource().sendFeedback(Component.literal(String.format(
                                "§6[Path] §fPos: §e%.3f %.3f %.3f §7| Yaw/Pitch: §e%.1f / %.1f",
                                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                mc.player.getYRot(), mc.player.getXRot()
                            )));
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("cache")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player == null) return 0;
                            int cx = (int) Math.floor(mc.player.getX()) >> 4;
                            int cz = (int) Math.floor(mc.player.getZ()) >> 4;
                            CachedChunk c = CachedWorld.get().getChunk(cx, cz);
                            if (c == null) {
                                ctx.getSource().sendFeedback(Component.literal("§cCurrent chunk not cached yet."));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(Component.literal(
                                "§6[Path] §fChunk §e(" + cx + ", " + cz + ") §7Loaded: §e" + c.isLoaded()
                            ));
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("walk")
                        .then(ClientCommands.argument("x", DoubleArgumentType.doubleArg())
                            .then(ClientCommands.argument("y", DoubleArgumentType.doubleArg())
                                .then(ClientCommands.argument("z", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> pathTo(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "x"),
                                        DoubleArgumentType.getDouble(ctx, "y"),
                                        DoubleArgumentType.getDouble(ctx, "z")
                                    ))
                                )
                            )
                        )
                    )
                    .then(ClientCommands.literal("pathto")
                        .then(ClientCommands.argument("x", DoubleArgumentType.doubleArg())
                            .then(ClientCommands.argument("y", DoubleArgumentType.doubleArg())
                                .then(ClientCommands.argument("z", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> pathTo(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "x"),
                                        DoubleArgumentType.getDouble(ctx, "y"),
                                        DoubleArgumentType.getDouble(ctx, "z")
                                    ))
                                )
                            )
                        )
                    )
                    .then(ClientCommands.literal("pathxz")
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                    PathingBehavior.get().setGoal(new GoalXZ(x, z));
                                    ctx.getSource().sendFeedback(Component.literal(
                                        "§6[Path] §aPathing to XZ §e" + x + ", " + z
                                    ));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(ClientCommands.literal("stop")
                        .executes(ctx -> {
                            PathingBehavior.get().stop();
                            ctx.getSource().sendFeedback(Component.literal("§6[Path] §aStopped."));
                            return 1;
                        })
                    )
                )

                .then(ClientCommands.literal("attack")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro attack <hostile|passive|mobs|mob_name...>"));
                        return 0;
                    })
                    .then(ClientCommands.literal("hostile")
                        .executes(ctx -> startAttack(ctx.getSource(), true, List.of())))
                    .then(ClientCommands.literal("passive")
                        .executes(ctx -> startAttack(ctx.getSource(), false, List.of(TargetRegistry.PASSIVE_TOKEN))))
                    .then(ClientCommands.literal("mobs")
                        .executes(ctx -> startAttack(ctx.getSource(), false, List.of())))
                    .then(ClientCommands.literal("set")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro attack set <attackmode|spaminterval> <value>"));
                            return 0;
                        })
                        .then(ClientCommands.literal("attackmode")
                            .then(ClientCommands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("spam");
                                    b.suggest("crit");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setAttackMode(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "mode")))))
                        .then(ClientCommands.literal("spaminterval")
                            .then(ClientCommands.argument("ms", IntegerArgumentType.integer(50))
                                .executes(ctx -> setSpamInterval(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "ms")))))
                    )
                    .then(ClientCommands.argument("mobs", StringArgumentType.greedyString())
                        .suggests(mobNameSuggestions())
                        .executes(ctx -> startAttack(ctx.getSource(), false,
                            splitNames(StringArgumentType.getString(ctx, "mobs")))))
                )

                .then(ClientCommands.literal("kill")
                )

                .then(ClientCommands.literal("follow")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§cUsage: /macro follow <mob_name|player_name>"));
                        return 0;
                    })
                    .then(ClientCommands.argument("target", StringArgumentType.greedyString())
                        .suggests(followTargetSuggestions())
                        .executes(ctx -> startFollow(ctx.getSource(),
                            StringArgumentType.getString(ctx, "target"))))
                )

                .then(ClientCommands.literal("combatstop")
                    .executes(ctx -> {
                        CombatController.stop();
                        FollowController.stop();
                        ctx.getSource().sendFeedback(Component.literal("§aCombat and follow stopped."));
                        return 1;
                    })
                )
            );
        });
    }

    private static int setSmoothLook(FabricClientCommandSource source, boolean enabled) {
        com.example.macromod.path.render.RotationController.setSmoothLookEnabled(enabled);
        source.sendFeedback(Component.literal("§aSmooth looking: §f" + (enabled ? "on" : "off")));
        return 1;
    }

    private static int setBotView(FabricClientCommandSource source, boolean enabled) {
        com.example.macromod.path.render.RotationController.setBotViewEnabled(enabled);
        source.sendFeedback(Component.literal("§aServer view body: §f" + (enabled ? "on" : "off")));
        return 1;
    }

    private static int pathTo(FabricClientCommandSource source, double x, double y, double z) {
        PathingBehavior.get().setGoal(new GoalBlock(x, y, z));
        source.sendFeedback(Component.literal(
            "§6[Path] §aPathing to §e" + String.format("%.3f", x) + " " + String.format("%.3f", y) + " " + String.format("%.3f", z)
        ));
        return 1;
    }

    private static int startAttack(FabricClientCommandSource source, boolean hostile, List<String> filters) {
        if (Minecraft.getInstance().player == null) {
            source.sendError(Component.literal("§cNot in a world."));
            return 0;
        }
        FollowController.stop();
        CombatController.start(hostile, filters);
        String targetDesc = hostile ? "hostile mobs"
            : filters.isEmpty() ? "all mobs"
            : filters.equals(List.of(TargetRegistry.PASSIVE_TOKEN)) ? "passive mobs"
            : String.join(", ", filters);
        source.sendFeedback(Component.literal("§aAttacking §f" + targetDesc
            + " §7(" + CombatController.getMode() + " mode, "
            + CombatController.getSpamInterval() + "ms interval)"));
        return 1;
    }

    private static int setAttackMode(FabricClientCommandSource source, String modeName) {
        String lower = modeName.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "spam" -> {
                CombatController.setMode(CombatController.AttackMode.SPAM);
                source.sendFeedback(Component.literal("§aAttack mode: §fspam"));
                return 1;
            }
            case "crit" -> {
                CombatController.setMode(CombatController.AttackMode.CRIT);
                source.sendFeedback(Component.literal("§aAttack mode: §fcrit"));
                return 1;
            }
            default -> {
                source.sendError(Component.literal("§cUnknown attack mode: " + modeName + " (use spam or crit)"));
                return 0;
            }
        }
    }

    private static int setSpamInterval(FabricClientCommandSource source, int ms) {
        CombatController.setSpamInterval(ms);
        source.sendFeedback(Component.literal("§aSpam interval: §f" + CombatController.getSpamInterval() + "ms"));
        return 1;
    }

    private static int startFollow(FabricClientCommandSource source, String target) {
        if (Minecraft.getInstance().player == null) {
            source.sendError(Component.literal("§cNot in a world."));
            return 0;
        }
        CombatController.stop();
        FollowController.start(target);
        source.sendFeedback(Component.literal("§aFollowing §f" + target));
        return 1;
    }

    private static List<String> splitNames(String input) {
        List<String> names = new ArrayList<>();
        for (String part : input.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!part.isBlank()) names.add(part);
        }
        return names;
    }

    private static SuggestionProvider<FabricClientCommandSource> mobNameSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            java.util.Set<String> seen = new java.util.HashSet<>();

            for (String literal : List.of("hostile", "passive", "mobs")) {
                if (literal.startsWith(remaining)) {
                    builder.suggest(literal);
                    seen.add(literal);
                }
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.player != null) {
                for (var e : client.level.entitiesForRendering()) {
                    if (!(e instanceof net.minecraft.world.entity.LivingEntity)) continue;
                    if (e.getId() == client.player.getId()) continue;
                    String name = com.example.macromod.TargetRegistry.shortName(e.getType());
                    if (seen.add(name) && name.startsWith(remaining)) builder.suggest(name);
                }
            }

            for (String name : com.example.macromod.TargetRegistry.mobNames()) {
                if (seen.add(name) && name.startsWith(remaining)) builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> followTargetSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            java.util.Set<String> seen = new java.util.HashSet<>();
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.player != null) {
                for (var e : client.level.entitiesForRendering()) {
                    if (e.getId() == client.player.getId()) continue;
                    String name = e.hasCustomName()
                        ? e.getCustomName().getString().toLowerCase(Locale.ROOT)
                        : com.example.macromod.TargetRegistry.shortName(e.getType());
                    if (seen.add(name) && name.startsWith(remaining)) builder.suggest(name);
                }
            }
            for (String name : com.example.macromod.TargetRegistry.mobNames()) {
                if (seen.add(name) && name.startsWith(remaining)) builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> buildMoveCommand(String name) {
        return ClientCommands.literal(name)
            .then(ClientCommands.argument("from", IntegerArgumentType.integer(1))
                .suggests(lineNumberSuggestions())
                .then(ClientCommands.argument("to", IntegerArgumentType.integer(1))
                    .suggests(lineNumberSuggestions())
                    .executes(ctx -> moveAction(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "from"),
                        IntegerArgumentType.getInteger(ctx, "to")))));
    }

    private static SuggestionProvider<FabricClientCommandSource> macroNameSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String name : MacroEditor.listMacroNames()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> lineNumberSuggestions() {
        return (ctx, builder) -> {
            if (!MacroEditor.isEditing()) return builder.buildFuture();
            int count = MacroEditor.lineCount();
            for (int i = 1; i <= count; i++) builder.suggest(String.valueOf(i));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> actionSuggestions() {
        final String[] names = {
            "autogotohere", "autolookhere",
            "gotohere", "lookhere",
            "smoothlookat", "smoothlookhere",
            "autogoto", "autolook",
            "snapgotohere", "snaplookhere",
            "snapgoto", "snaplook",
            "goto", "walk", "walkto", "pathfind", "go",
            "tp", "teleport",
            "pathstop",
            "hold", "release", "press", "click",
            "chat", "cmd",
            "wait", "loop", "endloop",
            "bind", "cycle",
            "attack", "kill", "follow",
            "combatstop", "stopcombat", "stopattack", "unfollow",
            "nolookgoto", "nolookautogoto"
        };
        return (ctx, builder) -> {
            String remaining = builder.getRemaining();
            if (!remaining.contains(" ")) {
                String lower = remaining.toLowerCase(Locale.ROOT);
                for (String n : names) {
                    if (n.startsWith(lower)) builder.suggest(n);
                }
            }
            return builder.buildFuture();
        };
    }

    private static int showHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("§6§lMacroMod §r§7commands:"));
        source.sendFeedback(Component.literal("§f/macro load <name> §7— run a saved macro"));
        source.sendFeedback(Component.literal("§f/macro stop §7— stop the running macro"));
        source.sendFeedback(Component.literal("§f/macro create <name> §7— create a new empty macro"));
        source.sendFeedback(Component.literal("§f/macro edit <name> §7— enter edit mode"));
        source.sendFeedback(Component.literal("§f/macro exit §7— leave edit mode"));
        source.sendFeedback(Component.literal("§f/macro save §7— save the current macro"));
        source.sendFeedback(Component.literal("§f/macro action add <action> §7— add an action"));
        source.sendFeedback(Component.literal("§f/macro action remove <n> §7— remove action N"));
        source.sendFeedback(Component.literal("§f/macro action list §7— list all actions"));
        source.sendFeedback(Component.literal("§f/macro action move <from> <to> §7— move an action"));
        source.sendFeedback(Component.literal("§f/macro attack <hostile|passive|mobs|names> §7— hunt and attack mobs"));
        source.sendFeedback(Component.literal("§f/macro attack set attackmode <spam|crit> §7— choose attack style"));
        source.sendFeedback(Component.literal("§f/macro attack set spaminterval <ms> §7— set hit interval"));
        source.sendFeedback(Component.literal("§f/macro follow <name> §7— follow a mob or player"));
        source.sendFeedback(Component.literal("§f/macro combatstop §7— stop attack/follow routines"));
        source.sendFeedback(Component.literal("§f/macro set smoothlook <true|false> §7— smooth all head turning"));
        source.sendFeedback(Component.literal("§f/macro set botview <true|false> §7— point the body where the server sees it"));
        source.sendFeedback(Component.literal("§f/macro pathdebug walk <x> <y> <z> §7— pathfind to a coord"));
        source.sendFeedback(Component.literal("§f/macro pathdebug pathxz <x> <z> §7— pathfind to XZ"));
        source.sendFeedback(Component.literal("§f/macro pathdebug stop §7— stop pathing"));
        source.sendFeedback(Component.literal("§f/macro pathdebug status|here|cache §7— debug info"));
        if (MacroEditor.isEditing()) {
            source.sendFeedback(Component.literal("§7Currently editing: §f" + MacroEditor.getCurrentName() + ".macro"));
        }
        List<String> macros = MacroEditor.listMacroNames();
        if (!macros.isEmpty()) {
            source.sendFeedback(Component.literal("§7Macros: §f" + String.join("§7, §f", macros)));
        }
        return 1;
    }

    private static int runMacro(FabricClientCommandSource source, String filename) {
        Path macrosDir = MacroEditor.getMacrosDir();
        Path file = macrosDir.resolve(filename);
        if (!Files.exists(file) && !filename.endsWith(".macro")) {
            file = macrosDir.resolve(filename + ".macro");
        }
        if (!Files.exists(file)) {
            source.sendError(Component.literal("§cMacro file not found: " + file));
            return 0;
        }
        try {
            List<MacroAction> actions = MacroParser.parse(file);
            if (actions.isEmpty()) {
                source.sendError(Component.literal("§cMacro file is empty."));
                return 0;
            }
            MacroExecutor.start(actions);
            source.sendFeedback(Component.literal("§aRunning macro: §f" + file.getFileName()
                    + " §7(" + actions.size() + " actions)"));
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("§cFailed to parse macro: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int createMacro(FabricClientCommandSource source, String name) {
        String normalized = normalizeName(name);
        if (MacroEditor.listMacroNames().contains(normalized)) {
            source.sendError(Component.literal("§cMacro already exists: §f" + normalized + ".macro"));
            source.sendError(Component.literal("§7Use §f/macro edit " + normalized + " §7to edit it."));
            return 0;
        }
        if (!MacroEditor.create(normalized)) {
            source.sendError(Component.literal("§cCould not create macro: " + normalized));
            return 0;
        }
        source.sendFeedback(Component.literal("§aCreated §f" + normalized + ".macro §aand entered edit mode."));
        source.sendFeedback(Component.literal("§7Add actions with §f/macro action add <action>"));
        return 1;
    }

    private static int editMacro(FabricClientCommandSource source, String name) {
        String normalized = normalizeName(name);
        if (!MacroEditor.open(normalized)) {
            source.sendError(Component.literal("§cMacro not found: §f" + normalized + ".macro"));
            source.sendError(Component.literal("§7Create it with §f/macro create " + normalized));
            return 0;
        }
        source.sendFeedback(Component.literal("§aEditing §f" + normalized + ".macro §7("
                + MacroEditor.lineCount() + " lines)"));
        source.sendFeedback(Component.literal("§7Use §f/macro action list §7to see the current actions."));
        return 1;
    }

    private static int exitEdit(FabricClientCommandSource source) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro."));
            return 0;
        }
        String name = MacroEditor.getCurrentName();
        MacroEditor.save();
        MacroEditor.close();
        source.sendFeedback(Component.literal("§aExited edit mode for §f" + name + ".macro§a."));
        return 1;
    }

    private static int saveEdit(FabricClientCommandSource source) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro."));
            return 0;
        }
        if (MacroEditor.save()) {
            source.sendFeedback(Component.literal("§aSaved §f" + MacroEditor.getCurrentName() + ".macro§a."));
            return 1;
        }
        source.sendError(Component.literal("§cFailed to save."));
        return 0;
    }

    private static int addAction(FabricClientCommandSource source, String text) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro. Use §f/macro edit <name>§c first."));
            return 0;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            String translated = translateShortcut(text, client);
            MacroEditor.addLine(translated);
            source.sendFeedback(Component.literal("§aAdded: §f" + translated));
            source.sendFeedback(Component.literal("§7Line " + MacroEditor.lineCount() + " of §f"
                    + MacroEditor.getCurrentName() + ".macro"));
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("§cCould not add action: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeAction(FabricClientCommandSource source, int index) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro."));
            return 0;
        }
        if (!MacroEditor.removeLine(index)) {
            source.sendError(Component.literal("§cNo action at line " + index + "."));
            return 0;
        }
        source.sendFeedback(Component.literal("§aRemoved line " + index + "."));
        return 1;
    }

    private static int listActions(FabricClientCommandSource source) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro."));
            return 0;
        }
        List<String> lines = MacroEditor.getLines();
        source.sendFeedback(Component.literal("§6" + MacroEditor.getCurrentName() + ".macro §7(" + lines.size() + " lines)"));
        if (lines.isEmpty()) {
            source.sendFeedback(Component.literal("§7(no actions yet)"));
            return 1;
        }
        for (int i = 0; i < lines.size(); i++) {
            source.sendFeedback(Component.literal("§7" + (i + 1) + ": §f" + lines.get(i)));
        }
        return 1;
    }

    private static int moveAction(FabricClientCommandSource source, int from, int to) {
        if (!MacroEditor.isEditing()) {
            source.sendError(Component.literal("§cNot editing any macro."));
            return 0;
        }
        if (!MacroEditor.moveLine(from, to)) {
            source.sendError(Component.literal("§cInvalid line numbers (from " + from + " to " + to + ")."));
            return 0;
        }
        source.sendFeedback(Component.literal("§aMoved line " + from + " to position " + to + "."));
        return 1;
    }

    private static String translateShortcut(String input, Minecraft client) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Empty action");

        if (trimmed.startsWith("#")) return trimmed;

        String[] parts = trimmed.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String rest = parts.length > 1 ? parts[1] : "";

        if (cmd.equals("auto") || cmd.equals("snap")) {
            String[] sub = rest.split("\\s+", 2);
            if (sub.length == 0 || sub[0].isBlank()) throw new IllegalArgumentException("Need a target after " + cmd);
            String target = sub[0].toLowerCase(Locale.ROOT);
            String targetRest = sub.length > 1 ? sub[1] : "";
            if (target.equals("goto") || target.equals("position") || target.equals("pos")) {
                return "auto goto \"" + resolveCoords(targetRest, client) + "\"";
            }
            if (target.equals("look") || target.equals("direction") || target.equals("dir") || target.equals("at")) {
                return "auto look \"" + resolveDirection(targetRest, client) + "\"";
            }
            throw new IllegalArgumentException("Unknown " + cmd + " target: " + target);
        }

        switch (cmd) {
            case "autogotohere", "snapgotohere":
                return "auto goto \"" + currentPos(client) + "\"";
            case "autolookhere", "snaplookhere":
                return "auto look \"" + currentLook(client) + "\"";
            case "autogoto", "snapgoto", "snap_goto":
                return "auto goto \"" + resolveCoords(rest, client) + "\"";
            case "autolook", "snaplook", "snap_look":
                return "auto look \"" + resolveDirection(rest, client) + "\"";

            case "gotohere", "walkhere", "pathhere":
                return "goto \"" + currentPos(client) + "\"";
            case "lookhere":
                return "look at \"" + currentLook(client) + "\"";

            case "goto", "walk", "walkto", "pathfind", "go":
                return "goto \"" + resolveCoords(rest, client) + "\"";
            case "tp", "teleport":
                return "tp \"" + resolveCoords(rest, client) + "\"";
            case "pathstop", "stopwalking", "stopgoto":
                return "pathstop";

            case "look": {
                String r = rest;
                if (r.toLowerCase(Locale.ROOT).startsWith("at ")) r = r.substring(3);
                return "look at \"" + resolveDirection(r, client) + "\"";
            }
            case "smoothlookat", "smoothlook", "smooth_look", "lookatsmooth": {
                String r = rest;
                if (r.toLowerCase(Locale.ROOT).startsWith("at ")) r = r.substring(3);
                return "smoothlookat \"" + resolveDirection(r, client) + "\"";
            }
            case "smoothlookhere":
                return "smoothlookat \"" + currentLook(client) + "\"";
            case "hold", "release", "press":
                return cmd + " " + requireToken(rest, cmd);
            case "click": {
                String b = requireToken(rest, cmd).toLowerCase(Locale.ROOT);
                if (!b.equals("left") && !b.equals("right") && !b.equals("middle"))
                    throw new IllegalArgumentException("Click button must be left/right/middle");
                return "click " + b;
            }
            case "chat", "say":
                return "chat \"" + stripQuotes(rest) + "\"";
            case "cmd", "command":
                return "cmd \"" + stripQuotes(rest) + "\"";
            case "wait":
                return "wait " + requireToken(rest, cmd);
            case "loop":
                return rest.isBlank() ? "loop" : "loop " + rest.trim();
            case "endloop":
                return "endloop";
            case "bind":
                return "bind " + rest.trim();
            case "cycle":
                return "cycle " + rest.trim();
            case "attack", "kill":
                if (rest.isBlank()) throw new IllegalArgumentException("attack needs hostile, passive, mobs, or mob names");
                return "attack " + rest.trim();
            case "follow":
                if (rest.isBlank()) throw new IllegalArgumentException("follow needs a mob or player name");
                return "follow " + rest.trim();
            case "combatstop", "stopcombat", "stopattack", "unfollow":
                return "combatstop";
            case "nolookgoto", "nolookwalk", "nolookgo":
                return "nolookgoto \"" + resolveCoords(rest, client) + "\"";
            case "nolookautogoto", "nolookautowalk":
                return "nolookautogoto \"" + resolveCoords(rest, client) + "\"";
            case "key_hold":
                return "hold " + rest.trim();
            case "key_release":
                return "release " + rest.trim();
            case "key_press":
                return "press " + rest.trim();
            case "mouse_hold":
                return "hold " + rest.trim().toLowerCase(Locale.ROOT);
            case "mouse_release":
                return "release " + rest.trim().toLowerCase(Locale.ROOT);
            case "mouse_click":
                return "click " + rest.trim().toLowerCase(Locale.ROOT);
            default:
                throw new IllegalArgumentException("Unknown action: " + cmd);
        }
    }

    private static String requireToken(String s, String cmd) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(cmd + " needs an argument");
        return trimmed.split("\\s+")[0];
    }

    private static String resolveCoords(String input, Minecraft client) {
        String trimmed = stripQuotes(input).trim();
        if (trimmed.isEmpty()) return currentPos(client);

        String[] parts = trimmed.split("[\\s,]+");
        if (parts.length != 3) throw new IllegalArgumentException("Need 3 coordinates (x y z)");

        if (client.player == null) throw new IllegalStateException("No player loaded");
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        double x = parseCoord(parts[0], px);
        double y = parseCoord(parts[1], py);
        double z = parseCoord(parts[2], pz);
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", x, y, z);
    }

    private static double parseCoord(String s, double fallback) {
        s = s.trim();
        if (s.equals("~")) return fallback;
        if (s.startsWith("~")) return fallback + Double.parseDouble(s.substring(1));
        return Double.parseDouble(s);
    }

    private static String resolveDirection(String input, Minecraft client) {
        String trimmed = stripQuotes(input).trim();
        if (trimmed.isEmpty()) return currentLook(client);

        String[] nums;
        if (trimmed.contains("/")) {
            nums = trimmed.split("\\s*/\\s*");
        } else {
            nums = trimmed.split("\\s+");
        }
        if (nums.length != 2) throw new IllegalArgumentException("Need yaw / pitch");

        if (client.player == null) throw new IllegalStateException("No player loaded");
        float fallbackYaw = client.player.getYRot();
        float fallbackPitch = client.player.getXRot();

        float yaw = parseAngle(nums[0], fallbackYaw);
        float pitch = parseAngle(nums[1], fallbackPitch);
        return String.format(Locale.ROOT, "%.1f / %.1f", yaw, pitch);
    }

    private static float parseAngle(String s, float fallback) {
        s = s.trim();
        if (s.equals("~")) return fallback;
        if (s.startsWith("~")) return fallback + Float.parseFloat(s.substring(1));
        return Float.parseFloat(s);
    }

    private static String currentPos(Minecraft client) {
        if (client.player == null) throw new IllegalStateException("No player loaded");
        return String.format(Locale.ROOT, "%.3f %.3f %.3f",
                client.player.getX(), client.player.getY(), client.player.getZ());
    }

    private static String currentLook(Minecraft client) {
        if (client.player == null) throw new IllegalStateException("No player loaded");
        return String.format(Locale.ROOT, "%.1f / %.1f",
                client.player.getYRot(), client.player.getXRot());
    }

    private static String stripQuotes(String s) {
        String trimmed = s.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String normalizeName(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(".macro")) {
            return name.substring(0, name.length() - ".macro".length());
        }
        return name;
    }
}
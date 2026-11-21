package com.dimensaolimbo.travessia;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class Comandos {
    private static final Pattern HEX = Pattern.compile("^#?[0-9a-fA-F]{6}$");

    private static final SuggestionProvider<CommandSourceStack> SUG_ESTILOS = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Estilo.values()).map(e -> e.id), b);

    private Comandos() {}

    public static MutableComponent pref() {
        return Component.literal("Travessia").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("travessia")
                .executes(ctx -> iniciar(ctx.getSource()))
                .then(Commands.literal("estilo")
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .suggests(SUG_ESTILOS)
                                .executes(ctx -> estilo(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))))
                .then(Commands.literal("estilos")
                        .executes(ctx -> listar(ctx.getSource())))
                .then(Commands.literal("cor")
                        .then(Commands.argument("cores", StringArgumentType.greedyString())
                                .executes(ctx -> cor(ctx.getSource(), StringArgumentType.getString(ctx, "cores")))))
                .then(Commands.literal("teleportar")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("alvos", EntityArgument.players())
                                .then(Commands.argument("jogador", EntityArgument.player())
                                        .executes(Comandos::teleportarParaJogador))
                                .then(Commands.argument("posicao", Vec3Argument.vec3())
                                        .executes(Comandos::teleportarParaPosicao))))
                .then(Commands.literal("conceder")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("jogador", EntityArgument.player())
                                .then(Commands.argument("estilo", StringArgumentType.greedyString())
                                        .suggests(SUG_ESTILOS)
                                        .executes(ctx -> conceder(ctx,
                                                StringArgumentType.getString(ctx, "estilo")))))));
    }

    private static int iniciar(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!Canalizacao.iniciar(p, Destino.spawn())) {
            p.displayClientMessage(Component.literal("Você já está canalizando.").withStyle(ChatFormatting.GRAY), true);
            return 0;
        }
        return 1;
    }

    private static int estilo(CommandSourceStack src, String txt) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Estilo e = Estilo.achar(txt);
        if (e == null) {
            src.sendFailure(pref().append(Component.literal("estilo não encontrado. Use /travessia estilos")));
            return 0;
        }
        if (!Dados.liberado(p, e)) {
            src.sendFailure(pref().append(Component.literal("esse estilo é secreto, só a staff libera.")));
            return 0;
        }
        Dados.setEstilo(p, e);
        src.sendSuccess(() -> pref()
                .append(Component.literal("estilo ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(e.nome).withStyle(st -> st.withColor(TextColor.fromRgb(e.cor1))))
                .append(Component.literal(" (" + e.origem + ")").withStyle(ChatFormatting.DARK_GRAY)), false);
        return 1;
    }

    private static int listar(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        MutableComponent lista = pref().append(Component.literal("estilos liberados:").withStyle(ChatFormatting.GRAY));
        for (Estilo e : Estilo.values()) {
            if (!Dados.liberado(p, e)) continue;
            lista.append(Component.literal("\n "))
                    .append(Component.literal(e.nome).withStyle(st -> st.withColor(TextColor.fromRgb(e.cor1))));
        }
        src.sendSuccess(() -> lista, false);
        return 1;
    }

    private static int cor(CommandSourceStack src, String txt) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String[] partes = txt.trim().split("\\s+");
        String primeira = Estilo.norm(partes[0]);
        if (primeira.equals("padrao") || primeira.equals("reset")) {
            Dados.limparCores(p);
            src.sendSuccess(() -> pref().append(Component.literal("cores voltaram ao padrão do estilo")
                    .withStyle(ChatFormatting.GRAY)), false);
            return 1;
        }
        if (partes.length > 2 || !HEX.matcher(partes[0]).matches()
                || (partes.length == 2 && !HEX.matcher(partes[1]).matches())) {
            src.sendFailure(pref().append(Component.literal(
                    "use assim: /travessia cor #ff5ca8 #ffe3f1  (a segunda é opcional; \"padrao\" volta ao normal)")));
            return 0;
        }
        int c1 = hex(partes[0]);
        int c2 = partes.length == 2 ? hex(partes[1]) : misturarComBranco(c1, 0.7);
        Dados.setCores(p, c1, c2);
        src.sendSuccess(() -> pref()
                .append(Component.literal("cor salva: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("#%06x", c1)).withStyle(st -> st.withColor(TextColor.fromRgb(c1))))
                .append(Component.literal(" "))
                .append(Component.literal(String.format("#%06x", c2)).withStyle(st -> st.withColor(TextColor.fromRgb(c2)))),
                false);
        return 1;
    }

    private static int conceder(CommandContext<CommandSourceStack> ctx, String txt) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer alvo = EntityArgument.getPlayer(ctx, "jogador");
        Estilo e = Estilo.achar(txt);
        if (e == null) {
            src.sendFailure(pref().append(Component.literal("estilo não encontrado.")));
            return 0;
        }
        if (!e.secreto) {
            src.sendFailure(pref().append(Component.literal("esse estilo já é livre para todos.")));
            return 0;
        }
        Dados.liberar(alvo, e);
        alvo.sendSystemMessage(Component.literal("Eryndor").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("você recebeu o estilo secreto ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(e.nome).withStyle(st -> st.withColor(TextColor.fromRgb(e.cor1)))));
        src.sendSuccess(() -> pref().append(Component.literal(
                e.nome + " liberado para " + alvo.getGameProfile().getName()).withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int teleportarParaJogador(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> alvos = EntityArgument.getPlayers(ctx, "alvos");
        ServerPlayer dest = EntityArgument.getPlayer(ctx, "jogador");
        return teleportar(ctx.getSource(), alvos, Destino.jogador(dest), dest, dest.getGameProfile().getName());
    }

    private static int teleportarParaPosicao(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> alvos = EntityArgument.getPlayers(ctx, "alvos");
        Vec3 pos = Vec3Argument.getVec3(ctx, "posicao");
        String nome = String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
        Destino d = Destino.posicao(ctx.getSource().getLevel().dimension(), pos);
        return teleportar(ctx.getSource(), alvos, d, null, nome);
    }

    private static int teleportar(CommandSourceStack src, Collection<ServerPlayer> alvos, Destino d,
                                  ServerPlayer destinoJogador, String nome) {
        int ok = 0;
        for (ServerPlayer a : alvos) {
            if (a == destinoJogador) continue; // não faz sentido levar o jogador até ele mesmo
            if (Canalizacao.iniciar(a, d)) ok++;
        }
        if (ok == 0) {
            src.sendFailure(pref().append(Component.literal("ninguém para teleportar (já canalizando ou é o próprio destino).")));
            return 0;
        }
        final int total = ok;
        src.sendSuccess(() -> pref().append(Component.literal(
                total + (total == 1 ? " jogador canalizando" : " jogadores canalizando") + " para " + nome)
                .withStyle(ChatFormatting.GRAY)), true);
        return total;
    }

    private static int hex(String s) {
        return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16);
    }

    private static int misturarComBranco(int rgb, double f) {
        int r = (int) (((rgb >> 16) & 255) + (255 - ((rgb >> 16) & 255)) * f);
        int g = (int) (((rgb >> 8) & 255) + (255 - ((rgb >> 8) & 255)) * f);
        int b = (int) ((rgb & 255) + (255 - (rgb & 255)) * f);
        return (r << 16) | (g << 8) | b;
    }
}

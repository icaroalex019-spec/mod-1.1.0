package com.dimensaolimbo.travessia;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;

public final class Canalizacao {
    /** 100 ticks = 5 segundos. */
    public static final int TICKS = 100;
    private static final int TRAVA = 255;

    private static final Map<UUID, Sessao> ATIVAS = new HashMap<>();

    private static final class Sessao {
        final Vec3 origem;
        final Estilo estilo;
        final Destino destino;
        final int c1;
        final int c2;
        int tick = 0;

        Sessao(Vec3 origem, Estilo estilo, Destino destino, int c1, int c2) {
            this.origem = origem;
            this.estilo = estilo;
            this.destino = destino;
            this.c1 = c1;
            this.c2 = c2;
        }
    }

    private Canalizacao() {}

    /** Com muita gente canalizando junto, diminui as partículas para não pesar. */
    private static float qualidade() {
        int n = ATIVAS.size();
        return n <= 3 ? 1f : (n <= 10 ? 0.5f : 0.25f);
    }

    /** Começa a travessia. Retorna false se o jogador já estiver canalizando. */
    public static boolean iniciar(ServerPlayer p, Destino destino) {
        if (ATIVAS.containsKey(p.getUUID())) return false;

        Estilo e = Dados.estilo(p);
        int c1 = Dados.cor1(p);
        int c2 = Dados.cor2(p);

        p.stopRiding();
        Sessao s = new Sessao(p.position(), e, destino, c1, c2);
        ATIVAS.put(p.getUUID(), s);

        // Trava o movimento: lentidão máxima (velocidade zero). O pulo é segurado no tick.
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TICKS + 40, TRAVA, false, false, false));

        // Título na tela: "Canalizando..." branco e centralizado, subtítulo na cor escolhida.
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, TICKS, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Você será teleportado!").withStyle(st -> st.withColor(TextColor.fromRgb(c1)))));
        p.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("Canalizando...").withStyle(ChatFormatting.WHITE)));

        Efeitos.inicio(p.serverLevel(), s.origem);
        return true;
    }

    public static void tick(ServerPlayer p) {
        if (ATIVAS.isEmpty()) return;
        Sessao s = ATIVAS.get(p.getUUID());
        if (s == null) return;

        if (!p.isAlive()) {
            ATIVAS.remove(p.getUUID());
            soltar(p);
            return;
        }

        // Travado no lugar: se tentar andar ou pular, volta para a origem. Sem cancelar.
        Vec3 pos = p.position();
        double dx = pos.x - s.origem.x;
        double dz = pos.z - s.origem.z;
        double dy = pos.y - s.origem.y;
        if (dx * dx + dz * dz > 0.0004 || Math.abs(dy) > 0.02) {
            p.connection.teleport(s.origem.x, s.origem.y, s.origem.z, 0f, 0f,
                    EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
            p.setDeltaMovement(Vec3.ZERO);
        }

        s.tick++;
        float q = qualidade();
        if (q >= 1f || s.tick % 2 == 0) {
            Efeitos.canalizar(p.serverLevel(), s.origem, s.estilo, s.c1, s.c2, s.tick, TICKS, q);
        }
        if (s.tick >= TICKS) {
            concluir(p, s);
        }
    }

    private static void concluir(ServerPlayer p, Sessao s) {
        ATIVAS.remove(p.getUUID());
        soltar(p);

        float q = qualidade();
        Efeitos.partida(p.serverLevel(), s.origem, s.estilo, s.c1, s.c2, q);

        Destino.Resolvido r = s.destino.resolver(p.server);
        if (r == null) {
            p.displayClientMessage(
                    Component.literal("Travessia: o destino não está disponível.").withStyle(ChatFormatting.RED), true);
            return;
        }

        p.teleportTo(r.nivel, r.pos.x, r.pos.y, r.pos.z, p.getYRot(), p.getXRot());
        Efeitos.chegada(r.nivel, r.pos, s.estilo, s.c1, s.c2, q);

        p.sendSystemMessage(Comandos.pref().append(
                Component.literal("você chegou " + s.destino.descricao).withStyle(ChatFormatting.GRAY)));
    }

    private static void soltar(ServerPlayer p) {
        p.connection.send(new ClientboundClearTitlesPacket(true));
        MobEffectInstance ef = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (ef != null && ef.getAmplifier() == TRAVA) {
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    public static void esquecer(UUID id) {
        ATIVAS.remove(id);
    }
}

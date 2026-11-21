package com.dimensaolimbo.travessia;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Para onde a travessia leva o jogador: spawn, outro jogador ou uma posição. */
final class Destino {
    static final class Resolvido {
        final ServerLevel nivel;
        final Vec3 pos;

        Resolvido(ServerLevel nivel, Vec3 pos) {
            this.nivel = nivel;
            this.pos = pos;
        }
    }

    final String descricao;
    private final UUID jogador;
    private final ResourceKey<Level> dim;
    private final Vec3 pos;

    private Destino(String descricao, UUID jogador, ResourceKey<Level> dim, Vec3 pos) {
        this.descricao = descricao;
        this.jogador = jogador;
        this.dim = dim;
        this.pos = pos;
    }

    static Destino spawn() {
        return new Destino("ao spawn", null, null, null);
    }

    static Destino jogador(ServerPlayer alvo) {
        return new Destino("até " + alvo.getGameProfile().getName(), alvo.getUUID(), null, null);
    }

    static Destino posicao(ResourceKey<Level> dim, Vec3 pos) {
        return new Destino("ao destino", null, dim, pos);
    }

    /** Resolvido só no final da canalização, então segue o jogador-alvo se ele se mexer. */
    Resolvido resolver(MinecraftServer srv) {
        if (jogador != null) {
            ServerPlayer j = srv.getPlayerList().getPlayer(jogador);
            if (j == null) return null;
            return new Resolvido(j.serverLevel(), j.position());
        }
        if (pos != null) {
            ServerLevel lv = srv.getLevel(dim);
            if (lv == null) return null;
            return new Resolvido(lv, pos);
        }
        ServerLevel ow = srv.overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
        return new Resolvido(ow, new Vec3(sp.getX() + 0.5, y, sp.getZ() + 0.5));
    }
}

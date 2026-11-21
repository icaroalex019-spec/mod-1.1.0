package com.dimensaolimbo.travessia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Guarda estilo, cores e estilos secretos no NBT persistente do jogador. */
public final class Dados {
    private static final String RAIZ = "travessia";

    private Dados() {}

    private static CompoundTag raiz(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persist = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persist.contains(RAIZ, Tag.TAG_COMPOUND)) {
            persist.put(RAIZ, new CompoundTag());
        }
        return persist.getCompound(RAIZ);
    }

    public static Estilo estilo(Player p) {
        Estilo e = Estilo.achar(raiz(p).getString("estilo"));
        if (e == null || !liberado(p, e)) return Estilo.PNEUMA;
        return e;
    }

    public static void setEstilo(Player p, Estilo e) {
        raiz(p).putString("estilo", e.id);
    }

    public static int cor1(Player p) {
        CompoundTag r = raiz(p);
        return r.contains("cor1") ? r.getInt("cor1") : estilo(p).cor1;
    }

    public static int cor2(Player p) {
        CompoundTag r = raiz(p);
        return r.contains("cor2") ? r.getInt("cor2") : estilo(p).cor2;
    }

    public static void setCores(Player p, int c1, int c2) {
        CompoundTag r = raiz(p);
        r.putInt("cor1", c1);
        r.putInt("cor2", c2);
    }

    public static void limparCores(Player p) {
        CompoundTag r = raiz(p);
        r.remove("cor1");
        r.remove("cor2");
    }

    public static boolean liberado(Player p, Estilo e) {
        return !e.secreto || raiz(p).getBoolean("lib_" + e.id);
    }

    public static void liberar(Player p, Estilo e) {
        raiz(p).putBoolean("lib_" + e.id, true);
    }

    /** Mantém os dados quando o jogador morre e renasce. */
    public static void copiar(Player antigo, Player novo) {
        CompoundTag old = antigo.getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            novo.getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    old.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }
}

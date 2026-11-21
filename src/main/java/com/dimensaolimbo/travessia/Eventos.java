package com.dimensaolimbo.travessia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Eventos {

    @SubscribeEvent
    public void comandos(RegisterCommandsEvent e) {
        Comandos.registrar(e.getDispatcher());
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp) {
            Canalizacao.tick(sp);
        }
    }

    @SubscribeEvent
    public void saiu(PlayerEvent.PlayerLoggedOutEvent e) {
        Canalizacao.esquecer(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone e) {
        Dados.copiar(e.getOriginal(), e.getEntity());
    }
}

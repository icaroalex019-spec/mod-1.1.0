package com.dimensaolimbo.travessia;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Travessia.ID)
public class Travessia {
    public static final String ID = "travessia";

    public Travessia() {
        MinecraftForge.EVENT_BUS.register(new Eventos());
    }
}

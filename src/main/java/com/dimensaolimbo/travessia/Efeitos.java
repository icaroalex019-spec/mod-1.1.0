package com.dimensaolimbo.travessia;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Partículas e sons de cada estilo. Tudo acontece no servidor. */
final class Efeitos {
    private Efeitos() {}

    private static Vector3f vec(int rgb) {
        return new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
    }

    private static DustColorTransitionOptions dust(int a, int b, float tam) {
        return new DustColorTransitionOptions(vec(a), vec(b), tam);
    }

    /** Uma partícula exata. */
    private static void p(ServerLevel lv, ParticleOptions o, double x, double y, double z) {
        lv.sendParticles(o, x, y, z, 1, 0, 0, 0, 0);
    }

    /** Várias partículas espalhadas numa região (um único pacote, bem barato). */
    private static void area(ServerLevel lv, ParticleOptions o, double x, double y, double z,
                             int n, double dx, double dy, double dz, double vel) {
        if (n < 1) n = 1;
        lv.sendParticles(o, x, y, z, n, dx, dy, dz, vel);
    }

    private static int n(double base, float q) {
        return Math.max(1, (int) Math.round(base * q));
    }

    private static int arcoIris(float h) {
        h = h - (float) Math.floor(h);
        float r = Math.max(0f, Math.min(1f, Math.abs(h * 6f - 3f) - 1f));
        float g = Math.max(0f, Math.min(1f, 2f - Math.abs(h * 6f - 2f)));
        float b = Math.max(0f, Math.min(1f, 2f - Math.abs(h * 6f - 4f)));
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    private static void raioVisual(ServerLevel lv, double x, double y, double z) {
        LightningBolt raio = EntityType.LIGHTNING_BOLT.create(lv);
        if (raio != null) {
            raio.moveTo(x, y, z);
            raio.setVisualOnly(true);
            lv.addFreshEntity(raio);
        }
    }

    static void inicio(ServerLevel lv, Vec3 o) {
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.2f);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /**
     * Chamado a cada tick (ou a cada 2 com muita gente) enquanto o jogador canaliza.
     * q = 1 é o máximo; cai quando há muitos jogadores canalizando ao mesmo tempo.
     */
    static void canalizar(ServerLevel lv, Vec3 o, Estilo e, int c1, int c2, int tick, int total, float q) {
        double t = tick / (double) total;      // 0 -> 1
        double k = 1.0 + 3.0 * t;              // intensidade crescente
        double ang = tick * 0.4;

        // Sons que sobem de tom conforme a canalização avança
        if (tick % 10 == 0) {
            lv.playSound(null, o.x, o.y, o.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.0f, (float) (0.5 + 1.5 * t));
        }
        if (tick % 20 == 0) {
            lv.playSound(null, o.x, o.y, o.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    1.0f, (float) (0.7 + 0.8 * t));
        }

        // Hélice de vários braços, com rastro
        int bracos = Math.max(2, Math.round(5 * q));
        for (int b = 0; b < bracos; b++) {
            for (int j = 0; j < 3; j++) {
                double a = ang - j * 0.28 + b * (Math.PI * 2 / bracos);
                double r = 1.9 - 1.0 * t;
                double y = o.y + ((tick * 0.16 + b * 0.65 - j * 0.12) % 3.4 + 3.4) % 3.4;
                p(lv, dust(c1, c2, 1.7f), o.x + Math.cos(a) * r, y, o.z + Math.sin(a) * r);
            }
        }

        // Anéis no chão que se fecham em direção ao jogador
        for (int ring = 0; ring < 2; ring++) {
            double fase = ((tick + ring * 15) % 30) / 30.0;
            double raio = 3.2 * (1.0 - fase);
            int pts = Math.max(8, Math.round(22 * q));
            for (int i = 0; i < pts; i++) {
                double a = i * (Math.PI * 2 / pts) + ang * 0.1;
                p(lv, dust(c2, c1, 1.3f), o.x + Math.cos(a) * raio, o.y + 0.08, o.z + Math.sin(a) * raio);
            }
        }

        // Coluna de luz que sobe e engrossa
        double h = 2.5 + 10.0 * t;
        area(lv, dust(c1, c2, 2.2f), o.x, o.y + h / 2, o.z, n(10 * k, q) + 2, 0.18, h / 2, 0.18, 0.0);
        area(lv, ParticleTypes.END_ROD, o.x, o.y + h / 2, o.z, n(3 * k, q), 0.12, h / 2, 0.12, 0.03);

        // Poeira subindo em volta
        area(lv, dust(c2, c1, 1.0f), o.x, o.y + 0.2, o.z, n(8 * k, q), 1.6, 0.1, 1.6, 0.0);

        // Nos últimos instantes tudo é sugado para dentro
        if (tick > total - 25) {
            area(lv, ParticleTypes.REVERSE_PORTAL, o.x, o.y + 1.0, o.z, n(45, q), 2.4, 1.4, 2.4, 0.25);
        }

        switch (e) {
            case PNEUMA -> {
                area(lv, ParticleTypes.CLOUD, o.x, o.y + 0.4, o.z, n(10 * k, q), 1.8, 0.6, 1.8, 0.03);
                area(lv, ParticleTypes.END_ROD, o.x, o.y + 1.2, o.z, n(6 * k, q), 1.2, 1.4, 1.2, 0.08);
                area(lv, ParticleTypes.REVERSE_PORTAL, o.x, o.y + 1.0, o.z, n(12, q), 1.5, 1.0, 1.5, 0.2);
            }
            case BIFROST -> {
                // Coluna arco-íris em espiral
                for (int i = 0; i < n(14, q); i++) {
                    int rgb = arcoIris(i / 14f + tick * 0.02f);
                    double a = ang * 0.8 + i * 0.5;
                    p(lv, dust(rgb, rgb, 1.8f), o.x + Math.cos(a) * 0.7, o.y + i * 0.45, o.z + Math.sin(a) * 0.7);
                }
                // Ponte de arco-íris que se abre ao lado
                if (tick % 3 == 0) {
                    int passos = n(36, q);
                    int ate = (int) (passos * Math.min(1.0, t * 1.4));
                    for (int i = 0; i <= ate; i++) {
                        double th = Math.PI * i / passos;
                        int rgb = arcoIris(i / (float) passos);
                        p(lv, dust(rgb, rgb, 1.7f), o.x + Math.cos(th) * 6.0, o.y + Math.sin(th) * 5.0, o.z);
                    }
                }
                area(lv, ParticleTypes.END_ROD, o.x, o.y + 1.5, o.z, n(8 * k, q), 1.0, 2.0, 1.0, 0.1);
            }
            case BOITATA -> {
                // Serpente de fogo enroscada
                for (int i = 0; i < n(12, q); i++) {
                    double a = ang * 1.4 - i * 0.28;
                    double r = 2.0 - 0.6 * t;
                    double y = o.y + 0.2 + i * 0.12 + Math.sin(tick * 0.2 + i) * 0.2;
                    p(lv, i == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                            o.x + Math.cos(a) * r, y, o.z + Math.sin(a) * r);
                }
                area(lv, ParticleTypes.FLAME, o.x, o.y + 0.3, o.z, n(14 * k, q), 2.0, 0.2, 2.0, 0.02);
                area(lv, ParticleTypes.FLAME, o.x, o.y + 1.5, o.z, n(10 * k, q), 0.5, 1.5, 0.5, 0.12);
                area(lv, ParticleTypes.SMALL_FLAME, o.x, o.y + 1.0, o.z, n(10 * k, q), 1.6, 1.2, 1.6, 0.03);
            }
            case KITSUNE -> {
                // Cinco fogos-fátuos azuis em órbita, cada um com rastro
                for (int orb = 0; orb < 5; orb++) {
                    for (int j = 0; j < n(4, q); j++) {
                        double a = ang * (1.0 + orb * 0.12) - j * 0.22 + orb * (Math.PI * 2 / 5);
                        double r = 1.5 + 0.5 * Math.sin(tick * 0.1 + orb);
                        double y = o.y + 0.9 + orb * 0.35 + Math.sin(tick * 0.15 + orb) * 0.3;
                        p(lv, ParticleTypes.SOUL_FIRE_FLAME, o.x + Math.cos(a) * r, y, o.z + Math.sin(a) * r);
                    }
                }
                area(lv, ParticleTypes.SOUL, o.x, o.y + 0.5, o.z, n(5 * k, q), 1.6, 0.4, 1.6, 0.03);
                area(lv, ParticleTypes.GLOW, o.x, o.y + 1.2, o.z, n(5 * k, q), 1.3, 1.2, 1.3, 0.05);
            }
            case RAIO -> {
                area(lv, ParticleTypes.ELECTRIC_SPARK, o.x, o.y + 1.2, o.z, n(18 * k, q), 2.0, 1.6, 2.0, 0.6);
                // Raio em zigue-zague caindo do céu
                int cada = t > 0.6 ? 2 : 4;
                if (tick % cada == 0) {
                    double x = o.x + (lv.random.nextDouble() - 0.5) * 3.0;
                    double z = o.z + (lv.random.nextDouble() - 0.5) * 3.0;
                    for (int i = 0; i < n(18, q); i++) {
                        p(lv, ParticleTypes.ELECTRIC_SPARK,
                                x + (lv.random.nextDouble() - 0.5) * 0.6, o.y + 9.0 - i * 0.5,
                                z + (lv.random.nextDouble() - 0.5) * 0.6);
                    }
                }
                if (t > 0.4 && tick % 30 == 0) {
                    raioVisual(lv, o.x + (lv.random.nextDouble() - 0.5) * 6, o.y, o.z + (lv.random.nextDouble() - 0.5) * 6);
                }
            }
            case NEVOA -> {
                area(lv, ParticleTypes.CLOUD, o.x, o.y + 0.2, o.z, n(22 * k, q), 3.2, 0.3, 3.2, 0.02);
                area(lv, ParticleTypes.GLOW, o.x, o.y + 1.2, o.z, n(10 * k, q), 2.2, 1.6, 2.2, 0.04);
                area(lv, ParticleTypes.SPORE_BLOSSOM_AIR, o.x, o.y + 2.2, o.z, n(12 * k, q), 2.4, 0.8, 2.4, 0.0);
                area(lv, ParticleTypes.ENCHANT, o.x, o.y + 1.5, o.z, n(18 * k, q), 1.6, 1.0, 1.6, 0.6);
            }
            case CHAMA -> {
                for (int arm = 0; arm < n(6, q); arm++) {
                    double a = ang * 1.3 + arm * (Math.PI * 2 / 6);
                    double r = 1.6 - 0.7 * t;
                    double y = o.y + ((tick * 0.2 + arm * 0.5) % 3.2);
                    p(lv, ParticleTypes.FLAME, o.x + Math.cos(a) * r, y, o.z + Math.sin(a) * r);
                }
                area(lv, ParticleTypes.FLAME, o.x, o.y + 2.0, o.z, n(12 * k, q), 0.4, 2.0, 0.4, 0.1);
                area(lv, ParticleTypes.FIREWORK, o.x, o.y + 1.5, o.z, n(8 * k, q), 1.4, 1.4, 1.4, 0.1);
                area(lv, ParticleTypes.LAVA, o.x, o.y + 0.3, o.z, 2, 1.6, 0.1, 1.6, 0.0);
            }
        }
    }

    /** Explosão no lugar de onde o jogador sai. */
    static void partida(ServerLevel lv, Vec3 o, Estilo e, int c1, int c2, float q) {
        explosao(lv, o, e, c1, c2, q);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.8f);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.5f, 0.7f);
        if (e == Estilo.RAIO) raioVisual(lv, o.x, o.y, o.z);
    }

    /** Explosão no destino. */
    static void chegada(ServerLevel lv, Vec3 o, Estilo e, int c1, int c2, float q) {
        explosao(lv, o, e, c1, c2, q);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.2f);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 1.0f);
        lv.playSound(null, o.x, o.y, o.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.5f, 1.0f);
        if (e == Estilo.RAIO) raioVisual(lv, o.x, o.y, o.z);
    }

    private static void explosao(ServerLevel lv, Vec3 o, Estilo e, int c1, int c2, float q) {
        // Clarão e onda de choque
        area(lv, ParticleTypes.FLASH, o.x, o.y + 1.0, o.z, 1, 0, 0, 0, 0);
        area(lv, ParticleTypes.SONIC_BOOM, o.x, o.y + 1.0, o.z, 1, 0, 0, 0, 0);

        // Nuvem colorida explodindo para fora e coluna de luz
        area(lv, dust(c1, c2, 2.4f), o.x, o.y + 1.0, o.z, n(180, q), 0.9, 1.0, 0.9, 0.6);
        area(lv, dust(c2, c1, 1.8f), o.x, o.y + 2.5, o.z, n(90, q), 0.5, 3.5, 0.5, 0.05);
        area(lv, ParticleTypes.END_ROD, o.x, o.y + 1.0, o.z, n(90, q), 0.4, 0.6, 0.4, 0.55);
        area(lv, ParticleTypes.FIREWORK, o.x, o.y + 1.0, o.z, n(40, q), 0.4, 0.6, 0.4, 0.4);

        // Ondas de choque no chão, anel após anel
        for (int rr = 1; rr <= 6; rr++) {
            double raio = rr * 1.1;
            int pts = Math.max(10, Math.round(26 * q));
            for (int i = 0; i < pts; i++) {
                double a = i * (Math.PI * 2 / pts);
                p(lv, dust(c1, c2, 1.8f), o.x + Math.cos(a) * raio, o.y + 0.1, o.z + Math.sin(a) * raio);
            }
        }

        // Toque de cada estilo
        switch (e) {
            case PNEUMA -> area(lv, ParticleTypes.CLOUD, o.x, o.y + 0.8, o.z, n(70, q), 0.6, 0.6, 0.6, 0.5);
            case BIFROST -> {
                for (int i = 0; i < n(40, q); i++) {
                    int rgb = arcoIris(i / 40f);
                    area(lv, dust(rgb, rgb, 2.0f), o.x, o.y + 1.0, o.z, 6, 0.3, 0.3, 0.3, 0.5);
                }
            }
            case BOITATA -> {
                area(lv, ParticleTypes.FLAME, o.x, o.y + 0.8, o.z, n(90, q), 0.4, 0.5, 0.4, 0.45);
                area(lv, ParticleTypes.LAVA, o.x, o.y + 0.8, o.z, n(14, q), 0.8, 0.5, 0.8, 0.0);
            }
            case KITSUNE -> area(lv, ParticleTypes.SOUL_FIRE_FLAME, o.x, o.y + 0.8, o.z, n(80, q), 0.4, 0.5, 0.4, 0.4);
            case RAIO -> area(lv, ParticleTypes.ELECTRIC_SPARK, o.x, o.y + 1.0, o.z, n(100, q), 0.5, 0.8, 0.5, 0.8);
            case NEVOA -> {
                area(lv, ParticleTypes.CLOUD, o.x, o.y + 0.5, o.z, n(70, q), 0.8, 0.3, 0.8, 0.3);
                area(lv, ParticleTypes.GLOW, o.x, o.y + 1.0, o.z, n(50, q), 0.8, 0.8, 0.8, 0.2);
            }
            case CHAMA -> {
                area(lv, ParticleTypes.FLAME, o.x, o.y + 0.8, o.z, n(100, q), 0.4, 0.6, 0.4, 0.5);
                area(lv, ParticleTypes.FIREWORK, o.x, o.y + 1.0, o.z, n(60, q), 0.4, 0.6, 0.4, 0.5);
            }
        }
    }
}

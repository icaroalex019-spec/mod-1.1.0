package com.dimensaolimbo.travessia;

import java.text.Normalizer;
import java.util.Locale;

public enum Estilo {
    PNEUMA("pneuma", "Pneuma", "estilo padrão", 0x9FE8FF, 0xFFFFFF, false),
    BIFROST("bifrost", "Bifröst", "mitologia escandinava", 0x66FFE6, 0xFFF27A, false),
    BOITATA("boitata", "Boitatá", "folclore brasileiro", 0xFF8A1F, 0xFFD23F, false),
    KITSUNE("kitsune", "Kitsune-bi", "mitologia japonesa", 0x5CA8FF, 0xB9E4FF, false),
    RAIO("raio", "Raio do Olimpo", "mitologia grega", 0xFFE97A, 0xFFFFFF, false),
    NEVOA("nevoa", "Névoa de Sídhe", "mitologias celtas", 0x4DFFA6, 0xEAFFF4, false),
    CHAMA("chama", "Chama Dourada", "estilo secreto, só a staff libera", 0xFFC933, 0xFFF1B8, true);

    public final String id;
    public final String nome;
    public final String origem;
    public final int cor1;
    public final int cor2;
    public final boolean secreto;

    Estilo(String id, String nome, String origem, int cor1, int cor2, boolean secreto) {
        this.id = id;
        this.nome = nome;
        this.origem = origem;
        this.cor1 = cor1;
        this.cor2 = cor2;
        this.secreto = secreto;
    }

    public static String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");
    }

    public static Estilo achar(String txt) {
        String n = norm(txt);
        if (n.isEmpty()) return null;
        for (Estilo e : values()) {
            if (e.id.equals(n) || norm(e.nome).equals(n)) return e;
        }
        for (Estilo e : values()) {
            if (norm(e.nome).startsWith(n)) return e;
        }
        return null;
    }
}

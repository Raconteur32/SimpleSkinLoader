package fr.raconteur.simpleskinswapper.gui;

public enum SkinType {
    CLASSIC("classic"),
    SLIM("slim");

    private final String mojangVariant;

    SkinType(String mojangVariant) {
        this.mojangVariant = mojangVariant;
    }

    public String getMojangVariant() {
        return mojangVariant;
    }
}

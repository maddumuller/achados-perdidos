package br.edu.udesc.item.service;

import java.util.UUID;

public final class ItemReference {
    private ItemReference() {}

    // The occurrence API uses UUIDs. Existing item IDs remain numeric.
    public static long numericId(String reference) {
        if (reference.length() == 36) {
            UUID id = UUID.fromString(reference);
            if (id.getMostSignificantBits() != 0 || id.getLeastSignificantBits() <= 0) {
                throw new IllegalArgumentException("Identificador de item inválido");
            }
            return id.getLeastSignificantBits();
        }
        return Long.parseLong(reference);
    }
}

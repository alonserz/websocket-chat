package org.example.messages;

import java.util.UUID;

public class ImageUpload {
    public final UUID uuid;

    public ImageUpload() {
        this.uuid = UUID.randomUUID();
    }
}

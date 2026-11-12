package com.example.elementalmagic;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MagicMod.MODID);

    public static final Supplier<AttachmentType<Integer>> MANA =
            ATTACHMENTS.register("mana", () -> AttachmentType.builder(() -> 100).copyOnDeath().build());

    private ModAttachments() {}
}

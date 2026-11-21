package com.example.elementalmagic.mixin;

import com.example.elementalmagic.magic.SwordSpells;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        // Если время остановлено и это не игрок-владелец — отменяем тик
        if (SwordSpells.TIME_STOPPED) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof Player) || !self.getUUID().equals(SwordSpells.TIME_STOP_OWNER)) {
                ci.cancel();
            }
        }
    }
}

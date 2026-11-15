package com.example.texturemanager.mixin;

import com.example.texturemanager.SelectionPack;
import com.example.texturemanager.TextureManagerMod;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    /** Letztes Element der Liste = hoechste Prioritaet. */
    @Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
    private void texturemanager$append(CallbackInfoReturnable<List<PackResources>> cir) {
        Map<String, String> sel = TextureManagerMod.config().selection;
        if (sel.isEmpty()) return;
        List<PackResources> list = new ArrayList<>(cir.getReturnValue());
        list.add(new SelectionPack((PackRepository) (Object) this, Map.copyOf(sel)));
        cir.setReturnValue(list);
    }
}

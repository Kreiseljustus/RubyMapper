package io.github.justuswalterhelk.rubymapper.mixin;

import io.github.justuswalterhelk.rubymapper.RubyMapper;
import io.github.justuswalterhelk.rubymapper.client.RubyMapperClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerListHeader", at = @At("HEAD"))
    private void onPlayerListHeader(PlayerListHeaderS2CPacket packet, CallbackInfo ci) {
        PlayerListHeaderS2CPacketAccessor accessor = (PlayerListHeaderS2CPacketAccessor) packet;
        Text header = accessor.getHeader();
        Text footer = accessor.getFooter();
    }
}

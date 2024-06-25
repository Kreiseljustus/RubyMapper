package io.github.justuswalterhelk.rubymapper.client;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import io.github.justuswalterhelk.rubymapper.ChunkDataHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBundler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RubyMapperClient implements ClientModInitializer {
    private ChunkPos lastChunkPos;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private boolean shouldMap = false;
    Gson gson = new Gson();
    String postUrl = "http://cloudplusplus.tech/submit";

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
            String serverAdress = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "singleplayer";

            if(serverAdress.equals("rubymc.de")) {
                if(checkCommandExists("spawn")) {
                    shouldMap = true;
                };
            }
        }));

        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            shouldMap = false;
        }));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if(!shouldMap) {return;}
        if (client.world != null && client.player != null) {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            ChunkPos currentChunkPos = new ChunkPos(player.getBlockPos());

            if (lastChunkPos == null || !lastChunkPos.equals(currentChunkPos)) {
                onEnterNewChunk(player, world, currentChunkPos);
                lastChunkPos = currentChunkPos;
            }
        }
    }

    private void onEnterNewChunk(ClientPlayerEntity player, ClientWorld world, ChunkPos chunkPos) {
        //player.sendMessage(Text.of("Entered new chunk: " + chunkPos.toString()), false);

        if(MinecraftClient.getInstance().player != null) {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;

            for(int x = 0; x < 16; x++) {
                for(int z = 0; z < 16; z++) {
                    RegistryEntry<Biome> biome = world.getBiome(chunkPos.getCenterAtY(260));
                    //p.sendMessage(Text.of("Biome is " + biome.getKey().get().getValue().getPath()));
                    Text name = Text.translatable("biome.minecraft." + biome.getKey().get().getValue().getPath());

                    ChunkDataHolder chunkDataHolder = new ChunkDataHolder();
                    chunkDataHolder.chunkX = chunkPos.x + x;
                    chunkDataHolder.chunkZ = chunkPos.z + z;
                    chunkDataHolder.biome = name.getString();

                    //p.sendMessage(Text.of("CHUNK POS IS " + chunkDataHolder.chunkZ + " "+ chunkDataHolder.chunkX));

                    sendPost(chunkDataHolder);
                }
            }
        }
    }

    private void sendPost(ChunkDataHolder data) {
        executorService.submit(() -> {
        HttpPost httpPost = new HttpPost(postUrl);
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            StringEntity postString = new StringEntity(gson.toJson(data), ContentType.APPLICATION_JSON);
            httpPost.setEntity(postString);
            httpPost.setHeader("Content-Type", "application/json");
            httpClient.execute(httpPost).close();
        } catch (UnsupportedEncodingException e) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("Problem occured while parsing biome data"));
        } catch (ClientProtocolException e) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("Wrong protocol oopsies"));
        } catch (IOException e) {
            //MinecraftClient.getInstance().player.sendMessage(Text.of("IOException"));
        }});
    }

    public static boolean checkCommandExists(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (networkHandler != null) {
                CommandDispatcher<CommandSource> dispatcher = networkHandler.getCommandDispatcher();
                CommandNode<CommandSource> commandNode = dispatcher.getRoot().getChild(command);
                if (commandNode != null) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}

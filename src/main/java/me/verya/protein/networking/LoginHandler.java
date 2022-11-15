package me.verya.protein.networking;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import me.verya.protein.GamesHandler.GameLifeTimeHandler;
import me.verya.protein.mixins.ServerLoginNetworkHandlerAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginHandler  {

    private final Map<UUID, Identifier> playersGameLoginBuffer = new HashMap<>();
    private final GameLifeTimeHandler gamesHandler = new GameLifeTimeHandler();
    public LoginHandler()
    {
        ServerLoginConnectionEvents.QUERY_START.register(this::onLogin);
        ServerLoginNetworking.registerGlobalReceiver(Channels.LOGIN_CHANNEL, this::onProxyResponse);
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    }

    public void onLogin(ServerLoginNetworkHandler handler,
                                 MinecraftServer server,
                                 PacketSender sender,
                                 ServerLoginNetworking.LoginSynchronizer synchronizer)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("on Login");
        PacketByteBuf packet = new PacketByteBuf (Unpooled.wrappedBuffer(out.toByteArray()));
        sender.sendPacket(Channels.LOGIN_CHANNEL, packet);
        assert packet.release();
    }

    public void onProxyResponse(MinecraftServer server,
                                     ServerLoginNetworkHandler handler,
                                     boolean understood,
                                     PacketByteBuf buf,
                                     ServerLoginNetworking.LoginSynchronizer synchronizer,
                                     PacketSender responseSender)
    {
        if(!understood)
        {
            handler.disconnect(Text.literal("please setup velocity correctly"));
        }
        //read the answer
        ByteArrayDataInput in = ByteStreams.newDataInput(buf.getWrittenBytes());
        var game = in.readUTF();
        assert buf.release();
        //TODO forward the full game ID
        //check if the server can handle the game requested
        var gameId = new Identifier("skywars", game);
        if(!gamesHandler.isGameSupported(gameId))
        {
            handler.disconnect(Text.literal("game unsupported"));
            return;
        }
        //get the already opened game, end if there is already game waiting for players
        var openedGames = gamesHandler.getWaitingGameSpace(gameId);
        if(openedGames.isEmpty())
        {
            //try to open the game if no one opened, the player should wait where he's actually connected
            var future = gamesHandler.openGame(gameId);
            future.handle((gameSpace, throwable) -> {
                if(throwable != null)
                    handler.disconnect(Text.literal(throwable.getMessage()));
                return null; });
            synchronizer.waitFor(future);
        }
        //memorize where the players want's to be connected
        var accessor = (ServerLoginNetworkHandlerAccessor)handler;
        var gameProfile = accessor.getProfile();
        playersGameLoginBuffer.put(gameProfile.getId(), gameId);
    }

    public void onPlayerJoin(ServerPlayNetworkHandler handler,
                             PacketSender sender,
                             MinecraftServer server)
    {
        var player = handler.getPlayer();
        var playerId = player.getUuid();
        var game = playersGameLoginBuffer.get(playerId);
        server.submit(() -> {
            var result = gamesHandler.tryToJoin(player, game);
            result.sendErrorsTo(player);
        });
    }
}

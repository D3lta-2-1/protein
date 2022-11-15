package me.verya.protein.GamesHandler;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.config.GameConfigs;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.game.manager.ManagedGameSpace;
import xyz.nucleoid.plasmid.game.player.GamePlayerJoiner;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GameLifeTimeHandler {

    GameJoinHandler waitingGameSpaces = new GameJoinHandler();

    public boolean isGameSupported(Identifier gameId)
    {
        return GameConfigs.getKeys().contains(gameId);
    }

    public CompletableFuture<ManagedGameSpace> openGame(Identifier gameId) throws RuntimeException
    {
        var config = GameConfigs.get(gameId);
        if(gameId == null) throw new RuntimeException("game not found");
        return GameSpaceManager.get().open(config);
    }

    public Set<GameSpace> getWaitingGameSpace(Identifier gameId)
    {
        return waitingGameSpaces.get(gameId);
    }

    public GamePlayerJoiner.Results tryToJoin(ServerPlayerEntity player, Identifier gameId) throws RuntimeException
    {
        var gameSpace = getWaitingGameSpace(gameId).stream().findFirst();
        if(gameSpace.isEmpty())
            throw new RuntimeException("no game to join");
        return GamePlayerJoiner.tryJoin(player, gameSpace.get());
    }

}

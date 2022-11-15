package me.verya.protein.GamesHandler;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.event.GameEvents;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.config.GameConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class GameJoinHandler {
    private Map<GameSpace, GameConfig> gameSpacesWaitingForPlayers = new HashMap<>();

    GameJoinHandler()
    {
        GameEvents.OPENED.register(this::onGameOpened);
        GameEvents.START_REQUEST.register(this::onStart);
    }

    private void onGameOpened(GameConfig<?> config, GameSpace gameSpace)
    {
        System.out.println("new game created " + config.source().toString());
        gameSpacesWaitingForPlayers.put(gameSpace, config);
    }

    private GameResult onStart(GameSpace gameSpace, GameResult result)
    {
        System.out.println("a game start : " + gameSpace.getMetadata().sourceConfig().source().toString());
        if(result != null && !result.isOk()) return result;
        gameSpacesWaitingForPlayers.remove(gameSpace);
        return result;
    }

    public Set<GameSpace> get(Identifier gameId)
    {
        Set<GameSpace> suitableSpace = new HashSet<>();
        gameSpacesWaitingForPlayers.forEach((gameSpace, config) ->{
            if(!config.source().equals(gameId)) return;
            suitableSpace.add(gameSpace);
        });
        return suitableSpace;
    }

}

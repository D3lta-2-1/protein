package me.verya.protein;

import me.verya.protein.networking.LoginHandler;
import net.fabricmc.api.ModInitializer;

public class Protein implements ModInitializer {
    public static final String MOD_ID = "protein";
    @Override
    public void onInitialize() {
        var loginHandler = new LoginHandler();
    }
}

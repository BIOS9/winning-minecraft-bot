package ciaassured.yrushwinner;

import ciaassured.yrushwinner.infrastructure.ServiceLifecycle;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class YRushWinnerClient implements ClientModInitializer {
    public static final String MOD_ID = "yrushwinner";

    private static YRushWinnerClient instance;
    private Injector injector;

    public static YRushWinnerClient getInstance() {
        return instance;
    }

    /** Retrieve a service at the Fabric boundary. Prefer constructor injection everywhere else. */
    public <T> T get(Class<T> type) {
        return injector.getInstance(type);
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        injector = Guice.createInjector(new YRushWinnerModule());

        ServiceLifecycle lifecycle = injector.getInstance(ServiceLifecycle.class);
        lifecycle.startAll();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> lifecycle.stopAll());
    }
}

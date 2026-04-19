package ciaassured.yrushwinner;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.input.BotToggleKeybind;
import ciaassured.yrushwinner.input.GotoCommand;
import ciaassured.yrushwinner.network.GameChatListener;
import ciaassured.yrushwinner.navigation.AStarNavigator;
import ciaassured.yrushwinner.navigation.DefaultStepChecker;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.StepChecker;
import ciaassured.yrushwinner.navigation.render.DebugPathRenderer;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class YRushWinnerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Inject a class-specific SLF4J logger into any @InjectLogger Logger field.
        // Equivalent to ILogger<T> in C# — the logger name is the declaring class.
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                Class<?> clazz = type.getRawType();
                while (clazz != null) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.getType() == Logger.class && field.isAnnotationPresent(InjectLogger.class)) {
                            Logger logger = LoggerFactory.getLogger(type.getRawType());
                            String className = type.getRawType().getName();
                            encounter.register((MembersInjector<I>) instance -> {
                                try {
                                    field.setAccessible(true);
                                    field.set(instance, logger);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to inject logger into " + className, e);
                                }
                            });
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        });

        bind(PathRenderer.class).to(DebugPathRenderer.class).in(Scopes.SINGLETON);
        bind(StepChecker.class).to(DefaultStepChecker.class).in(Scopes.SINGLETON);
        bind(Navigator.class).to(AStarNavigator.class).in(Scopes.SINGLETON);

        Multibinder<ManagedService> services = Multibinder.newSetBinder(binder(), ManagedService.class);
        services.addBinding().to(DebugPathRenderer.class);
        services.addBinding().to(BotToggleKeybind.class);
        services.addBinding().to(GotoCommand.class);
        services.addBinding().to(GameChatListener.class);
    }
}

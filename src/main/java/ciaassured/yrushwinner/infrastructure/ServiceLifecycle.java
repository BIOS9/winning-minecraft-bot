package ciaassured.yrushwinner.infrastructure;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.Set;

@Singleton
public class ServiceLifecycle {

    private enum State { CREATED, STARTED, STOPPED }

    @InjectLogger private Logger logger;

    private final Set<ManagedService> services;
    private State state = State.CREATED;

    @Inject
    public ServiceLifecycle(Set<ManagedService> services) {
        this.services = services;
    }

    public void startAll() {
        if (state != State.CREATED) throw new IllegalStateException("Services already started");
        state = State.STARTED;
        logger.info("Starting {} service(s)", services.size());
        services.forEach(ManagedService::start);
    }

    public void stopAll() {
        if (state != State.STARTED) throw new IllegalStateException("Services not started");
        state = State.STOPPED;
        logger.info("Stopping {} service(s)", services.size());
        services.forEach(ManagedService::stop);
    }
}

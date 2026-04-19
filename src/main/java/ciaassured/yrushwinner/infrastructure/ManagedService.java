package ciaassured.yrushwinner.infrastructure;

public interface ManagedService {
    void start();
    default void stop() {}
}

package io.appform.dropwizard.actors.router.tree;

@FunctionalInterface
public interface TriConsumerSupplier<S, R, K, V> {
    public S get(R routingKey, K key, V value);
}
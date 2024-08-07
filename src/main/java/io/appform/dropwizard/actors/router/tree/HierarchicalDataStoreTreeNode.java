package io.appform.dropwizard.actors.router.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;
import io.appform.dropwizard.actors.router.tree.key.HierarchicalRoutingKey;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class HierarchicalDataStoreTreeNode<K, V> {

    @JsonIgnore
    private final int depth;
    @JsonIgnore
    private final K token;

    private V defaultData;
    private Map<K, HierarchicalDataStoreTreeNode<K, V>> children = Maps.newConcurrentMap();


    public HierarchicalDataStoreTreeNode() {
        this.depth = 0;
        this.token = null;
        this.defaultData = null;
    }

    public HierarchicalDataStoreTreeNode(K token) {
        this.depth = 0;
        this.token = token;
        this.defaultData = null;
    }


    @Builder
    public HierarchicalDataStoreTreeNode(final int depth, final K token, final V defaultData) {
        this.depth = depth;
        this.token = token;
        this.defaultData = defaultData;
    }

    void traverse(final Consumer<V> consumer) {
        if (defaultData != null) {
            consumer.accept(defaultData);
        }
        children.forEach((k, kvHierarchicalStoreNode) -> {
            if (kvHierarchicalStoreNode != null) {
                kvHierarchicalStoreNode.traverse(consumer);
            }
        });
    }

    void addChild(final List<K> tokens, final V defaultData) {
        final K key = tokens.get(depth);

        log.debug("depth: {} name: {} key: {} tokens: {} defaultData: {}", depth, token, key, tokens, defaultData);

        if (tokens.size() > depth + 1) {
            children.computeIfAbsent(key, k -> new HierarchicalDataStoreTreeNode<>(depth + 1, tokens.get(depth), null));
            children.get(key).addChild(tokens, defaultData);
        } else {
            if (!children.containsKey(key)) {
                children.put(key, new HierarchicalDataStoreTreeNode<K, V>(depth + 1, tokens.get(depth), defaultData));
            } else {
                if (children.get(key)
                        .getDefaultData() == null) {
                    children.get(key)
                            .setDefaultData(defaultData);
                } else {
                    log.error("Request to overwrite at {} existing defaultData: {} new defaultData {}", tokens, children.get(key)
                            .getDefaultData(), defaultData);
                }
            }
        }
    }

    V findNode(final List<K> tokens) {
        if (tokens.size() == depth) {
            return defaultData;
        }

        if (!children.containsKey(tokens.get(depth))) {
            return defaultData;
        }

        V load = children.get(tokens.get(depth))
                .findNode(tokens);
        return load == null
                ? defaultData
                : load;
    }

    public void add(final HierarchicalRoutingKey<K> routingKey, final V payload) {
        addChild(routingKey.getRoutingKey(), payload);
    }

    public V find(final HierarchicalRoutingKey<K> routingKey) {
        return findNode(routingKey.getRoutingKey());
    }
}
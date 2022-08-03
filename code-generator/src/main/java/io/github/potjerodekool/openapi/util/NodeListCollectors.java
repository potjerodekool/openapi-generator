package io.github.potjerodekool.openapi.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

import java.util.stream.Collector;

public class NodeListCollectors {

    private NodeListCollectors() {
    }

    public static <T extends Node> Collector<T, NodeList<T>, NodeList<T>> collector() {
        return Collector.of(
                NodeListCollectors::supplier,
                NodeListCollectors::accumulator,
                NodeListCollectors::combiner
        );
    }

    public static <T extends Node> NodeList<T> supplier() {
        return new NodeList<>();
    }

    public static <T extends Node> void accumulator(final NodeList<T> first,
                                                    final T second) {
        first.add(second);
    }

    public static <T extends Node> NodeList<T> combiner(final NodeList<T> a,
                                                        final NodeList<T> b) {
        final var nodeList = new NodeList<T>();
        nodeList.addAll(a);
        nodeList.addAll(b);
        return nodeList;
    }

}

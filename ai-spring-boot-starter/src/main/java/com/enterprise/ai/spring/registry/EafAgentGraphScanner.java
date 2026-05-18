package com.enterprise.ai.spring.registry;

import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EafAgentGraphScanner {

    private final ObjectProvider<EafAgentGraph> graphs;
    private final List<EafAgentGraph> staticGraphs;

    public EafAgentGraphScanner(ObjectProvider<EafAgentGraph> graphs) {
        this.graphs = graphs;
        this.staticGraphs = List.of();
    }

    public EafAgentGraphScanner(List<EafAgentGraph> graphs) {
        this.graphs = null;
        this.staticGraphs = graphs == null ? List.of() : List.copyOf(graphs);
    }

    public List<EafAgentGraph> scan() {
        List<EafAgentGraph> out = new ArrayList<>();
        if (graphs != null) {
            graphs.orderedStream().forEach(out::add);
        } else {
            out.addAll(staticGraphs);
        }
        return out.stream()
                .sorted(Comparator.comparing(g -> g.code() == null ? "" : g.code()))
                .toList();
    }
}

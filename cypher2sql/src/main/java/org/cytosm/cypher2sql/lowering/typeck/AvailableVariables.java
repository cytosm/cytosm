package org.cytosm.cypher2sql.lowering.typeck;

import org.cytosm.cypher2sql.lowering.typeck.var.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Store the list of availables variables.
 */
public class AvailableVariables {

    private List<Var> availablesVariables;

    AvailableVariables() {
        availablesVariables = new ArrayList<>();
    }

    AvailableVariables(List<Var> previousVariables) {
        availablesVariables = new ArrayList<>(previousVariables);
    }

    AvailableVariables(AvailableVariables previousVariables) {
        availablesVariables = new ArrayList<>(previousVariables.availablesVariables);
    }

    void extend(List<Var> newVariables) {
        availablesVariables = Stream.concat(availablesVariables.stream()
                .filter(x -> newVariables.stream().allMatch(i -> !i.name.equals(x.name)))
                , newVariables.stream()).collect(Collectors.toList());
    }

    public Optional<Var> get(final String name) {
        return availablesVariables.stream().filter(x -> x.name.equals(name)).findFirst();
    }

    public boolean isEmpty() {
        return availablesVariables.isEmpty();
    }

    void add(final Var variable) {
        this.availablesVariables.add(variable);
    }
}

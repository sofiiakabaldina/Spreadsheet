package spreadsheet.graphs;

import spreadsheet.model.Cell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * Represents the directed graph of the spreadsheet cell dependencies.
 * Cycles are allowed temporarily.
 */
public class DependencyGraph {

    /**
     * Stores the values that depend on the cell accessed.
     * ex: outgoing.get(A) = set of cells that depend on A.
     */
    private final HashMap<Cell, HashSet<Cell>> outgoing = new HashMap<>();

    /**
     * Stores the values that the cell accessed depends on.
     * ex: incoming.get(B) = set of cells that B depends on.
     */
    private final HashMap<Cell, HashSet<Cell>> incoming = new HashMap<>();

    /**
     * Ensures that the cell exists in both maps.
     * Avoids nulls checks.
     *
     * @param c - cell to be registered to the graph
     */
    private void register(Cell c) {
        incoming.putIfAbsent(c, new HashSet<Cell>());
        outgoing.putIfAbsent(c, new HashSet<Cell>());
    }

    /**
     * Removes ALL dependencies pointing to this cell.
     * Called whenever the cell's formula changes.
     *
     * @param cell - cell to be cleared
     */
    public void clearDependencies(Cell cell) {
        register(cell);

        for (Cell dep : incoming.get(cell)) {
            outgoing.get(dep).remove(cell);
        }

        incoming.get(cell).clear();
    }

    /**
     * Adds a dependency edge: from -> to.
     *
     * @param from - dependency
     * @param to - dependent
     */
    public void addDependencies(Cell from, Cell to) {
        register(from);
        register(to);

        outgoing.get(from).add(to);
        incoming.get(to).add(from);
    }

    /**
     * Returns all cells that THIS cell depends on.
     *
     * @param cell - cell we are retrieving dependencies from
     * @return set of dependencies.
     */
    public Set<Cell> getDependencies(Cell cell) {
        register(cell);
        return incoming.get(cell);
    }

    /**
     * Returns all cells that depend on THIS cell.
     * @param cell - cell we are retrieving dependents from
     * @return set of dependents.
     */
    public Set<Cell> getDependents(Cell cell) {
        register(cell);
        return outgoing.get(cell);
    }

    /**
     * Returns every cell that appears in the graph.
     *
     * @return set of all cells.
     */
    public Set<Cell> getAllCells() {
        Set<Cell> all = new HashSet<>();
        all.addAll(outgoing.keySet());
        all.addAll(incoming.keySet());
        return all;
    }
}

package spreadsheet.graphs;

import spreadsheet.model.Cell;
import spreadsheet.model.Spreadsheet;

import javax.swing.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Performs topological sort on the cell dependency graph.
 * Evaluates cells during the sort (post-order) to compute their values.
 * Detects cycles and returns whether the sort was successful (acyclic).
 *
 * @author Jackson Steger
 * @version Winter 2026
 */
public class TopologicalSort {

    // spreadsheet.model.Cell states for topsort
    private static final int unevaluatedCell = 0;
    private static final int evaluatingCell = 1;
    private static final int evaluatedCell = 2;

    private final Spreadsheet mySpreadsheet;
    private final DependencyGraph myDependencyGraph;
    private final HashMap<Cell, Integer> myEvaluateState;
    private boolean myCycleDetected;

    /**
     * Constructs a spreadsheet.graph.TopologicalSort object with references to the spreadsheet and dependency graph.
     *
     * @param theSpreadsheet - the spreadsheet.model.Spreadsheet object containing all cells
     * @param theDependencyGraph - the spreadsheet.graph.DependencyGraph showing cell dependencies
     */
    public TopologicalSort(Spreadsheet theSpreadsheet, DependencyGraph theDependencyGraph) {
        mySpreadsheet = theSpreadsheet;
        myDependencyGraph = theDependencyGraph;
        myEvaluateState = new HashMap<>();
        myCycleDetected = false;
    }

    /**
     * Performs a topological sort (DFS-based) on all cells in the spreadsheet.
     * Evaluates each cell after all its dependencies have been evaluated.
     *
     * @return true if the sort was successful (no cycles), false if a cycle was found
     */
    public boolean topsort() {
        // Initializing all cells as unevaluated
        Set<Cell> allCells = myDependencyGraph.getAllCells();
        for (Cell cell : allCells) {
            myEvaluateState.put(cell, unevaluatedCell);
        }

        myCycleDetected = false;

        // Performs a DFS on all unevaluated cells
        for (Cell cell : allCells) {
            if (myEvaluateState.get(cell) == unevaluatedCell && !myCycleDetected) {
                dfs(cell);
            }
        }

        return !myCycleDetected;
    }

    /**
     * Depth-First Search to evaluate cells and find cycles using recursion.
     * Uses post-order traversal: visit all dependencies first, then evaluate the cell.
     *
     * @param cell - the cell to evaluate
     */
    private void dfs(Cell cell) {
        // If cycle already detected, stop processing
        if (myCycleDetected) {
            return;
        }

        int state = myEvaluateState.getOrDefault(cell, unevaluatedCell);

        // If already evaluated, nothing to do
        if (state == evaluatedCell) {
            return;
        }

        // If currently evaluating, we found a cycle
        if (state == evaluatingCell) {
            JOptionPane.showMessageDialog(null, "Cycle detected in cell's formula." +
                    "\nReverting Changes.");
            myCycleDetected = true;
            return;
        }

        // Mark as evaluating to detect cycles
        myEvaluateState.put(cell, evaluatingCell);

        // Recursively evaluate all cells that this cell depends on
        Set<Cell> dependencies = myDependencyGraph.getDependencies(cell);
        for (Cell dependency : dependencies) {
            dfs(dependency);
            if (myCycleDetected) {
                return;
            }
        }

        // Post-order
        cell.evaluate(mySpreadsheet);
        myEvaluateState.put(cell, evaluatedCell);
    }


    /**
     * Returns true if a cycle was found during the sort.
     *
     * @return true if a cycle exists, false otherwise
     */
    public boolean hasCycle() {
        return myCycleDetected;
    }
}

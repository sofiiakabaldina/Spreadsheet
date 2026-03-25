package spreadsheet.model;

import spreadsheet.graphs.DependencyGraph;
import spreadsheet.expression.ExpressionTree;
import spreadsheet.graphs.SpreadsheetUtils;
import spreadsheet.graphs.TopologicalSort;
import spreadsheet.token.CellToken;
import spreadsheet.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Represents a 2D spreadsheet of spreadsheet.model.Cell objects and coordinates all formula updates,
 * dependency tracking, expression tree construction, and recalculation.
 * <p>
 * The spreadsheet.model.Spreadsheet is responsible for:
 *  - Storing a grid of spreadsheet.model.Cell objects
 *  - Updating a cell's formula and expression tree
 *  - Extracting referenced cells from postfix tokens
 *  - Maintaining the spreadsheet.graph.DependencyGraph (dependency → dependent)
 *  - Performing a topological sort to determine evaluation order
 *  - Recalculating all affected cells in dependency-safe order
 * <p>
 * This class does NOT parse formulas itself; parsing is handled by spreadsheet.graph.SpreadsheetUtils.
 *
 * @author Anthony
 * @version Winter 2026
 */
public class Spreadsheet {

    /**
     * Size of table.
     */
    public static final int ROWS = 30;
    public static final int COLUMNS = 30;

    private final Cell[][] cells;
    private final DependencyGraph graph = new DependencyGraph();

    /**
     * Constructs a new spreadsheet using the default rows and columns
     */
    public Spreadsheet(){
        this(ROWS);
    }
    /**
     * Constructs a new square spreadsheet of the given size.
     * Each cell is initialized with default formula "0" and value 0.
     *
     * @param size - the number of rows and columns in the spreadsheet
     */
    public Spreadsheet(int size) {
        cells = new Cell[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    /**
     * Returns the number of rows in the spreadsheet.
     *
     * @return number of rows
     */
    public int getNumRows() {
        return cells.length;
    }

    /**
     * Returns the number of columns in the spreadsheet.
     *
     * @return number of columns
     */
    public int getNumColumns() {
        return cells[0].length;
    }

    /**
     * Returns the spreadsheet.model.Cell at the location specified by a spreadsheet.token.CellToken.
     *
     * @param token - the parsed cell reference
     * @return the corresponding spreadsheet.model.Cell
     */
    public Cell getCell(CellToken token) {
        return cells[token.getRow()][token.getColumn()];
    }

    /**
     * Returns the integer value stored in the cell at the given row and column.
     *
     * @param row - row index
     * @param column - column index
     * @return the evaluated integer of the cell
     */
    public double getCellValue(int row, int column) {
        return cells[row][column].getValue();
    }

    /**
     * Returns the spreadsheet.model.Cell corresponding to a spreadsheet-style name.
     * Uses spreadsheet.graph.SpreadsheetUtils to parse the name into a spreadsheet.token.CellToken.
     *
     * @param cellName - the string name of the cell
     * @return the corresponding spreadsheet.model.Cell
     */
    public Cell getCell(String cellName) {
        CellToken token = new CellToken();
        SpreadsheetUtils.getCellToken(cellName, 0, token);
        return cells[token.getRow()][token.getColumn()];
    }

    /**
     * Updates the formula of the specified cell, rebuilds its expression tree,
     * updates dependency edges, performs a topological sort, and evaluates all
     * cells in dependency-safe order.
     * <p>
     * This is the core recalculation pipeline:
     *  1. Save the spreadsheet.model.Cell's previous formula, expression tree, and dependencies
     *  2. Store new formula
     *  3. Build expression tree from postfix tokens
     *  4. Extract referenced cells
     *  5. Update spreadsheet.graph.DependencyGraph edges
     *  6. Topologically sort all cells
     *  7. Evaluate cells in sorted order
     * <p>
     *  If cycle detected:
     *  1. Topological sort automatically restores the spreadsheet.model.Cell's value
     *  2. This method will restore the spreadsheet.model.Cell's previous:
     *      - formula string
     *      - spreadsheet.expression.ExpressionTree
     *      - dependency edges in the graph
     * <p>
     * This ensures the spreadsheet remains in a consistent state even
     * when an invalid formula is entered.
     *
     * @param token - the cell being modified
     * @param formula - the raw formula string
     * @param postfix - the postfix token stack produced by spreadsheet.graph.SpreadsheetUtils
     */
    public boolean changeCellFormulaAndRecalculate(CellToken token, String formula, Stack<Token> postfix) {
        Cell cell = getCell(token);

        String oldFormula = cell.getFormula();
        ExpressionTree oldTree = cell.getExpressionTreeCopy();
        Set<Cell> oldDeps = graph.getDependencies(cell);
        Stack<Token> postfixCopy = (Stack<Token>) postfix.clone();

        cell.setFormula(formula);
        cell.buildExpressionTree(postfix);
        List<Cell> refs = extractReferences(postfixCopy);

        graph.clearDependencies(cell);
        for (Cell ref : refs) {
            graph.addDependencies(ref, cell);
        }

        TopologicalSort sort = new TopologicalSort(this, graph);
        boolean isSorted = sort.topsort();

        if (!isSorted) {
            cell.setFormula(oldFormula);
            cell.setExpressionTree(oldTree);
            graph.clearDependencies(cell);
            for  (Cell dep : oldDeps) {
                graph.addDependencies(dep, cell);
            }
            return false;   // cycle detected
        }
        return true;        // success
    }

    /**
     * Prints the evaluated integer values of all cells in row order.
     * Primarily used by driver for testing.
     */
    public void printValues() {
        for (Cell[] cell : cells) {
            for (Cell value : cell) {
                System.out.print(formatValue(value.getValue()));
            }
        }
    }

    /**
     * Formats a double cell value for display: shows up to 3 decimal places,
     * but omits trailing zeros (e.g. 4.0 → "4", 0.714285... → "0.714").
     *
     * @param v - the value to format
     * @return formatted string
     */
    public static String formatValue(double v) {
        // Round to 3 decimal places using half-up rounding
        double rounded = Math.round(v * 1000.0) / 1000.0;
        // If the rounded value is a whole number, show it without any decimal point
        long asLong = (long) rounded;
        if (rounded == asLong) {
            return String.valueOf(asLong);
        }
        // Otherwise format to exactly 3 decimal places, then erase following zeros
        // e.g. 5.89662 -> "5.897", 1.500 -> "1.5", 0.71428... -> "0.714"
        String s = String.format("%.3f", rounded);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /**
     * Prints the formula stored in a specified cell.
     *
     * @param token - the cell to print
     */
    public void printCellFormula(CellToken token) {
        Cell cell = getCell(token);
        System.out.print(cell.getFormula());
    }

    /**
     * Prints the formulas of all cells in row-major order.
     * Mainly used by driver for debugging.
     */
    public void printAllFormulas() {
        for (Cell[] cell : cells) {
            for (Cell value : cell) {
                System.out.print(value.getFormula());
            }
        }
    }

    /**
     * Extracts all referenced cells from a postfix token stack.
     * Only spreadsheet.token.CellToken objects represent cell references; all others are ignored.
     *
     * @param postfix the postfix token sequence for a formula
     * @return a list of all referenced spreadsheet.model.Cell objects
     */
    private List<Cell> extractReferences(Stack<Token> postfix) {
        List<Cell> refs = new ArrayList<>();
        for (Token t : postfix) {
            if (t instanceof CellToken token) {
                int row = token.getRow();
                int column = token.getColumn();
                refs.add(cells[row][column]);
            }
        }
        return refs;
    }

    /**
     * Returns the formula stored in the cell at the given column and row.
     * Bridge method for the GUI
     *
     * @param col column index
     * @param row row index
     * @return the formula string
     */
    public String getFormula(int row, int col) {
        return cells[row][col].getFormula();
    }

    /**
     * Returns the computed value of the cell at a given column and row
     * Bridge method for the GUI
     * @param col column index
     * @param row row index
     * @return the integer value
     */
    public double getValue(int row, int col) {
        return cells[row][col].getValue();
    }

    /**
     * Parses the formula string, builds the expression tree, updates the
     * dependency graph and recalculates all affected cells.
     * Bridge method for the GUI.
     *
     * @param col column index
     * @param row row index
     * @param formula the formula string
     */
    public boolean changeCell(int row, int col, String formula) {
        CellToken token = new CellToken();
        token.setRow(row);
        token.setColumn(col);

        // parse the formula string into a postfix token stack
        Stack<Token> postfix = SpreadsheetUtils.getFormula(formula);

        return changeCellFormulaAndRecalculate(token, formula, postfix);
    }
}

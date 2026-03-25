package spreadsheet.model;

import spreadsheet.expression.ExpressionTree;
import spreadsheet.token.Token;

import java.util.Stack;

/**
 * Represents a single cell in a spreadsheet.
 * Each cell stores:
 *   - its formula as a String (e.g., "5 + B3 * 8")
 *   - its computed integer value
 *   - an spreadsheet.expression.ExpressionTree built from the formula, used to evaluate the cell
 *
 * @author Sofiia Kabaldina
 * @version Winter 2026
 */
public class Cell {

    private String formula;
    private double value;
    private ExpressionTree expressionTree;

    /**
     * Constructs a default spreadsheet.model.Cell with formula "0" and value 0.
     */
    public Cell() {
        this.formula = "";
        this.value = 0;
        this.expressionTree = new ExpressionTree();
    }

    /**
     * Returns the formula string stored in this cell.
     * @return the formula string
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Sets the formula string for this cell.
     * @param formula - the new formula string
     */
    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * Returns the most recently computed integer value of this cell.
     * @return the cell's value
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the computed value of this cell directly.
     * @param value - the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Returns the spreadsheet.expression.ExpressionTree for this cell.
     * @return the expression tree
     */
    public ExpressionTree getExpressionTree() {
        return expressionTree;
    }

    /**
     * Returns a copy of this cell's current expression tree.
     * <p>
     * This method is used before modifying a cell's formula so that the
     * spreadsheet can restore the original tree if a cycle is later detected.
     * The returned spreadsheet.expression.ExpressionTree is independent of the original and can be
     * safely modified without affecting the cell's current state.
     *
     * @return a deep copy of the current spreadsheet.expression.ExpressionTree, or null if no tree exists
     */
    public ExpressionTree getExpressionTreeCopy() {
        return expressionTree == null ? null : expressionTree.copyExpressionTree();
    }

    /**
     * Replaces this cell's current expression tree with the specified tree.
     * <p>
     * This method is used during rollback when a cycle is detected in the
     * dependency graph. The spreadsheet restores the previously saved
     * spreadsheet.expression.ExpressionTree so that the cell's internal representation matches
     * its original formula.
     *
     * @param tree the spreadsheet.expression.ExpressionTree to set back
     */
    public void setExpressionTree(ExpressionTree tree) {
        this.expressionTree = tree;
    }

    /**
     * Builds the spreadsheet.expression.ExpressionTree for this cell from a postfix token stack.
     * !!!Call this after parsing the formula with getFormula() in spreadsheet.model.Spreadsheet.
     *
     * @param tokenStack - a Stack of spreadsheet.token.Token objects in postfix order
     */
    public void buildExpressionTree(Stack<Token> tokenStack) {
        expressionTree = new ExpressionTree();
        expressionTree.buildExpressionTree(tokenStack);
    }

    /**
     * Evaluates this cell's expression tree using current cell values
     * from the spreadsheet and stores the result.
     * !!!Should only be called after all cells this cell depends on
     * have already been evaluated (i.e., after topological sort).
     *
     * @param spreadsheet - the spreadsheet.model.Spreadsheet used to look up referenced cell values
     */
    public void evaluate(Spreadsheet spreadsheet) {
        if (expressionTree == null || expressionTree.isEmpty()) {
            value = 0;
        } else {
            value = expressionTree.evaluate(spreadsheet);
        }
    }

    /**
     * Prints the formula string of this cell.
     */
    public void printFormula() {
        System.out.print(formula);
    }
}
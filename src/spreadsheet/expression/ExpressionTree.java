package spreadsheet.expression;

import spreadsheet.model.Spreadsheet;
import spreadsheet.token.CellToken;
import spreadsheet.token.LiteralToken;
import spreadsheet.token.OperatorToken;
import spreadsheet.token.Token;

import java.util.Stack;

/**
 * An expression tree built from a postfix stack of Tokens.
 * Used to represent and evaluate a cell's formula.
 *
 * The tree is a binary tree (NOT a binary search tree).
 * Operator nodes have two children. Literal/cell nodes are leaves.
 *
 * Example: formula "5 + B3 * 8" becomes postfix "5 B3 8 * +"
 * and produces:
 *        +
 *       / \
 *      5   *
 *         / \
 *        B3  8
 *
 * @author Sofiia Kabaldina
 * @version Winter 2026
 */
public class ExpressionTree {

    private ExpressionTreeNode root;

    /**
     * Constructs an empty spreadsheet.expression.ExpressionTree.
     */
    public ExpressionTree() {
        root = null;
    }

    /**
     * Clears the expression tree.
     */
    public void makeEmpty() {
        root = null;
    }

    /**
     * Returns true if the tree is empty.
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Builds the expression tree from a stack of tokens in postfix order.
     * The stack should have the postfix expression with the first token
     * of the postfix at the BOTTOM and the last at the TOP.
     *
     * Note: param s will be empty after building expression tree.
     *
     * @param s - a Stack of spreadsheet.token.Token objects representing a postfix expression
     */
    public void buildExpressionTree(Stack<Token> s) {
        root = getExpressionTree(s);
        if (!s.isEmpty()) {
            System.out.println("Error in buildExpressionTree: stack not empty after build.");
        }
    }

    /**
     * Recursively builds an expression tree from the postfix stack.
     * Pops a token from the stack:
     *   - If it is a literal or cell token, it becomes a leaf node.
     *   - If it is an operator, recursively build right then left subtrees.
     *
     * @param s - the postfix token stack
     * @return the root of the subtree built from the stack
     */
    private ExpressionTreeNode getExpressionTree(Stack<Token> s) {
        if (s.isEmpty()) {
            return null;
        }

        Token token = s.pop();

        if (token instanceof LiteralToken || token instanceof CellToken) {
            // Literals and cell references are leaf nodes
            return new ExpressionTreeNode(token, null, null);

        } else if (token instanceof OperatorToken) {
            // Operators have a right subtree and a left subtree.
            // Right is built first because the stack is LIFO and postfix
            // pushes the right operand after the left.
            ExpressionTreeNode rightSubtree = getExpressionTree(s);
            ExpressionTreeNode leftSubtree = getExpressionTree(s);
            return new ExpressionTreeNode(token, leftSubtree, rightSubtree);
        }

        // Should never reach here
        System.out.println("Error in getExpressionTree: unknown token type.");
        return null;
    }

    /**
     * Creates and returns a copy of this spreadsheet.expression.ExpressionTree.
     * <p>
     * The returned tree has the same structure and tokens as the original,
     * but consists of entirely new spreadsheet.expression.ExpressionTreeNode objects. This allows
     * the spreadsheet to save the tree before modification and restore it
     * later without risk of shared references.
     *
     * @return a new spreadsheet.expression.ExpressionTree that is a structural copy of this tree
     */
    public ExpressionTree copyExpressionTree() {
        ExpressionTree copy = new ExpressionTree();
        copy.root = copyExpressionTreeNode(this.root);
        return copy;
    }

    /**
     * Recursively creates a copy of the given spreadsheet.expression.ExpressionTreeNode.
     * <p>
     * Tokens are reused because they are immutable, but new node objects
     * are created for the left and right subtrees. This ensures that the
     * copied spreadsheet.expression.ExpressionTree is fully independent of the original.
     *
     * @param node the node to copy
     * @return a deep copy of the node, or null if the node is null
     */
    private ExpressionTreeNode copyExpressionTreeNode(ExpressionTreeNode node) {
        if (node == null) return null;
        return new ExpressionTreeNode(
                node.token,
                copyExpressionTreeNode(node.left),
                copyExpressionTreeNode(node.right)
        );
    }

    /**
     * Evaluates the expression tree and returns the integer result.
     * Uses a post-order traversal: evaluate left subtree, evaluate right subtree,
     * then apply the operator at the current node.
     * spreadsheet.model.Cell tokens are looked up in the spreadsheet to get their current value.
     *
     * @param spreadsheet - the spreadsheet.model.Spreadsheet used to look up cell values
     * @return the integer result of evaluating this expression
     */
    public double evaluate(Spreadsheet spreadsheet) {
        return evaluateNode(root, spreadsheet);
    }

    /**
     * Recursively evaluates a subtree rooted at the given node.
     *
     * @param node - the current node
     * @param spreadsheet - the spreadsheet.model.Spreadsheet used to look up cell values
     * @return the integer result
     */
    private double evaluateNode(ExpressionTreeNode node, Spreadsheet spreadsheet) {
        if (node == null) {
            return 0;
        }

        Token token = node.token;

        if (token instanceof LiteralToken) {
            // Base case: return the literal integer value
            return ((LiteralToken) token).getValue();

        } else if (token instanceof CellToken) {
            // Base case: look up the cell's value in the spreadsheet
            CellToken cellToken = (CellToken) token;
            return spreadsheet.getCellValue(cellToken.getRow(), cellToken.getColumn());

        } else if (token instanceof OperatorToken) {
            // Recursive case: evaluate both subtrees then apply the operator
            double leftValue  = evaluateNode(node.left,  spreadsheet);
            double rightValue = evaluateNode(node.right, spreadsheet);

            switch (((OperatorToken) token).getOperatorToken()) {
                case OperatorToken.Plus:
                    return leftValue + rightValue;
                case OperatorToken.Minus:
                    return leftValue - rightValue;
                case OperatorToken.Mult:
                    return leftValue * rightValue;
                case OperatorToken.Div:
                    if (rightValue == 0) {
                        System.out.println("Error: division by zero.");
                        return 0;
                    }
                    return leftValue / rightValue;
                case OperatorToken.Exp:               // BONUS: exponentiation operator
                    return Math.pow(leftValue,  rightValue);
                default:
                    System.out.println("Error in evaluateNode: unknown operator.");
                    return 0;
            }
        }

        // Should never reach here
        System.out.println("Error in evaluateNode: unknown token type.");
        return 0;
    }

    /**
     * Prints the expression tree in infix notation (for debugging).
     */
    public void printTree() {
        if (root == null) {
            System.out.println("(empty tree)");
        } else {
            System.out.println(printNode(root));
        }
    }

    /**
     * Recursively builds an infix string representation of the subtree.
     *
     * @param node - the current node
     * @return infix string representation
     */
    private String printNode(ExpressionTreeNode node) {
        if (node == null) {
            return "";
        }

        Token token = node.token;

        if (token instanceof LiteralToken) {
            return Spreadsheet.formatValue(((LiteralToken) token).getValue());

        } else if (token instanceof CellToken) {
            return token.toString();

        } else if (token instanceof OperatorToken) {
            String left  = printNode(node.left);
            String right = printNode(node.right);
            return "(" + left + " " + ((OperatorToken) token).getOperatorToken() + " " + right + ")";
        }

        return "?";
    }
}
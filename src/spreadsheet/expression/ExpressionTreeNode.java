package spreadsheet.expression;

import spreadsheet.token.Token;

/**
 * A node in an spreadsheet.expression.ExpressionTree.
 * Each node holds a spreadsheet.token.Token (spreadsheet.token.LiteralToken, spreadsheet.token.CellToken, or spreadsheet.token.OperatorToken)
 * and references to left and right child nodes.
 * Leaf nodes (literals and cell references) have null children.
 * Internal nodes (operators) have two children.
 *
 * @author Sofiia Kabaldina
 * @version Winter 2026
 */
public class ExpressionTreeNode {

    Token token;
    ExpressionTreeNode left;
    ExpressionTreeNode right;

    /**
     * Constructs an spreadsheet.expression.ExpressionTreeNode with the given token and children.
     * @param token - the token stored in this node
     * @param left - the left child (or null if leaf)
     * @param right - the right child (or null if leaf)
     */
    public ExpressionTreeNode(Token token, ExpressionTreeNode left, ExpressionTreeNode right) {
        this.token = token;
        this.left  = left;
        this.right = right;
    }
}
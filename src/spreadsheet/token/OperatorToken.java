package spreadsheet.token;

/**
 * A token representing an operator in a formula.
 * Supported operators: +, -, *, /, (
 *
 * @author Sofiia Kabaldina
 * @version Winter 2026
 */
public class OperatorToken extends Token {

    public static final char Plus = '+';
    public static final char Minus = '-';
    public static final char Mult = '*';
    public static final char Div = '/';
    public static final char Exp = '^'; // BONUS: exponentiation operator
    public static final char LeftParen = '(';

    private final char operatorToken;

    /**
     * Constructs an spreadsheet.token.OperatorToken for the given operator character.
     * @param operatorToken - the operator character (+, -, *, /, or ()
     */
    public OperatorToken(char operatorToken) {
        this.operatorToken = operatorToken;
    }

    /**
     * Returns the operator character.
     * @return the operator character
     */
    public char getOperatorToken() {
        return operatorToken;
    }

    /**
     * Returns the priority of this operator.
     * +, -  -> 0
     * *, /  -> 1
     * (     -> 2
     * ^     -> 3
     * @return the priority
     */
    public int priority() {
        switch (operatorToken) {
            case Plus, Minus:
                return 0;
            case Mult, Div:
                return 1;
            case Exp:     // BONUS: exponentiation operator
                return 2;
            case LeftParen:
                return 3;
            default:
                System.out.println("Error in spreadsheet.token.OperatorToken priority().");
                System.exit(0);
                return -1;
        }
    }

    @Override
    public String toString() {
        return Character.toString(operatorToken);
    }
}
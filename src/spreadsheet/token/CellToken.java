package spreadsheet.token;

/**
 * A token representing a cell reference in a formula (e.g., A3, B12).
 * Column A = 0, B = 1, C = 2, etc.
 * Row is a non-negative integer.
 *
 * @author Sofiia Kabaldina
 * @version Winter 2026
 */
public class CellToken extends Token {

    public static final int BadCell = -1;

    private int column;
    private int row;

    /**
     * Constructs a spreadsheet.token.CellToken with row and column set to BadCell.
     */
    public CellToken() {
        this.column = BadCell;
        this.row = BadCell;
    }

    /**
     * Constructs a spreadsheet.token.CellToken with the given column and row.
     * @param column - the column index (A=0, B=1, ...)
     * @param row - the row index (#-based)
     */
    public CellToken(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public String toString() {
        if (row == BadCell || column == BadCell) {
            return "BadCell";
        }
        // Convert column number back to letter(s), e.g. 0 -> A, 1 -> B
        String colStr = "";
        int col = column;
        int largest = 26;
        int digits = 2;
        while (largest <= col) {
            largest *= 26;
            digits++;
        }
        largest /= 26;
        digits--;
        while (digits > 1) {
            colStr += (char) ((col / largest) - 1 + 'A');
            col = col % largest;
            largest /= 26;
            digits--;
        }
        colStr += (char) (col + 'A');
        return colStr + row;
    }
}

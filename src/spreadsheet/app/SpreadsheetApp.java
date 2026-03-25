package spreadsheet.app;/*
 * Driver program of a spreadsheet application.
 * Text-based user interface.
 *
 * @author Donald Chinn
 */

import spreadsheet.graphs.SpreadsheetUtils;
import spreadsheet.model.Spreadsheet;
import spreadsheet.token.CellToken;
import spreadsheet.token.Token;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;

public class SpreadsheetApp {

    /**
     * Read a string from standard input.
     * All characters up to the first carriage return are read.
     * The return string does not include the carriage return.
     * @return  a line of input from standard input
     */
    public static String readString() {
        BufferedReader inputReader;
        String returnString = "";
        char ch;

        inputReader = new BufferedReader (new InputStreamReader(System.in));

        // read all characters up to a carriage return and append them
        // to the return String
        try {
            returnString = inputReader.readLine();
        }
        catch (IOException e) {
            System.out.println("Error in reading characters in readString.");
        }
        return returnString;
    }

    private static void menuPrintValues(Spreadsheet theSpreadsheet) {
        theSpreadsheet.printValues();
    }

    private static void menuPrintCellFormula(Spreadsheet theSpreadsheet) {
        CellToken cellToken = new CellToken();
        String inputString;

        System.out.println("Enter the cell: ");
        inputString = readString();
        SpreadsheetUtils.getCellToken(inputString, 0, cellToken);

        System.out.println(cellToken);
        System.out.println(": ");

        if ((cellToken.getRow() < 0) ||
            (cellToken.getRow() >= theSpreadsheet.getNumRows()) ||
            (cellToken.getColumn() < 0) ||
            (cellToken.getColumn() >= theSpreadsheet.getNumColumns())) {

            System.out.println("Bad cell.");
            return;
        }

        theSpreadsheet.printCellFormula(cellToken);
        System.out.println();
    }

    private static void menuPrintAllFormulas(Spreadsheet theSpreadsheet) {
        theSpreadsheet.printAllFormulas();
        System.out.println();
    }


    private static void menuChangeCellFormula(Spreadsheet theSpreadsheet) {
        String inputCell;
        String inputFormula;
        CellToken cellToken = new CellToken();
        Stack<Token> expTreeTokenStack;
        // ExpressionTreeToken expTreeToken;

        System.out.println("Enter the cell to change: ");
        inputCell = readString();
        SpreadsheetUtils.getCellToken(inputCell, 0, cellToken);

        // error check to make sure the row and column
        // are within spreadsheet array bounds.
        if ((cellToken.getRow() < 0) || (cellToken.getRow() >= theSpreadsheet.getNumRows()) || (cellToken.getColumn() < 0) || (cellToken.getColumn() >= theSpreadsheet.getNumColumns()) ) {

            System.out.println("Bad cell.");
            return;
        }

        System.out.println("Enter the cell's new formula: ");
        inputFormula = readString();
        expTreeTokenStack = SpreadsheetUtils.getFormula(inputFormula);

        theSpreadsheet.changeCellFormulaAndRecalculate(cellToken, inputFormula, expTreeTokenStack);
        System.out.println();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new SpreadsheetGUI());
    }

}
package spreadsheet.app;

import spreadsheet.graphs.SpreadsheetUtils;
import spreadsheet.model.Spreadsheet;
import spreadsheet.token.Token;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.Stack;

/**
 * spreadsheet.app.SpreadsheetGUI is the graphical user interface for the spreadsheet application.
 * This class extends JFrame and serves as the front end of the program. It shows
 * a grid display of cells with adjustable columns, a formula bar for entering and editing formulas and
 * a menu bar with a new clickable button to create a new spreadsheet and file operations; save, load and exit.
 *
 * @author daniellabirungi
 * @version Winter 2026
 */
public final class SpreadsheetGUI extends JFrame {
    /**
     * The backend spreadsheet model that handles all formula parsing, expression tree
     * construction, dependency graph management, topological sort, and cell evaluation.
     */
    private Spreadsheet spreadsheet;
    /**
     * the grid component that displays the computed values of all the cells.
     */
    private JTable table;
    /**
     * the data model backing the JTable to update the cell display
     */
    private DefaultTableModel tableModel;
    /**
     * The formula input bar at the top of the window that displays the formula
     * that is currently selected.
     */
    private JTextField formulaBar;
    /**
     * A label to the left of the formula bar showing the name of the currently selected cell.
     */
    private JLabel cellLabel;
    /**
     * to stop the TableModeListener from working during refresh table
     */
    private boolean isRefreshing = false;
    /**
     * Cache to preserve cell values between refreshes
     */
    private String [][] cellCache = new String[ROWS][COLS];

    /**
     * the number of rows in the spreadsheet
     * setup in the back end
     */
    private static final int ROWS = Spreadsheet.ROWS;
    /**
     * the number of columns in the spreadsheet
     * set up in the backend
     */
    private static final int COLS = Spreadsheet.COLUMNS;

    /**
     * Constructor to display the spreadsheetGUI
     */
    public SpreadsheetGUI() {
        spreadsheet = new Spreadsheet();
        setupUI();
    }

    /**
     * Assembles and lays out everything in the JFrame. It packs the frame,
     * centers it and sets it to visible.
     */

    private void setupUI() {
        setTitle("TCSS 342 spreadsheet.model.Spreadsheet");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createFormulaBar(), BorderLayout.NORTH);
        add(createTable(), BorderLayout.CENTER);
        createMenuBar();
        // sizes window to fit all components
        pack();
        // lets the user resize manually if needed
        setResizable(true);
        // center on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Creates and attaches the menu bar to the frame
     */
    private void createMenuBar() {
        // Create a menu bar that attaches to the JFrame
        JMenuBar menuBar = new JMenuBar();
        // create a clickable button to create a new spreadsheet
        JMenu newMenu = new JMenu("New ");
        newMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                newSpreadsheet();
            }
        });
        // create the file dropdown menu
        JMenu fileMenu = new JMenu("File");
        // the menu items for file
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem loadItem = new JMenuItem("Load");
        JMenuItem exitItem = new JMenuItem("Exit");

        saveItem.addActionListener(e -> saveToFile());
        loadItem.addActionListener(e -> loadFromFile());
        exitItem.addActionListener(e -> System.exit(0));
        // add save and load to file menu
        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        // separate save and load from exit
        fileMenu.addSeparator();
        // add exit to file menu
        fileMenu.add(exitItem);
        //add  new spreadsheet button
        menuBar.add(newMenu);
        // add file menu to menu bar
        menuBar.add(fileMenu);
        // attach menu bar to the JFrame
        setJMenuBar(menuBar);

    }

    /**
     * Saves the current spreadsheet formulas to a user-selected text file.
     * cells with no formulas are skipped.
     */
    private void saveToFile() {
        // open save dialog so the user can pick where to save the file
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);

        //only proceed if the user clicks save.
        if (result == JFileChooser.APPROVE_OPTION) {
            // get the file path the user chooses
            File file = chooser.getSelectedFile();
            //open file for writing
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // go through every cell in the spreadsheet
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLS; col++) {
                        // get the formula string for this cell from the backend
                        String formula = spreadsheet.getFormula(row, col);
                        // only save the cells with a formula. Empty cells default to zero no need to save them
                        if (formula != null && !formula.isEmpty()) {
                            // convert the column number back to letter
                            String cellName = columnToName(col) + row;
                            //write one line: B3 = A1+B1
                            writer.println(cellName + " = " + formula);
                        }
                    }
                }
                // saved successfully
                JOptionPane.showMessageDialog(this, "File was saved successfully.");
                // something went wrong writing the file and show the error to the user
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Loads spreadsheet formulas from the file the user selects.
     * Opens a dialog so the user can browse and select a previously saved spreadsheet file
     * once the file is chosen it is read and restores each cell's formula by
     * forwarding it to the backend as if the user had typed it manually.
     */
    private void loadFromFile() {
        //Open a dialog box so the user can pick which file to load
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        // only proceed if the user chooses a file
        if (result == JFileChooser.APPROVE_OPTION) {
            // get the file selected
            File file = chooser.getSelectedFile();
            // open the file for reading
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                // go through the file line by line till the end
                while ((line = reader.readLine()) != null) {
                    // remove any extra whitespace from the front and back
                    line = line.trim();
                    // skip blank lines
                    if (line.isEmpty())
                        continue;
                    //split on " = " to get the cell name and cell formula
                    String[] parts = line.split(" = ", 2);
                    //if the line does not contain " = " its not written correctly
                    if (parts.length != 2)
                        continue;

                    String cellName = parts[0].trim();
                    String formula = parts[1].trim();

                    // Extract letters (column) and digits (row)
                    int i = 0;
                    while (i < cellName.length() && Character.isLetter(cellName.charAt(i))) {
                        i++;
                    }

                    String colLetters = cellName.substring(0, i);
                    String rowDigits = cellName.substring(i);

                    int col = nameToColumn(colLetters);
                    int row = Integer.parseInt(rowDigits);

                    //send the formula to the backend it parses the formula builds an expression tree
                    //and updates the dependency graph
                    cellCache[row][col] = formula;
                    spreadsheet.changeCell(row, col, formula);
                }

                // push all newly computed values to the visual grid
                refreshTable();
                // load was successful
                JOptionPane.showMessageDialog(this, "Loaded successfully.");
                // something went wrong trying to read file
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    /**
     * Clears the current spreadsheet and resets everything to blank
     */
    private void newSpreadsheet() {
        // ask the user to confirm before wiping the current data
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to create a new spreadsheet? \nAll " +
                        "unsaved changes will be lost.", "New spreadsheet.model.Spreadsheet",
                JOptionPane.YES_NO_OPTION
        );
        // if the user clicks No do nothing
        if (confirm != JOptionPane.YES_OPTION)
            return;

        // reset to a fresh spreadsheet
        spreadsheet = new Spreadsheet();

        // clear the local cache
        cellCache = new String[ROWS][COLS];

        // reset the visual grid
        isRefreshing = true;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 1; col <= COLS; col++) {
                tableModel.setValueAt("", row, col);
            }
        }
        isRefreshing = false;

        // reset the formula bar and cell label back to default
        formulaBar.setText("");
        cellLabel.setText(columnToName(0) + "1");

        // clear the cell selection
        table.clearSelection();
    }
    /**
     * Creates the formula bar panel displayed at the top of the window
     *
     * @return a panel containing the cell label and the input
     */
    private JPanel createFormulaBar() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // label that shows which cell is currently selected.
        cellLabel = new JLabel("A1");
        cellLabel.setPreferredSize(new Dimension(40, 20));

        // field where the user types or views formula for the selected cell.
        formulaBar = new JTextField();
        // when the user presses enter the formula bar, apply formula to the cell
        formulaBar.addActionListener(e-> applyFormula());

        // mirrors what is typed in the formula bar into the selected cell in real time
        formulaBar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();
                int rowCol = col - 1;

                if (row < 0 || rowCol < 0) return;

                // get whatever is currently typed in the formula bar
                String typed = formulaBar.getText();

                // mirror it into the selected cell visually
                isRefreshing = true;
                tableModel.setValueAt(typed, row, col);
                isRefreshing = false;
            }
        });

        formulaBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                formulaBar.requestFocusInWindow();
            }
        });

        panel.add(cellLabel, BorderLayout.WEST);
        panel.add(formulaBar, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a scrollable JTable to show the spreadsheet grid. The table has
     * columns the first one being for just the row numbers and the A - however many are decided in the backend
     * and rows 0-however many are decided in the backend.
     *
     * @return a JScrollPane with the JTable
     */

    private JScrollPane createTable() {
        // column headers : blank then A,B,C,D,E,F,G,H
        String[] columnHeaders = new String[COLS + 1];
        columnHeaders[0] = ""; // the first one is blank
        for (int col = 0; col < COLS; col++) {
            columnHeaders[col + 1] = columnToName(col);
        }
        // Build the initial empty grid data
        Object[][] data = new Object[ROWS][COLS + 1];
        for (int row = 0; row < ROWS; row++) {
            data[row][0] = String.valueOf(row + 1);
            for (int col = 1; col <= COLS; col++) {
                data[row][col] = "";
            }
        }
        // create a table model then override isCellEditable so row labels cannot be edited.
        tableModel = new DefaultTableModel(data, columnHeaders) {
            @Override
            public boolean isCellEditable(int row, int col) {
                //  block the row number column
                return col != 0 ;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        // narrow row label
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        // makes grid lines visible
        table.setShowGrid(true);
        table.setGridColor(Color.BLACK);

        // set colour for row label column to separate it from the rest of the grid
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                // call the default renderer first to get the base component
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);

                // grey background with centered bold text to look like a header
                c.setBackground(Color.LIGHT_GRAY);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);

                return c;
            }
        });

        // Mouse listener so that clicking a cell updates the formula bar and cell label
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // only works once after the full click
                onCellSelected();
            }
        });
        //Key listener so that navigating with arrow keys also updates the formula bar
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                // works after arrow key navigation is complete
                onCellSelected();
            }
        });

        // Allow the user to drag column borders to resize them
        // Allow the user to drag row borders to resize them
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // default height, user can drag to change
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);

        // Calculate exact size needed for the grid
        int tableWidth = table.getPreferredSize().width;
        int tableHeight = table.getPreferredSize().height + table.getTableHeader().getPreferredSize().height;

        // Set the scroll pane to exactly fit the grid
        scrollPane.setPreferredSize(new Dimension(tableWidth, tableHeight));

        // As the user types directly into a cell, mirror it in the formula bar
        tableModel.addTableModelListener(e -> {
            // ignore refreshTable()
            if (isRefreshing)
                return;

            int row = e.getFirstRow();
            int col = e.getColumn() - 1; // -1 to skip row label column

            if (row < 0 || col < 0)
                return;
            // gets what is typed into the cell
            Object value = tableModel.getValueAt(row, e.getColumn());
            String typed = value != null ? value.toString() : "";

            // mirror it in the formula bar
            formulaBar.setText(typed);

            // update cell label
            String cellName = columnToName(col) + (row + 1);
            cellLabel.setText(cellName);

            // send the value to the backend so it isn't lost on refresh
            if (!typed.isEmpty()) {
                boolean success = spreadsheet.changeCell(row, col, typed);
                if (!success) {
                    // clear cache for this cell
                    cellCache[row][col] = null;
                    // restore formula bar to backend's actual formula
                    String reverted = spreadsheet.getFormula(row, col);
                    formulaBar.setText(reverted == null || reverted.isBlank() ? "" : reverted);
                    refreshTable();
                    return;
                }
                // success: store typed formula
                cellCache[row][col] = typed;
                refreshTable();

                String formula = spreadsheet.getFormula(row, col);
                formulaBar.setText(formula == null || formula.isBlank() ? "" : formula);
            } else {
                // cell was cleared reset to zero
                cellCache[row][col] = null;
                spreadsheet.changeCell(row, col, "");
                formulaBar.setText("");
                refreshTable();
            }

        });
        return scrollPane;
    }

    /**
     * called whenever a user selects a different cell in the table.
     */
    private void onCellSelected() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        //make sure both column and drow are selected before doing anything
        if (row < 0 || col < 0)
            return;
        // subtract 1 to skip the row label column
        int rowCol = col - 1;
        // nothing valid is selected
        if (rowCol < 0)
            return;

        // update the cell label (e.g. "A1")
        String cellName = columnToName(rowCol) + (row + 1);
        cellLabel.setText(cellName);

        // get the formula from your backend and show it in the formula bar
        String formula = spreadsheet.getFormula(row, rowCol);
        // show empty if the cell is empty or zero, otherwise show the formula
        if(formula == null || formula.isEmpty() || formula.equals("0")) {
            formulaBar.setText("");
        } else {
            formulaBar.setText(formula);
        }
    }

    /**
     * Reads the formula from the formula bar and applies it to the currently
     * selected cell.
     */
    private void applyFormula() {
        // get the row and column of the currently selected cell
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        // subtract 1 to skip the row label column so col lines up with the backend
        int rowCol = col - 1;
        // if selected input is valid do nothing
        if (row < 0 || rowCol < 0)
            return;

        // get what is typed in the formula bar and remove extra whitespace
        String formula = formulaBar.getText().trim();

        // if the formula bar is empty, do nothing
        if (formula.isEmpty()) {
            cellCache[row][col] = null;
            spreadsheet.changeCell(row, rowCol, "0");
            refreshTable();
            return;
        }
        // save formula to local cache
        cellCache[row][rowCol] = formula;

        // send formula to your backend spreadsheet
        spreadsheet.changeCell(row, rowCol, formula);

        // sync cache to backend in case of a cycle caused a revert
        String backendFormula = spreadsheet.getFormula(row, rowCol);
        if (backendFormula == null || backendFormula.equals("0")) {
            // cell was blank - clear the cache.
            cellCache[row][col] = null;
        } else if(!backendFormula.equals(formula)) {
            // cell had a previous value - restore the cache to that value.
            cellCache[row][col] = backendFormula;
        }

        // refresh the visual table to show new values
        refreshTable();
        table.requestFocusInWindow();
    }

    /**
     * Updates the JTable display.
     */
    private void refreshTable() {
        // stop TableModeListener from working while we update the grid
        isRefreshing = true;
        // loop through every cell in the spreadsheet
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {

                // get the cells computed integer value from the backend
                double value = spreadsheet.getValue(row, col);
                String formula = spreadsheet.getFormula(row, col);
                // get cached value for this cell
                String cached = cellCache[row][col];

                if (value != 0) {
                    // non-zero result- display the computed value
                    tableModel.setValueAt(Spreadsheet.formatValue(value), row , col + 1);
                } else if (formula != null && !formula.isEmpty() && !formula.equals("0")) {
                    //check if this is a real formula or just plain text
                    Stack<Token> textStack = SpreadsheetUtils.getFormula(formula);
                    if (textStack.isEmpty()){
                        // plain text e.g "word" - display as is
                        tableModel.setValueAt(formula, row , col + 1);
                    } else {
                        // valid formula evaluates to zero - show zero and clear cache
                        tableModel.setValueAt("0", row, col + 1);
                        cellCache[row][col] = null;
                    }
                } else if (cached != null && !cached.isEmpty()) {
                    tableModel.setValueAt(cached, row, col + 1);
                } else {
                    // empty cell
                    tableModel.setValueAt("", row , col + 1);
                }
            }
        }

        // allow tableModeListener to work again
        isRefreshing = false;
        // resize all columns to fit their content after updating values
        autoFitColumns();

    }

    /**
     * helper method to calculate the longest formula in each column and resizes
     * accordingly
     */
    private void autoFitColumns() {
        // loop through every column in the table
        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 0;

            // check the column header width first
            TableColumn column = table.getColumnModel().getColumn(col);
            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComponent = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, 0, col);
            width = headerComponent.getPreferredSize().width;

            // Check every row in this column and track the widest cell
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
                Component cellComponent = table.prepareRenderer(cellRenderer, row, col);
                int cellWidth = cellComponent.getPreferredSize().width;
                width = Math.max(width, cellWidth);
            }
            // only widen the column when needed
            int currentWidth = column.getPreferredWidth();
            if (width + 10 > currentWidth) {
                column.setPreferredWidth(width + 10);
            }
        }

    }


    /**
     * Converts a zero-based column index into an Excel-style column name.
     * For example: 0 → "A", 25 → "Z", 26 → "AA".
     *
     * @param col - zero-based column index
     * @return the corresponding column name
     */
    private String columnToName(int col) {
        StringBuilder sb = new StringBuilder();
        col++; // convert 0-based to 1-based

        while (col > 0) {
            col--; // shift to 0–25
            sb.insert(0, (char)('A' + (col % 26)));
            col /= 26;
        }

        return sb.toString();
    }

    /**
     * Converts an Excel-style column name into a zero-based index.
     * For example: "A" → 0, "Z" → 25, "AA" → 26.
     *
     * @param name column name consisting of one or more letters
     * @return zero-based column index
     */
    private int nameToColumn(String name) {
        int col = 0;
        for (int i = 0; i < name.length(); i++) {
            col = col * 26 + (name.charAt(i) - 'A' + 1);
        }
        return col - 1; // convert back to 0-based
    }

}
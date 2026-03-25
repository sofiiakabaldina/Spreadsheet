# 📊 TCSS 342 spreadsheet.model.Spreadsheet
**Quarter:** Winter 2025  
**Project:** TCSS 342 spreadsheet.model.Spreadsheet

A simplified spreadsheet application built for TCSS 342. It supports formula parsing, expression trees, dependency tracking, cycle detection, and a Swing-based GUI.

---

## 👥 Group Members
- **Anthony Co**
- **Sofiia Kabaldina**
- **Daniella Birungi**
- **Jackson Steger**

---

## ✨ Features
- Integer formulas with operators and cell references (`A1 + B2 * 3`)
- Expression tree evaluation
- Dependency graph with cycle detection and rollback
- Instant recalculation using topological sorting
- Excel-style multi-letter columns (A–Z, AA–AZ, BA–BZ, ...)
- Swing GUI with formula bar, cell label, and editable grid
- Save/load formulas to a simple text format

---

## ⚙️ How It Works
- Formulas are tokenized and converted to postfix.
- A postfix stack builds an expression tree.
- A cloned postfix stack extracts referenced cells for the dependency graph.
- Cycles are detected before committing updates.
- A topological sort determines evaluation order.
- The GUI displays computed values and lets users edit formulas.

---

## 🚀 Running the Program
1. Open the project in IntelliJ or any Java IDE
2. Ensure Java 17+
3. Run `spreadsheet.app.SpreadsheetApp.java`
4. The spreadsheet window will open

---

## 💾 File Format
Saved files contain lines like:
```
A1 = 5 B1 = A1 + 3 AA3 = B1 * 2
```
Only cells with formulas are saved.

---
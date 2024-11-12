# csv-postfix-notation
This Java program processes a CSV file, where each cell may contain a numeric value, a reference to another cell (e.g., "A1"), or a postfix expression (e.g., "3 4 +"). The program evaluates each cell's value, including resolving nested cell references and postfix expressions, and then outputs the evaluated CSV content.

HOW TO RUN:
- Go to the project folder.
- Open the terminal.
- To BUILD this, use this command for Windows: javac com\processcsv*.java
- To RUN this, use this command: java com.processcsv.Main input.csv


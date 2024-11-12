package com.processcsv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Please provide the CSV file path.");
      return;
    }

    String csvFile = args[0];
    List < List < String >> data = readCSV(csvFile);
    List < List < String >> result = new ArrayList < > ();

    // Set to track references currently being evaluated to prevent cyclic dependencies
    Set < String > currentlyEvaluating = new HashSet < > ();
    
    // Set to track references that have already been evaluated
    Set < String > evaluated = new HashSet < > ();

    for (int row = 0; row < data.size(); row++) {
      List < String > resultRow = new ArrayList < > ();
      for (int col = 0; col < data.get(row).size(); col++) {
        String cellValue = data.get(row).get(col);
        System.out.println("Evaluating cell: " + cellValue); 
        
        try {
          double evaluatedValue = evaluateExpression(cellValue, data, currentlyEvaluating, evaluated);
          resultRow.add(String.valueOf(evaluatedValue));
          System.out.println("Evaluated result: " + evaluatedValue); 
        } catch (Exception e) {
          resultRow.add("#ERR");
          System.out.println("Error in evaluation: " + e.getMessage()); 
        }
      }
      result.add(resultRow);
    }

    printCSV(result);
  }

  // Read CSV file and store it in a list of lists
  private static List < List < String >> readCSV(String filePath) {
    List < List < String >> rows = new ArrayList < > ();
    
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        rows.add(Arrays.asList(values));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return rows;
  }

  // Evaluate a postfix expression
  private static double evaluateExpression(String expr, List < List < String >> data, Set < String > currentlyEvaluating, Set < String > evaluated) throws Exception {
    Stack < Double > stack = new Stack < > ();
    String[] tokens = expr.trim().split("\\s+");

    System.out.println("Tokens for expression: " + Arrays.toString(tokens));

    // First, resolve all references in the expression before evaluating the postfix expression
    List < String > resolvedTokens = new ArrayList < > ();
    for (String token: tokens) {
      if (isReference(token)) {
        // Convert the reference to uppercase to ensure consistency
        token = token.toUpperCase();
        System.out.println("Resolving reference: " + token); 
        double value = getCellValue(token, data, currentlyEvaluating, evaluated);
        resolvedTokens.add(String.valueOf(value));
      } else {
        resolvedTokens.add(token);
      }
    }

    System.out.println("Resolved tokens: " + resolvedTokens);

    // Evaluate the postfix expression using stack and resolved tokens
    for (String token: resolvedTokens) {
      if (isNumber(token)) {
        stack.push(Double.parseDouble(token));
      } else if (isOperator(token)) {
        if (stack.size() < 2) throw new Exception("Invalid Expression");
        
        double b = stack.pop();
        double a = stack.pop();
        
        stack.push(applyOperator(a, b, token));
      } else {
        throw new Exception("Invalid token: " + token);
      }
    }

    if (stack.size() != 1) throw new Exception("Invalid Expression");
    return stack.pop();
  }

  // Function to get the value of a cell by reference, handling nested references until resolved
  private static double getCellValue(String reference, List < List < String >> data, Set < String > currentlyEvaluating, Set < String > evaluated) throws Exception {
    reference = reference.toUpperCase();

    if (evaluated.contains(reference)) {
      return fetchValueFromCell(reference, data);
    }

    // Detect cycles in references
    if (currentlyEvaluating.contains(reference)) {
      throw new Exception("Cyclic reference detected: " + reference);
    }

    // Mark this reference as currently being evaluated to prevent cyclic dependencies
    currentlyEvaluating.add(reference);

    // Fetch the row and column based on the reference format (e.g., A1, B2)
    int col = reference.charAt(0) - 'A';
    int row = Integer.parseInt(reference.substring(1)) - 1;

    if (row < 0 || row >= data.size() || col < 0 || col >= data.get(row).size()) {
      throw new Exception("Invalid cell reference: " + reference);
    }

    String cellValue = data.get(row).get(col);
    System.out.println("Fetched value for " + reference + ": " + cellValue); 

    double finalValue;

    // Check if cell value is a numeric value directly
    if (isNumber(cellValue)) {
      finalValue = Double.parseDouble(cellValue);
    }
    
    // If cell contains a postfix expression, evaluate it
    else if (isPostfixExpression(cellValue)) {
      System.out.println("Postfix expression in reference cell: " + cellValue);
      finalValue = evaluateExpression(cellValue, data, currentlyEvaluating, evaluated);
    }
    
    // Handle cases where cell value is another reference (e.g., B2 containing "A1")
    else if (isReference(cellValue)) {
      System.out.println("Cell contains another reference: " + cellValue); 
      // Recursively resolve the reference until we get a final value
      finalValue = getCellValue(cellValue, data, currentlyEvaluating, evaluated);
    }
    
    // If none of the above, it's an invalid cell value
    else {
      throw new Exception("Invalid cell content: " + cellValue);
    }

    // Mark this reference as evaluated and remove it from currently evaluating
    evaluated.add(reference);
    currentlyEvaluating.remove(reference);

    return finalValue;
  }

  // Fetch the value from a cell, handling direct numeric values and postfix expressions
  private static double fetchValueFromCell(String reference, List < List < String >> data) throws Exception {
    int col = reference.charAt(0) - 'A';
    int row = Integer.parseInt(reference.substring(1)) - 1;

    if (row < 0 || row >= data.size() || col < 0 || col >= data.get(row).size()) {
      throw new Exception("Invalid cell reference: " + reference);
    }

    String cellValue = data.get(row).get(col);
    System.out.println("Fetched value for " + reference + ": " + cellValue); 

    // If the cell contains a numeric value, return it directly
    if (isNumber(cellValue)) {
      return Double.parseDouble(cellValue);
    }
    
    // If the cell contains a postfix expression, evaluate it
    if (isPostfixExpression(cellValue)) {
      System.out.println("Postfix expression in reference cell: " + cellValue);
      return evaluateExpression(cellValue, data, new HashSet < > (), new HashSet < > ());
    }

    // If it contains another reference, recursively resolve the value
    if (isReference(cellValue)) {
      System.out.println("Cell contains another reference: " + cellValue); 
      return getCellValue(cellValue, data, new HashSet < > (), new HashSet < > ());
    }

    throw new Exception("Invalid cell content: " + cellValue);
  }

  private static boolean isNumber(String token) {
    try {
      Double.parseDouble(token);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isReference(String token) {
    return token.matches("[a-zA-Z]+[0-9]+"); // Matching references like A1, B1, etc.
  }

  private static boolean isPostfixExpression(String value) {
    // Check if the value contains spaces (indicating a postfix expression)
    return value.contains(" ");
  }

  private static boolean isOperator(String token) {
    return "+-*/".contains(token);
  }

  private static double applyOperator(double a, double b, String operator) throws Exception {
    switch (operator) {
    case "+":
      return a + b;
    case "-":
      return a - b;
    case "*":
      return a * b;
    case "/":
      if (b == 0) throw new Exception("Division by zero");
      return a / b;
    default:
      throw new Exception("Unknown operator");
    }
  }

  private static void printCSV(List < List < String >> data) {
    System.out.println("FINAL OUTPUT:");
    for (List < String > row: data) {
      System.out.println(String.join(",", row));
    }
  }
}
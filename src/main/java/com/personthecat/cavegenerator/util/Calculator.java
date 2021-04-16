package com.personthecat.cavegenerator.util;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

/** An expression evaluator used for generating numeric field data in presets. */
public class Calculator {

    /** A pattern used for testing whether a string is or is not a supported expression. */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\d+\\.?\\s*\\)*\\s*[()*/^+-]\\s*\\(*\\s*\\d+.*");

    public static boolean isExpression(String exp) {
        return EXPRESSION_PATTERN.matcher(exp).find();
    }

    public static double evaluate(String exp) {
        try {
            return evaluateInternal(exp);
        } catch (EmptyStackException e) {
            throw runExF("Invalid expression: {}", exp);
        }
    }

    private static double evaluateInternal(String exp) {
        final char[] tokens = exp.toCharArray();
        final Stack<Double> values = new Stack<>();
        final Stack<Character> ops = new Stack<>();

        for (int i = 0; i < tokens.length; i++) {
            final char token = tokens[i];
            if (token == ' ') continue;

            if (isNumeric(token)) {
                final StringBuilder buffer = new StringBuilder();

                while(i < tokens.length && isNumeric(tokens[i])) {
                    final char c = tokens[i++];
                    if (token == '.' && c == '.') {
                        throw new IllegalArgumentException("Invalid decimal");
                    }
                    buffer.append(c);
                }
                values.push(Double.parseDouble(buffer.toString()));
                i--;
            } else if (token == '(') {
                ops.push(token);
            } else if (token == ')') {
                while (ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.pop();
            } else if (isOperator(token)) {
                while (!ops.isEmpty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token);
            }
        }
        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }
        return values.pop();
    }

    private static boolean isNumeric(char c) {
        return c == '.' || (c >= '0' && c <= '9');
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') {
            return false;
        }
        if (op1 == '^') {
            return false;
        }
        return (op1 != '*' && op1 != '/') || (op2 != '+' && op2 != '-');
    }

    private static double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '^': return Math.pow(a, b);
            case '*': return a * b;
            case '/':
                if (b == 0.0) throw new UnsupportedOperationException("Divide by zero");
                return a / b;
            default: throw new UnsupportedOperationException("operator: " + op);
        }
    }
}
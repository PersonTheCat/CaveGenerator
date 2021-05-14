package com.personthecat.cavegenerator.util;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

/** An expression evaluator used for generating numeric field data in presets. */
public class Calculator {

    /** Determines whether a string contains numbers and operations. */
    private static final Pattern IS_EXP = Pattern.compile("[\\d.]*\\s*([-+/*()^]\\s*[\\d.]*\\s*)*");

    public static boolean isExpression(String exp) {
        return IS_EXP.matcher(exp).matches();
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
        boolean lastIsNumber = false;
        int sign = 1;

        for (int i = 0; i < tokens.length; i++) {
            final char token = tokens[i];
            if (token == ' ') continue;
            boolean number = isNumeric(token);

            if (number) {
                final StringBuilder buffer = new StringBuilder();

                while(i < tokens.length && isNumeric(tokens[i])) {
                    final char c = tokens[i++];
                    if (token == '.' && c == '.') {
                        throw new IllegalArgumentException("Invalid decimal");
                    }
                    buffer.append(c);
                }
                values.push(sign * Double.parseDouble(buffer.toString()));
                sign = 1;
                i--;
            } else if (token == '(') {
                if (lastIsNumber) {
                    ops.push('*');
                }
                ops.push(token);
            } else if (token == ')') {
                while (ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.pop();
                // A numeric value was extracted
                number = true;
            } else if (isOperator(token)) {
                if (!lastIsNumber) {
                    if (token == '-') {
                        sign = -sign;
                        continue;
                    } else if (token == '+') {
                        continue;
                    }
                }
                while (!ops.isEmpty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token);
            }
            lastIsNumber = number;
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

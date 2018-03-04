package filters;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Parse a string in the filter language and return the filter.
 * Throws a SyntaxError exception on failure.
 * <p>
 * This is a top-down recursive descent parser (a.k.a., LL(1))
 * <p>
 * The really short explanation is that an LL(1) grammar can be parsed by a collection
 * of mutually recursive methods, each of which is able to recognize a single grammar symbol.
 * <p>
 * The grammar (EBNF) for our filter language is:
 * <p>
 * goal    ::= evaluateExpression
 * evaluateExpression    ::= buildOrExpression
 * buildOrExpression  ::= buildAndExpression ( "or" buildAndExpression )*
 * buildAndExpression ::= buildNotExpression ( "and" buildNotExpression )*
 * buildNotExpression ::= buildEmptyExpression | "not" buildNotExpression
 * buildEmptyExpression    ::= word | "(" evaluateExpression ")"
 * <p>
 * The reason for writing it this way is that it respects the "natural" precedence of boolean
 * expressions, where the precedence order (decreasing) is:
 * parens
 * not
 * and
 * or
 * This allows an expression like:
 * blue or green and not red or yellow and purple
 * To be parsed like:
 * blue or (green and (not red)) or (yellow and purple)
 */
public class Parser {
    private final Scanner scanner;
    private static final String LPAREN = "(";
    private static final String RPAREN = ")";
    private static final String OR = "or";
    private static final String AND = "and";
    private static final String NOT = "not";

    public Parser(String input) {
        scanner = new Scanner(input);
    }

    public Filter parse() throws SyntaxError {
        Filter ans = null;
        try {
            ans = evaluateExpression();
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SyntaxError) {
                throw (SyntaxError) cause;
            }
        }
        if (scanner.peek() != null) {
            throw new SyntaxError("Extra stuff at end of input: " + scanner.peek());
        }
        return ans;
    }

    private Filter evaluateExpression() {
        return buildOrExpression();
    }

    private Filter expressionBuilderHelper(
            String expr,
            Supplier<Filter> left,
            Supplier<Filter> right,
            BiFunction<Filter, Filter, Filter> result) {
        Filter subExpr = left.get();
        String token = scanner.peek();
        while (token != null && token.equals(expr)) {
            scanner.advance();
            subExpr = result.apply(subExpr, right.get());
            token = scanner.peek();
        }
        return subExpr;
    }

    private Filter buildOrExpression() {
        return expressionBuilderHelper(
                OR,
                this::buildAndExpression,
                this::buildAndExpression,
                OrFilter::new);
    }

    private Filter buildAndExpression() {
        return expressionBuilderHelper(
                AND,
                this::buildNotExpression,
                this::buildNotExpression,
                AndFilter::new);
    }

    private Filter buildNotExpression() {
        String token = scanner.peek();
        if (token.equals(NOT)) {
            scanner.advance();
            Filter sub = buildNotExpression();
            return new NotFilter(sub);
        } else {
            Filter sub = buildEmptyExpression();
            return sub;
        }
    }

    private Filter buildEmptyExpression() {
        String token = scanner.peek();
        if (token.equals(LPAREN)) {
            scanner.advance();
            Filter sub = evaluateExpression();
            if (!scanner.peek().equals(RPAREN)) {
                throw new RuntimeException(new SyntaxError("Expected ')'"));
            }
            scanner.advance();
            return sub;
        } else {
            Filter sub = new BasicFilter(token);
            scanner.advance();
            return sub;
        }
    }
}

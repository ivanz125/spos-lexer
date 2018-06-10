import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Lang {

    private Set<String> keywords;
    private Set<String> directives;
    private Set<String> punctuation;
    private Set<String> operators;

    public Lang() {
        keywords = new HashSet<>();
        directives = new HashSet<>();
        punctuation = new HashSet<>();
        operators = new HashSet<>();
        readFile("keywords.txt", keywords);
        readFile("directives.txt", directives);
        readFile("punctuation.txt", punctuation);
        readFile("operators.txt", operators);
    }

    private void readFile(String fileName, Set<String> set) {
        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNext()) set.add(scanner.nextLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLineBreak(char c) {
        // carriage return or line feed
        return c == 10 || c == 13;
    }

    public boolean isDirective(String lexeme) {
        return directives.contains(lexeme.trim());
    }

    public boolean isPunctuation(String lexeme) {
        return punctuation.contains(lexeme.trim());
    }

    public boolean isKeyword(String lexeme) {
        return keywords.contains(lexeme.trim());
    }

    public boolean isKeywordChar(char c) {
        return c >= 'a' && c <= 'z';
    }

    public boolean isIdChar(char c) {
        return c >= 'a' && c <= 'z'
                || c >= 'A' && c <= 'Z'
                || c >= '0' && c <= '9'
                || c == '_';
    }

    public boolean isIdStart(char c) {
        return c >= 'a' && c <= 'z'
                || c >= 'A' && c <= 'Z'
                || c == '_' || c == '@';
    }

    public boolean isOperatorChar(char c) {
        return c == '.' || c == '?' || c == ':' || c == '+' || c == '-' || c == '*' || c == '/'
                || c == '%' || c == '&' || c == '|' || c == '^' || c == '!' || c == '~' || c == '='
                || c == '>' || c == '<';
    }

    public boolean isOperator(String lexeme) {
        return operators.contains(lexeme.trim());
    }

    public boolean canFollowNumber(char c) {
        return c == '/' || isLineBreak(c) || isOperatorChar(c) || isPunctuation(String.valueOf(c));
    }

    public boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public boolean isEscapeChar(char c) {
        return c == '\'' || c == '"' || c == '\\' || c == '0' || c == 'a'
                || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't' || c == 'v';
    }
}

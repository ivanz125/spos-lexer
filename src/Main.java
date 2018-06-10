public class Main {

    public static void main(String[] args) {
        //Lexer lexer = new Lexer("E:\\Универ\\3 курс\\2 sem\\spos-lexer\\csharp\\ConsoleApplication1\\ConsoleApplication1\\Program.cs");
        Lexer lexer = new Lexer("Program.cs");
        lexer.generateTokens();
        lexer.printTokens();
    }
}

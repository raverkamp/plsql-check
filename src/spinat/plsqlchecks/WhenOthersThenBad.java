package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Ast.Component;
import spinat.plsqlparser.CodeWalker;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;
import spinat.plsqlparser.Token;

public class WhenOthersThenBad {

    static final String charSet = "utf-8";

    static boolean isLoggingStatement(Ast.Statement stm) {
        if (!(stm instanceof Ast.ProcedureCall)) {
            return false;
        }
        Ast.ProcedureCall pc = (Ast.ProcedureCall) stm;
        if (pc.callparts.size() == 3 && pc.callparts.get(0) instanceof Component) {
            Ast.Component c = (Ast.Component) pc.callparts.get(0);
            return c.ident.val.equalsIgnoreCase("logging");
        } else {
            return false;
        }
    }

    static Seq scan(String s) {
        ArrayList<Token> a = Scanner.scanAll(s);
        ArrayList<Token> r = new ArrayList<>();
        for (Token t : a) {
            if (Scanner.isRelevant(t)) {
                r.add(t);
            }
        }
        return new Seq(r);
    }

    public static void main(String[] args) throws IOException {
        checkDir(args[0]);
    }

    public static void checkDir(String dirName) throws IOException {
        Path dirPath = Paths.get(dirName).toAbsolutePath();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
            for (Path path : directoryStream) {
                Path p2 = dirPath.resolve(path);
                checkFile(p2);
            }
        }
    }

    public static void checkFile(Path path) throws IOException {
        Parser p = new Parser();
        System.out.println("doing file: " + path.getFileName().toString());
        String source = new String(java.nio.file.Files.readAllBytes(path), charSet);
        String su = source.toUpperCase();
        int pa = su.indexOf("PACKAGE");
        int pb = su.indexOf("BODY");

        if (pa >= 0 && pb >= pa && pb <= 50) {
            try {
                Ast.PackageBody b = p.pCRPackageBody.pa(scan(source)).v;
                Walker w = new Walker();
                w.source = source;
                w.walkPackageBody(b);
            } catch (Exception ex) {
                System.out.println("   Exception " + ex.toString());
                ex.printStackTrace(System.out);
            }
        } else {
            System.out.println("skipping");
        }

    }
    
    // fixme, this simple but slow
    public static int findLineInString(String str,int pos) {
        int res = 1;
        for(int i=0;i<str.length();i++) {
            if (pos < i) {
                return res;
            }
            if (str.charAt(i) == 10) {
                res++;
            }
        }
        throw new RuntimeException("not found");
    }

    static class Walker extends CodeWalker {

        String source;
        
        int line(int pos) {
            return findLineInString(source, pos);
        }
        
        @Override
        public void walkBlock(Ast.Block block) {
            super.walkBlock(block);
            if (block.exceptionBlock != null && block.exceptionBlock.othershandler != null) {
                List<Ast.Statement> stml = block.exceptionBlock.othershandler;
                if (stml.size() == 1) {
                    Ast.Statement stm = stml.get(0);
                    if (stm instanceof Ast.NullStatement) {
                        System.out.println("  stupid null exception handler at line: " + line(stm.getStart()));
                    }
                    if (stm instanceof Ast.RaiseStatement) {
                        System.out.println("  stupid raise exception handler at line: " + line(stm.getStart()));
                    }
                } else {
                    for (int i = 0; i < stml.size() - 1; i++) {
                        if (!isLoggingStatement(stml.get(i))) {
                            return;
                        }
                    }
                    // now we assume the first stateents where just for logging
                    if (stml.get(stml.size() - 1) instanceof Ast.RaiseStatement) {
                        Ast.RaiseStatement rs = (Ast.RaiseStatement) stml.get(stml.size() - 1);
                        if (rs.name == null) {
                           System.out.println("  stupid logging and raise exception handler at line: " + line(stml.get(0).getStart()));
                        } else {
                            System.out.println("  almost stupid logging and raise exception handler at line: " + line(stml.get(0).getStart()));
                        }
                    }
                }
            }
        }
    }
}

package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import spinat.plsqlchecks.CodeWalkerWithSource.Note;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;

public class ToNumberNoNLS {

    static final String charSet = "ISO_8859_1";

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
                Seq  seq = Scanner.scanToSeq(source);
                Ast.PackageBody b = p.pCRPackageBody.pa(seq).v;
                ToNumberNoNLSWalker w = new ToNumberNoNLSWalker(source);
                w.walkPackageBody(b);
                for(Note note: w.getNotes()) {
                    System.out.println(note.line + " " + note.bla);
                }
            } catch (Exception ex) {
                System.out.println("   Exception " + ex.toString());
                ex.printStackTrace(System.out);
            }
        } else {
            System.out.println("skipping");
        }
    }

    static class ToNumberNoNLSWalker extends CodeWalkerWithSource {
        
        public ToNumberNoNLSWalker(String source) {
            super(source);
        }

        @Override
        public void walkExpression(Ast.Expression expr) {
            if (expr instanceof Ast.VarOrCallExpression) {

                Ast.VarOrCallExpression c = (Ast.VarOrCallExpression) expr;
                if (c.callparts.size() == 2
                        && c.callparts.get(0) instanceof Ast.Component
                        && c.callparts.get(1) instanceof Ast.CallOrIndexOp) {

                    Ast.Component co = (Ast.Component) c.callparts.get(0);
                    Ast.CallOrIndexOp f = (Ast.CallOrIndexOp) c.callparts.get(1);
                   
                    if (co.ident.val.equals("TO_NUMBER") && f.params.size() == 2) {
                        this.takeNoteAtPos(expr.getStart(),"to_number without nls");           
                    }
                }
            }
            super.walkExpression(expr);
        }
    }

}

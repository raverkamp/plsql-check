package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import spinat.plsqlchecks.CodeWalkerWithSource.Note;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Ast.Component;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Scanner;

public class WhenOthersThenBad {

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
        String source = Util.readFile(path);
        String su = source.toUpperCase();
        int pa = su.indexOf("PACKAGE");
        int pb = su.indexOf("BODY");

        if (pa >= 0 && pb >= pa && pb <= 50) {
            try {
                Ast.PackageBody b = p.pCRPackageBody.pa(Scanner.scanToSeq(source)).v;
                WhenOthersThenBadWalker w = new WhenOthersThenBadWalker(source);
                w.walkPackageBody(b);
                w.report(System.out);
            } catch (Exception ex) {
                System.out.println("   Exception " + ex.toString());
                ex.printStackTrace(System.out);
            }
        } else {
            System.out.println("skipping");
        }
    }

    static class WhenOthersThenBadWalker extends CodeWalkerWithSource {

        public WhenOthersThenBadWalker(String source) {
            super(source);
        }

        @Override
        public void walkBlock(Ast.Block block) {
            super.walkBlock(block);
            if (block.exceptionBlock != null && block.exceptionBlock.othershandler != null) {
                List<Ast.Statement> stml = block.exceptionBlock.othershandler;
                if (stml.size() == 1) {
                    Ast.Statement stm = stml.get(0);
                    if (stm instanceof Ast.NullStatement) {
                        this.takeNoteAtPos(stm.getStart(), "stupid null exception handler");
                    }
                    if (stm instanceof Ast.RaiseStatement) {
                        this.takeNoteAtPos(stm.getStart(), "stupid raise exception handler");
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
                            this.takeNoteAtPos(stml.get(0).getStart(), "stupid logging and raise exception handler");
                        } else {
                            this.takeNoteAtPos(stml.get(0).getStart(), "almost stupid logging and raise exception handler");
                        }
                    }
                }
            }
        }
    }
}

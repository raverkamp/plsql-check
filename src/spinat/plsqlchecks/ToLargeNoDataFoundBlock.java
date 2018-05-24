package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import spinat.plsqlchecks.CodeWalkerWithSource.Note;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.CodeWalker;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Scanner;

public class ToLargeNoDataFoundBlock {

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
                NoDataFoundWalker w = new NoDataFoundWalker(source);
                w.walkPackageBody(b);
                for (Note note : w.getNotes()) {
                    System.out.println("" + note.line + " " + note.bla);
                }
            } catch (Exception ex) {
                System.out.println("   Exception " + ex.toString());
                ex.printStackTrace(System.out);
            }
        } else {
            System.out.println("skipping");
        }

    }

    static class NoDataFoundWalker extends CodeWalkerWithSource {

        public NoDataFoundWalker(String source) {
            super(source);
        }

        @Override
        public void walkBlock(Ast.Block block) {
            super.walkBlock(block);
            if (block.exceptionBlock != null) {
                for (Ast.ExceptionHandler eh : block.exceptionBlock.handlers) {
                    if (eh.exceptions.size() == 1
                            && eh.exceptions.get(0).idents.size() == 1
                            && eh.exceptions.get(0).idents.get(0).val.equals("NO_DATA_FOUND")) {
                        if (block.statements.size() > 2) {
                            BadStatementCounter bsc = new BadStatementCounter();
                            bsc.walkStatements(block.statements);
                            if (bsc.counter > 1) {
                                this.takeNoteAtPos(block.statements.get(0).getStart(),
                                        "to large block for no_data_found handler");
                            }
                        }
                    }
                }
            }
        }
    }

    // count the direct sql statements in a block which has no_data_found exception handler
    // wrapping no data found around a block is clearly bad
    static class BadStatementCounter extends CodeWalker {

        public int counter = 0;

        @Override
        public void walkStatement(Ast.Statement s) {
            if (s instanceof Ast.SqlStatement) {
                counter++;
                return;
            }
            super.walkStatement(s);
        }

        @Override
        public void walkBlock(Ast.Block b) {
            counter += 2;
        }

        @Override
        public void walkDeclaration(Ast.Declaration d) {
            //     
        }

        @Override
        public void walkExpression(Ast.Expression e) {
            //     
        }

    }
}

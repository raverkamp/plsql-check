package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.CodeWalker;
import spinat.plsqlparser.ParseException;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Patch;
import spinat.plsqlparser.Res;
import spinat.plsqlparser.ScanException;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;
import spinat.plsqlparser.Token;

public class Varchar2Fix {

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
        if (args.length >= 1 && args[0].equals("dir")) {
            fixDirectory(args);
            return;
        }
        if (args.length != 1) {
            throw new RuntimeException("expecting one argument");
        }
        String filename = args[0];
        String filenameU = filename.toUpperCase();
        if (filenameU.endsWith("PKB")) {
            Path path = Paths.get(filename);
            String source = new String(java.nio.file.Files.readAllBytes(path), "ascii");
            String patched = patchBody(source);
            System.out.print(patched);
        }
        if (filenameU.endsWith("PKS")) {
            Path path = Paths.get(filename);
            String source = new String(java.nio.file.Files.readAllBytes(path), "ascii");
            String patched = patchSpec(source);
            System.out.print(patched);
        }
    }

    static void fixDirectory(String[] args) throws IOException {
        // first argument is "dir"
        String dirName = args[1];
        String charSet = "utf-8";
        Path dirPath = Paths.get(dirName).toAbsolutePath();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
            for (Path path : directoryStream) {
                Path p2 = dirPath.resolve(path);
                String source = new String(java.nio.file.Files.readAllBytes(path), charSet);
                String patched = null;
                try {
                    Seq seq = scan(source);
                    int pt = source.toUpperCase().indexOf("TYPE");
                    // if word type appears in the first 40 character, assume this 
                    // file contains a type definition
                    if (pt >= 0 && pt < 40) {
                        System.err.println("skipping " + path + " it is a type");
                        continue;
                    }
                    // the same trick to differentiate between 
                    // package spec and body
                    int p = source.toUpperCase().indexOf("BODY");
                    if (p >= 0 && p < 50) {
                        patched = patchBody(source);
                    } else {
                        patched = patchSpec(source);
                    }
                } catch (ParseException e) {
                    System.err.println("skipping " + path + " " + e.toString());
                } catch (ScanException e) {
                    System.err.println("skipping " + path + " " + e.toString());
                }
                if (patched != null) {
                    System.err.println("patched " + path);
                    Files.write(p2, patched.getBytes(charSet));
                }
            }
        }
    }

    public static String patchBody(String source) {
        Parser parser = new Parser();
        Seq seq = scan(source);
        Res<Ast.PackageBody> r = parser.pCRPackageBody.pa(seq);
        Walker ex = new Walker();
        ex.walkPackageBody(r.v);
        ArrayList<Integer> l = ex.varchar2Positions;
        ArrayList<Patch> pl = new ArrayList<>();
        for (Integer pos : l) {
            // pos is the position after the type, whichn in this then position after ")"
            Patch p = new Patch(pos - 1, Patch.Position.LEADING, " char");
            pl.add(p);
        }
        return Patch.applyPatches(source, pl);
    }

    public static String patchSpec(String source) {
        Parser parser = new Parser();
        Seq seq = scan(source);
        Res<Ast.PackageSpec> r = parser.pCRPackage.pa(seq);
        if (r == null) {
            System.out.println(source.substring(0, 200));
        }
        Walker ex = new Walker();
        ex.walkPackageSpec(r.v);
        ArrayList<Integer> l = ex.varchar2Positions;
        ArrayList<Patch> pl = new ArrayList<>();
        for (Integer pos : l) {
            // pos is the position after the type, whichn in this then position after ")"
            Patch p = new Patch(pos - 1, Patch.Position.LEADING, " char");
            pl.add(p);
        }
        return Patch.applyPatches(source, pl);
    }

    public static class Walker extends CodeWalker {

        public ArrayList<Integer> varchar2Positions = new ArrayList<>();

        void workOnDataType(Ast.DataType dt) {
            if (dt instanceof Ast.ParameterizedType) {
                Ast.ParameterizedType pt = (Ast.ParameterizedType) dt;
                String what = pt.ident.val.toUpperCase();

                if ((what.equals("VARCHAR2") || what.equals("CHAR") || what.equals("VARCHAR")) && pt.charOrByte == null) {
                    varchar2Positions.add(pt.getEnd());
                }
            }
        }

        @Override
        public void walkDeclaration(Ast.Declaration d) {
            if (d instanceof Ast.VariableDeclaration) {
                Ast.VariableDeclaration vd = (Ast.VariableDeclaration) d;
                workOnDataType(vd.datatype);
            }
            if (d instanceof Ast.TypeDeclaration) {
                Ast.TypeDefinition td = ((Ast.TypeDeclaration) d).typedefinition;
                if (td instanceof Ast.SubType) {
                    workOnDataType(((Ast.SubType) td).datatype);
                }
                if (td instanceof Ast.RecordType) {
                    for (Ast.RecordField f : ((Ast.RecordType) td).fields) {
                        workOnDataType(f.datatype);
                    }
                }
                if (td instanceof Ast.TableSimple) {
                    workOnDataType(((Ast.TableSimple) td).datatype);
                }
                if (td instanceof Ast.TableIndexed) {
                    workOnDataType(((Ast.TableIndexed) td).datatype);
                }
            } else {
                super.walkDeclaration(d);
            }
        }
    }

}

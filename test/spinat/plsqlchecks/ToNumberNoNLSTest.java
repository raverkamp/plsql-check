package spinat.plsqlchecks;

import java.io.IOException;
import org.junit.Test;

import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Scanner;

public class ToNumberNoNLSTest {

    public ToNumberNoNLSTest() {
    }

    @Test
    public void test1() throws IOException {
        String s = "create or replace package body pack1 as\n"
                + "function x(a varchar2) return number is\n"
                + "begin\n"
                + "  a:= to_char(to_number('1212','99düä00'));\n"
                + " bla(12);-- nix da\n"
                + "  return /* ooo*/ to_number('123','00d00','nlsbla');\n"
                + "  return to_number('123');\n"
                + "end;\n"
                + "end;\n";
        Parser p = new Parser();
        Ast.PackageBody b = p.pCRPackageBody.pa(Scanner.scanToSeq(s)).v;
        ToNumberNoNLS.ToNumberNoNLSWalker w = new ToNumberNoNLS.ToNumberNoNLSWalker(s);
        w.walkPackageBody(b);
        w.report(System.out);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spinat.plsqlchecks;

import org.junit.Test;
import static org.junit.Assert.*;
import static spinat.plsqlchecks.ToNumberNoNLS.scan;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Parser;

/**
 *
 * @author roland
 */
public class ToNumberNoNLSTest {

    public ToNumberNoNLSTest() {
    }

    @Test
    public void test1() {
        String s = "create or replace package body pack1 as\n"
                + "function x(a varchar2) return number is\n"
                + "begin\n"
                + "  a:= to_char(to_number('1212','99d00'));\n"
                + " bla(12);\n"
                + "  return to_number('123','00d00','nlsbla');\n"
                + "end;\n"
                + "end;\n";
        Parser p = new Parser();
        Ast.PackageBody b = p.pCRPackageBody.pa(scan(s)).v;
        ToNumberNoNLS.ToNumberNoNLSWalker w = new ToNumberNoNLS.ToNumberNoNLSWalker();
        w.source = s;
        w.walkPackageBody(b);
    }

}

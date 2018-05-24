package spinat.plsqlchecks;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import spinat.plsqlparser.CodeWalker;

public class CodeWalkerWithSource extends CodeWalker {
    
    public static class Note {
        public final int line;
        public final String bla;
        public Note(int line, String bla) {
            this.line = line;
            this.bla = bla;
        }
    }
    
    // fixme, this simple but slow
    private static int findLineInString(String str, int pos) {
        if (pos >= str.length()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int res = 1;
        for (int i = 0; i <= pos; i++) {
            if (str.charAt(i) == 10) {
                res++;
            }
        }
        return res;
    }
    
    final protected String source;
    
    protected int findLine(int pos) {
        return findLineInString(this.source, pos);
    }
    
    public CodeWalkerWithSource(String source) {
        super();
        this.source = source;
    }
    
    private ArrayList<Note> notes = new ArrayList<>();
    
    protected void takeNoteAtPos(int pos, String bla) {
        int line = findLine(pos);
        Note note = new Note(line, bla);
        this.notes.add(note);
    }
    
    public ArrayList<Note> getNotes() {
        ArrayList<Note> x = new ArrayList<>(this.notes);
        Collections.sort(x, (Note t1, Note t2) -> Integer.compare(t1.line, t2.line));
        return x;
    }
    
    public void report(Writer w) throws IOException {
       for(Note note: this.getNotes()) {
           w.write("" + note.line + ": " + note.bla + "\n");
       }
    }
    
    public void report(PrintStream w) throws IOException {
       for(Note note: this.getNotes()) {
           w.print("" + note.line + ": " + note.bla + "\n");
       }
    }
}

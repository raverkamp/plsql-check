package spinat.plsqlchecks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Util {
    
    public static String readFile(Path path) throws IOException {
        byte[] b =java.nio.file.Files.readAllBytes(path);
        ByteBuffer bb = ByteBuffer.wrap(b);
        
        Charset cs = StandardCharsets.US_ASCII;
        CharsetDecoder d = cs.newDecoder();
        d.onMalformedInput(CodingErrorAction.IGNORE);
        return d.decode(bb).toString();
    }
}

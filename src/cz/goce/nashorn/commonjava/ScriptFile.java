package cz.goce.nashorn.commonjava;

import java.io.Reader;

/**
 * @author goce.cz
 */
public class ScriptFile {
    private final Reader reader;
    private final String uri;

    public ScriptFile(Reader reader, String uri) {
        this.reader = reader;
        this.uri = uri;
    }

    public ScriptFile(Reader reader) {
        this(reader, null);
    }

    public Reader getReader() {
        return reader;
    }

    public String getUri() {
        return uri;
    }
}

package cz.goce.idea.scripter;

import cz.goce.nashorn.NashornConsole;

import java.io.PrintWriter;

/**
 * @author goce.cz
 */
public class ConsoleLogger extends NashornConsole {

    public ConsoleLogger( PrintWriter out, PrintWriter system, PrintWriter err ) {

        super( out, system, err, err );

    }

    public PrintWriter getOut() {
        return log;
    }

    public PrintWriter getSystem() {
        return info;
    }

    public PrintWriter getErr() {
        return error;
    }
}

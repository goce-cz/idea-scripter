package cz.goce.nashorn;

import jdk.nashorn.internal.objects.NativeError;
import jdk.nashorn.internal.objects.NativeJSON;
import jdk.nashorn.internal.runtime.ScriptObject;

import java.io.PrintWriter;

/**
 * @author goce.cz
 */
public class NashornConsole {
    protected final PrintWriter log;
    protected final PrintWriter info;
    protected final PrintWriter warn;
    protected final PrintWriter error;

    public NashornConsole( PrintWriter log, PrintWriter info, PrintWriter warn, PrintWriter error ) {
        this.log = log;
        this.info = info;
        this.warn = warn;
        this.error = error;
    }

    private String toString( Object object ) {
        if (object instanceof NativeError) {
            NativeError error = (NativeError) object;
            return String.valueOf( error.get( "stack" ) );
        } else if (object instanceof ScriptObject) {
            return String.valueOf( NativeJSON.stringify( null, object, null, 2 ) );
        } else {
            return String.valueOf( object );
        }
    }

    private void logTo( PrintWriter writer, Object[] messages ) {
        for (Object message : messages) {
            if (message instanceof Throwable) {
                ((Throwable) message).printStackTrace( writer );
            } else {
                writer.print( toString( message ) );
            }
        }
        writer.println();
    }

    public void log( Object... messages ) {
        logTo( log, messages );
    }

    public void error( Object... messages ) {
        logTo( error, messages );
    }

    public void warn( Object... messages ) {
        logTo( warn, messages );
    }

    public void info( Object... messages ) {
        logTo( info, messages );
    }

    public void trace() {
        new Exception().printStackTrace( error );
    }
}

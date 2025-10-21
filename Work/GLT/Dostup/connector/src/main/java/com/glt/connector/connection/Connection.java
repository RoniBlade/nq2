// com/glt/connector/connection/Connection.java
package com.glt.connector.connection;

import java.io.Closeable;
import java.io.IOException;

public interface Connection extends Closeable {
    String execStdout(String command) throws IOException;
    String execStderr(String command) throws IOException;

    com.glt.connector.dto.ExecResult execWithResult(String command) throws Exception;

    @Override
    void close() throws IOException;
}

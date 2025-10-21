package com.glt.connector.connection;

import com.glt.connector.task.ScanTask;

public interface ConnectionFactory {
    boolean supports(ScanTask task);
    Connection create(ScanTask task) throws Exception;
}
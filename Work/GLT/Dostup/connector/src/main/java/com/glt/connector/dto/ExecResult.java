package com.glt.connector.dto;

import lombok.Data;

@Data
public class ExecResult {

    public final String stdout;
    public final String stderr;
    public final int exitCode;

    public ExecResult(String stdout, String stderr, int exitCode) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

}

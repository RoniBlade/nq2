package com.glt.connector.dto;

import com.glt.connector.dto.Account.AccountBatch;
import com.glt.connector.task.ScanTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanResultMessage {
    private ScanTask task;
    private AccountBatch users;
}

package com.glt.connector.connector;

import com.glt.connector.dto.Account.AccountDto;
import com.glt.connector.dto.GroupDto;
import com.glt.connector.model.MachineInfo;
import com.glt.connector.model.Server;
import com.glt.connector.model.Account;
import com.glt.connector.task.ScanTask;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ServerConnector {

    List<AccountDto> scanUsers(ScanTask task) throws Exception;

    Set<GroupDto> scanGroups(ScanTask task);

    MachineInfo scanMachineInfo(ScanTask task) throws IOException;
    void blockUser(ScanTask task, Account account);
    void unblockUser(ScanTask task, Account account);
    AccountDto createUser(ScanTask task, AccountDto accountDTO, Server server);

    void addUserToGroup(ScanTask task, String login, String group);
    void removeUserFromGroup(ScanTask task, String login, String group);

    void deleteUser(ScanTask task, Account account);

    void updateUser(ScanTask task, AccountDto accountDTO, Account existingAccount);

    void changePassword(ScanTask task, Account account, String newPassword);
}

package com.pennypilot.api.service;

import com.pennypilot.api.entity.Account;
import com.pennypilot.api.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledSyncService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledSyncService.class);

    private final AccountRepository accountRepository;
    private final SyncService syncService;

    public ScheduledSyncService(AccountRepository accountRepository, SyncService syncService) {
        this.accountRepository = accountRepository;
        this.syncService = syncService;
    }

    @Scheduled(cron = "${app.sync.cron:0 0 2 * * *}")
    public void syncAllAccounts() {
        log.info("Starting scheduled sync for all accounts");
        List<Account> allAccounts = accountRepository.findAll();

        int success = 0;
        int failed = 0;

        for (Account account : allAccounts) {
            try {
                syncService.syncAccount(account.getUserId(), account.getId());
                success++;
            } catch (Exception e) {
                log.error("Scheduled sync failed for account {} (user {}): {}",
                        account.getId(), account.getUserId(), e.getMessage());
                failed++;
            }
        }

        log.info("Scheduled sync complete: {} succeeded, {} failed", success, failed);
    }
}

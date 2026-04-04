package com.pennypilot.api.service;

import com.pennypilot.api.entity.Account;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LoginSyncService {

    private static final Logger log = LoggerFactory.getLogger(LoginSyncService.class);

    private final AccountRepository accountRepository;
    private final SyncService syncService;
    private final Clock clock;
    private final Duration staleThreshold;

    public LoginSyncService(AccountRepository accountRepository,
                            SyncService syncService,
                            Clock clock,
                            @Value("${app.sync.stale-threshold-hours:1}") long staleThresholdHours) {
        this.accountRepository = accountRepository;
        this.syncService = syncService;
        this.clock = clock;
        this.staleThreshold = Duration.ofHours(staleThresholdHours);
    }

    public void syncIfStale(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);

        for (Account account : accounts) {
            if (isStale(account)) {
                try {
                    syncService.syncAccount(userId, account.getId());
                    log.info("Login sync completed for account {} (user {})", account.getId(), userId);
                } catch (Exception e) {
                    log.warn("Login sync failed for account {} (user {}): {}",
                            account.getId(), userId, e.getMessage());
                }
            }
        }
    }

    private boolean isStale(Account account) {
        if (account.getLastSyncedAt() == null) {
            return true;
        }
        return Duration.between(account.getLastSyncedAt(), clock.now()).compareTo(staleThreshold) > 0;
    }
}

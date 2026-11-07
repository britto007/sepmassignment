package com.kuet.ums.security;

import com.kuet.ums.service.AccountDirectory;
import com.kuet.ums.service.StoredAccount;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountDirectory accountDirectory;

    public DatabaseUserDetailsService(AccountDirectory accountDirectory) {
        this.accountDirectory = accountDirectory;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        StoredAccount account = accountDirectory.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown username."));
        return User.withUsername(account.username())
                .password(account.password())
                .roles(account.role().name())
                .build();
    }
}

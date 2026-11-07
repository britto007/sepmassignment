package com.kuet.ums.service;

import com.kuet.ums.dto.AdminRequest;
import com.kuet.ums.dto.AdminResponse;
import com.kuet.ums.model.Admin;
import com.kuet.ums.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AccountDirectory accountDirectory;

    @Mock
    private PasswordPolicy passwordPolicy;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createNormalizesAdminDetailsAndEncodesPassword() {
        AdminRequest request = new AdminRequest();
        request.setFullName("  Alex Admin  ");
        request.setEmail("  ALEX@KUET.AC.BD ");
        request.setUsername(" alex ");
        request.setPassword("secret12");
        when(adminRepository.findByEmailIgnoreCase("alex@kuet.ac.bd")).thenReturn(Optional.empty());
        when(passwordPolicy.encodeRequired("secret12")).thenReturn("encoded-password");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> {
            Admin admin = invocation.getArgument(0);
            admin.setId(1L);
            return admin;
        });

        AdminResponse response = adminService.create(request);

        assertEquals("Alex Admin", response.fullName());
        assertEquals("alex@kuet.ac.bd", response.email());
        assertEquals("alex", response.username());
        verify(passwordPolicy).encodeRequired("secret12");
    }
}

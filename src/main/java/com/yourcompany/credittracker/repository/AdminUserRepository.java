package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.model.AdminRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmailIgnoreCase(String email);
    List<AdminUser> findByRoleAndActiveTrue(AdminRole role);
    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AdminUser u where u.id = :id")
    Optional<AdminUser> findByIdForUpdate(Long id);
}

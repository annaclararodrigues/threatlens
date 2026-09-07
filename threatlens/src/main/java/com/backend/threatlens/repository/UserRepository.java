package com.backend.threatlens.repository;

import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    @Query("""
            SELECT user FROM UserEntity user
            WHERE (:role IS NULL OR user.role = :role)
              AND (:search IS NULL OR LOWER(user.username) LIKE :search ESCAPE '\\'
                                   OR LOWER(user.email)    LIKE :search ESCAPE '\\')
            """)
    Page<UserEntity> findAllWithFilters(
            @Param("role") Role role,
            @Param("search") String search,
            Pageable pageable
    );
}

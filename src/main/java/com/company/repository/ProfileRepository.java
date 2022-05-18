package com.company.repository;

import com.company.entity.ProfileEntity;
import com.company.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Integer> {

    Optional<ProfileEntity> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("update ProfileEntity set status = :status where id = :id")
    int updateStatus(@Param("status") ProfileStatus status, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ProfileEntity set attachId = :attachId where id = :id")
    void updateAttach(@Param("attachId") Integer attachId, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ProfileEntity set email = :email where id = :id")
    void updateEmail(@Param("email") String email, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ProfileEntity set name = :name , surname = :surname , updatedDate = :now where id = :id")
    void updateBio(@Param("name") String name, @Param("surname") String surname,
                  @Param("now") LocalDateTime now, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ProfileEntity set password = :password where id = :id")
    void updatePassword(@Param("password") String password, @Param("id") Integer id);
}
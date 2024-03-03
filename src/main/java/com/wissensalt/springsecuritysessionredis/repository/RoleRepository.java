package com.wissensalt.springsecuritysessionredis.repository;

import com.wissensalt.springsecuritysessionredis.model.Role;
import com.wissensalt.springsecuritysessionredis.model.Role.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findFirstByName(RoleName name);
}

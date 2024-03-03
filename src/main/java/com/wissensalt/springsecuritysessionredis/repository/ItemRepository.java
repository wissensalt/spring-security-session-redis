package com.wissensalt.springsecuritysessionredis.repository;

import com.wissensalt.springsecuritysessionredis.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

}

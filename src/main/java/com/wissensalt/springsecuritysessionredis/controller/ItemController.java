package com.wissensalt.springsecuritysessionredis.controller;


import com.wissensalt.springsecuritysessionredis.model.Item;
import com.wissensalt.springsecuritysessionredis.repository.ItemRepository;
import com.wissensalt.springsecuritysessionredis.request.CreateItemRequest;
import com.wissensalt.springsecuritysessionredis.request.UpdateItemRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ItemController {

  private final ItemRepository itemRepository;

  @PreAuthorize("hasAnyAuthority('priv-read-item')")
  @GetMapping("/items")
  public List<Item> getItems() {

    return itemRepository.findAll();
  }

  @PreAuthorize("hasAuthority('priv-write-item')")
  @PostMapping("/items")
  public Item createItem(@RequestBody @Valid CreateItemRequest request) {
    final Item item = new Item();
    item.setName(request.name());
    item.setPrice(request.price());

    return itemRepository.save(item);
  }

  @PreAuthorize("hasAuthority('priv-write-item')")
  @PutMapping("/items")
  public Item updateItem(@RequestBody @Valid UpdateItemRequest request) {
    final Item item = itemRepository.findById(request.id()).orElseThrow(
        () -> new EntityNotFoundException("Item with id " + request.id() + " Not found"));
    item.setName(request.name());
    item.setPrice(request.price());

    return item;
  }
}

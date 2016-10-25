package com.containerstore.api.todo.repository;

import com.containerstore.api.todo.domain.Todo;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Todo entity.
 */
@SuppressWarnings("unused")
public interface TodoRepository extends MongoRepository<Todo,String> {

}

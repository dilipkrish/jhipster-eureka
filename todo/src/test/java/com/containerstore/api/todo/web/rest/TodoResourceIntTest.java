package com.containerstore.api.todo.web.rest;

import com.containerstore.api.todo.TodoApp;

import com.containerstore.api.todo.domain.Todo;
import com.containerstore.api.todo.repository.TodoRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the TodoResource REST controller.
 *
 * @see TodoResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TodoApp.class)
public class TodoResourceIntTest {

    private static final String DEFAULT_DESCRIPTION = "AAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBB";

    private static final Boolean DEFAULT_IS_COMPLETE = false;
    private static final Boolean UPDATED_IS_COMPLETE = true;

    @Inject
    private TodoRepository todoRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restTodoMockMvc;

    private Todo todo;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TodoResource todoResource = new TodoResource();
        ReflectionTestUtils.setField(todoResource, "todoRepository", todoRepository);
        this.restTodoMockMvc = MockMvcBuilders.standaloneSetup(todoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Todo createEntity() {
        Todo todo = new Todo()
                .description(DEFAULT_DESCRIPTION)
                .isComplete(DEFAULT_IS_COMPLETE);
        return todo;
    }

    @Before
    public void initTest() {
        todoRepository.deleteAll();
        todo = createEntity();
    }

    @Test
    public void createTodo() throws Exception {
        int databaseSizeBeforeCreate = todoRepository.findAll().size();

        // Create the Todo

        restTodoMockMvc.perform(post("/api/todos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(todo)))
                .andExpect(status().isCreated());

        // Validate the Todo in the database
        List<Todo> todos = todoRepository.findAll();
        assertThat(todos).hasSize(databaseSizeBeforeCreate + 1);
        Todo testTodo = todos.get(todos.size() - 1);
        assertThat(testTodo.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTodo.isIsComplete()).isEqualTo(DEFAULT_IS_COMPLETE);
    }

    @Test
    public void getAllTodos() throws Exception {
        // Initialize the database
        todoRepository.save(todo);

        // Get all the todos
        restTodoMockMvc.perform(get("/api/todos?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(todo.getId())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
                .andExpect(jsonPath("$.[*].isComplete").value(hasItem(DEFAULT_IS_COMPLETE.booleanValue())));
    }

    @Test
    public void getTodo() throws Exception {
        // Initialize the database
        todoRepository.save(todo);

        // Get the todo
        restTodoMockMvc.perform(get("/api/todos/{id}", todo.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(todo.getId()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.isComplete").value(DEFAULT_IS_COMPLETE.booleanValue()));
    }

    @Test
    public void getNonExistingTodo() throws Exception {
        // Get the todo
        restTodoMockMvc.perform(get("/api/todos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateTodo() throws Exception {
        // Initialize the database
        todoRepository.save(todo);
        int databaseSizeBeforeUpdate = todoRepository.findAll().size();

        // Update the todo
        Todo updatedTodo = todoRepository.findOne(todo.getId());
        updatedTodo
                .description(UPDATED_DESCRIPTION)
                .isComplete(UPDATED_IS_COMPLETE);

        restTodoMockMvc.perform(put("/api/todos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedTodo)))
                .andExpect(status().isOk());

        // Validate the Todo in the database
        List<Todo> todos = todoRepository.findAll();
        assertThat(todos).hasSize(databaseSizeBeforeUpdate);
        Todo testTodo = todos.get(todos.size() - 1);
        assertThat(testTodo.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTodo.isIsComplete()).isEqualTo(UPDATED_IS_COMPLETE);
    }

    @Test
    public void deleteTodo() throws Exception {
        // Initialize the database
        todoRepository.save(todo);
        int databaseSizeBeforeDelete = todoRepository.findAll().size();

        // Get the todo
        restTodoMockMvc.perform(delete("/api/todos/{id}", todo.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Todo> todos = todoRepository.findAll();
        assertThat(todos).hasSize(databaseSizeBeforeDelete - 1);
    }
}

package controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @author Chao Chen
 */
@RestController
public class TaskController {
//    private final TaskRepository repository;
//    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
//
//    @Autowired
//    public TaskController(TaskRepository repository) {
//        this.repository = repository;
//    }
//
//    @PostMapping(value = "/tasks", consumes = "application/json")
//    public ResponseEntity<String> createOneTask( @RequestBody Task task) {
//
//        Task newTask = repository.save(task);
//        logger.debug(newTask.toDto().getTitle());
//        return new ResponseEntity<>(newTask.toDto().getTitle(), HttpStatus.OK);
//    }
//
//    @GetMapping(value = "/tasks/{id}")
//    public ResponseEntity<TaskDto> readOneTask(@PathVariable("id")  Long id) {
//        Optional<Task> taskOpt = repository.findById(id);
//        if (taskOpt.isPresent()) {
//            return new ResponseEntity<>(taskOpt.get().toDto(), HttpStatus.OK);
//        } else {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//    }
//
//    @PutMapping(value = "/tasks/{id}", consumes = "application/json")
//    public ResponseEntity<String> updateOneTask(@PathVariable Long id,@Valid @RequestBody Task task) {
//        Optional<Task> taskOpt = repository.findById(id);
//        if (taskOpt.isPresent()) {
//            TaskDto putTaskDto = task.toDto();
//            boolean exists = false;
//            StringBuilder statusSb = new StringBuilder();
//            for (TaskStatus status : TaskStatus.values()) {
//                if (status.name().equalsIgnoreCase(putTaskDto.getStatus())) {
//                    exists = true;
//                }
//                statusSb.append(status.name()).append(", ");
//            }
//            if (!exists) {
//                statusSb.deleteCharAt(statusSb.length() - 2);
//                String response = "Available statuses are: " + statusSb.toString() + ".";
//                return new ResponseEntity<>(response, HttpStatus.OK);
//            }
//            repository.save(task);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } else {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//
//    }
//
//
//    @DeleteMapping(value = "/tasks/{id}")
//    public ResponseEntity<Void> deleteOneTask(@PathVariable Long id) {
//        Optional<Task> taskOpt = repository.findById(id);
//        if (taskOpt.isPresent()) {
//            repository.deleteById(id);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } else {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//
//    }
//
//
//    @GetMapping(value = "/tasks/describe/{id}")
//    public ResponseEntity<String> describeOneTask(@PathVariable Long id) {
//        Optional<Task> taskOpt = repository.findById(id);
//        if (taskOpt.isPresent()) {
//            TaskDto taskDto = taskOpt.get().toDto();
//            String response = "Description of Task [" + taskDto.getId() + ":" + taskDto.getTitle() + "]is:" + taskDto.getDescription();
//            return new ResponseEntity<>(response, HttpStatus.OK);
//        } else {
//            String response = "Task with id = " + id + " does not exist";
//            return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
//        }
//    }
//
//    @GetMapping(value = "/tasks", produces = "application/json")
//    public ResponseEntity<List<TaskDto>> getTasks() {
//        List<Task> taskList = new ArrayList<>();
//        repository.findAll().forEach(taskList::add);
//        List<TaskDto> taskDtosList = new ArrayList<>();
//        for (Task t : taskList)
//            taskDtosList.add(t.toDto());
//        return new ResponseEntity<>(taskDtosList, HttpStatus.OK);
//    }
//
//    @GetMapping(value = "/tasks/describe", produces = "application/json")
//    public ResponseEntity<List<String>> describeTasks() {
//        List<Task> taskList = new ArrayList<>();
//        repository.findAll().forEach(taskList::add);
//        List<String> taskDescription = new ArrayList<>();
//        for (Task task : taskList) {
//            String description = "Description of Task [" + task.toDto().getId() + ":" + task.toDto().getTitle() + "]is:" + task.toDto().getDescription();
//            taskDescription.add(description);
//        }
//        return new ResponseEntity<>(taskDescription, HttpStatus.OK);
//    }
}

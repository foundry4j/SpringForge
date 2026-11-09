package io.github.foundry4j.forge.app.dict;

import io.github.foundry4j.forge.data.dict.DictItemForm;
import io.github.foundry4j.forge.data.dict.DictItemView;
import io.github.foundry4j.forge.data.dict.DictService;
import io.github.foundry4j.forge.data.dict.DictType;
import io.github.foundry4j.forge.data.dict.DictTypeForm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// Administrative CRUD of dictionaries.
@RestController
@RequestMapping("/api/admin/dicts")
public class DictAdminController {

    private final DictService dictService;

    /// Creates the controller.
    public DictAdminController(DictService dictService) {
        this.dictService = dictService;
    }

    /// Lists every dictionary type.
    @GetMapping
    public List<DictType> types() {
        return dictService.types();
    }

    /// Lists every item of a dictionary, disabled ones included.
    @GetMapping("/{typeCode}/items")
    public List<DictItemView> items(@PathVariable String typeCode) {
        return dictService.allItems(typeCode);
    }

    /// Creates a dictionary type.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictType createType(@Valid @RequestBody DictTypeRequest request) {
        return dictService.createType(new DictTypeForm(request.code(), request.name(),
                request.remark(), request.enabled()));
    }

    /// Updates a dictionary type.
    @PutMapping("/{typeCode}")
    public DictType updateType(@PathVariable String typeCode, @Valid @RequestBody DictTypeRequest request) {
        return dictService.updateType(typeCode, new DictTypeForm(request.code(), request.name(),
                request.remark(), request.enabled()));
    }

    /// Deletes a dictionary type with all of its items.
    @DeleteMapping("/{typeCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteType(@PathVariable String typeCode) {
        dictService.deleteType(typeCode);
    }

    /// Creates an item inside a dictionary.
    @PostMapping("/{typeCode}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DictItemView createItem(@PathVariable String typeCode, @Valid @RequestBody DictItemRequest request) {
        return dictService.createItem(typeCode, toForm(request));
    }

    /// Updates an item selected by its value.
    @PutMapping("/{typeCode}/items/{value}")
    public DictItemView updateItem(@PathVariable String typeCode, @PathVariable String value,
                                   @Valid @RequestBody DictItemRequest request) {
        return dictService.updateItem(typeCode, value, toForm(request));
    }

    /// Deletes an item selected by its value.
    @DeleteMapping("/{typeCode}/items/{value}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable String typeCode, @PathVariable String value) {
        dictService.deleteItem(typeCode, value);
    }

    private static DictItemForm toForm(DictItemRequest request) {
        return new DictItemForm(request.label(), request.value(), request.sortNo(), request.enabled(),
                request.extra(), request.parentCode(), request.level(), request.description());
    }
}

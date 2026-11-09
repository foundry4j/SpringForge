package io.github.foundry4j.forge.app.dict;

import io.github.foundry4j.forge.data.dict.DictItemView;
import io.github.foundry4j.forge.data.dict.DictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// Exposes cached data dictionary entries.
@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private final DictService dictService;

    /// Creates the controller.
    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /// Returns the enabled items of a dictionary; repeated calls hit the cache.
    @GetMapping("/{typeCode}")
    public List<DictItemView> items(@PathVariable String typeCode) {
        return dictService.items(typeCode);
    }
}

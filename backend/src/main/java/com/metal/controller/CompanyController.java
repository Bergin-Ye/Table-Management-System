package com.metal.controller;

import com.metal.common.Result;
import com.metal.common.ServiceHelper;
import com.metal.entity.Company;
import com.metal.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService service;

    @GetMapping
    public Result<List<Company>> findAll() {
        return Result.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public Result<Company> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<Company> create(@RequestBody Company company) {
        ServiceHelper.requireAdmin();
        return Result.ok(service.create(company));
    }

    @PutMapping("/{id}")
    public Result<Company> update(@PathVariable Long id, @RequestBody Company company) {
        ServiceHelper.requireAdmin();
        company.setId(id);
        return Result.ok(service.update(company));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ServiceHelper.requireAdmin();
        service.delete(id);
        return Result.ok();
    }
}

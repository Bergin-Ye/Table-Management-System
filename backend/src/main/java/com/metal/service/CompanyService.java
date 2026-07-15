package com.metal.service;

import com.metal.common.BizException;
import com.metal.entity.Company;
import com.metal.mapper.CompanyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    @Autowired
    private CompanyMapper mapper;

    public List<Company> findAll() {
        return mapper.findAll();
    }

    public Company getById(Long id) {
        Company c = mapper.findById(id);
        if (c == null) throw new BizException("公司不存在");
        return c;
    }

    @Transactional
    public Company create(Company company) {
        mapper.insert(company);
        return company;
    }

    @Transactional
    public Company update(Company company) {
        getById(company.getId());
        mapper.update(company);
        return company;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}

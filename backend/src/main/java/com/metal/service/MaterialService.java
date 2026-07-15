package com.metal.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.entity.Material;
import com.metal.mapper.MaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialService {

    @Autowired
    private MaterialMapper mapper;

    public PageResult<Material> query(int page, int pageSize, Long companyId, String keyword) {
        PageHelper.startPage(page, pageSize);
        List<Material> list = mapper.search(companyId, keyword);
        PageInfo<Material> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public Material getById(Long id) {
        Material m = mapper.findById(id);
        if (m == null) throw new BizException("物料不存在");
        return m;
    }

    @Transactional
    public Material create(Material material) {
        mapper.insert(material);
        return material;
    }

    @Transactional
    public Material update(Material material) {
        getById(material.getId());
        mapper.update(material);
        return material;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择要删除的记录");
        mapper.batchDelete(ids);
    }

    public List<Material> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return mapper.searchByKeyword(keyword);
    }
}

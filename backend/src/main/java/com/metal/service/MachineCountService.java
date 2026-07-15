package com.metal.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.common.ServiceHelper;
import com.metal.entity.MachineCount;
import com.metal.mapper.MachineCountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MachineCountService {

    @Autowired
    private MachineCountMapper mapper;

    public PageResult<MachineCount> query(int page, int pageSize, Long companyId, String keyword,
                                           String statMonth, String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<MachineCount> list = mapper.search(companyId, keyword, statMonth, sortField, sortOrder);
        PageInfo<MachineCount> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public MachineCount getById(Long id) {
        MachineCount r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    @Transactional
    public MachineCount create(MachineCount record) {
        mapper.insert(record);
        return record;
    }

    @Transactional
    public MachineCount update(MachineCount record) {
        getById(record.getId());
        mapper.update(record);
        return record;
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
}

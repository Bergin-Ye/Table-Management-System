package com.metal.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.metal.common.BizException;
import com.metal.common.PageResult;
import com.metal.common.ServiceHelper;
import com.metal.entity.SettlementMachine;
import com.metal.mapper.SettlementMachineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettlementMachineService {

    @Autowired
    private SettlementMachineMapper mapper;

    public PageResult<SettlementMachine> query(int page, int pageSize, Long companyId, String keyword,
                                                String machineModel, String sortField, String sortOrder) {
        sortField = ServiceHelper.sanitizeSortField(sortField, "id");
        sortOrder = ServiceHelper.sanitizeSortOrder(sortOrder);
        PageHelper.startPage(page, pageSize);
        List<SettlementMachine> list = mapper.search(companyId, keyword, machineModel, sortField, sortOrder);
        PageInfo<SettlementMachine> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, pageSize, list);
    }

    public SettlementMachine getById(Long id) {
        SettlementMachine r = mapper.findById(id);
        if (r == null) throw new BizException("记录不存在");
        return r;
    }

    @Transactional
    public SettlementMachine create(SettlementMachine record) {
        mapper.insert(record);
        return record;
    }

    @Transactional
    public SettlementMachine update(SettlementMachine record) {
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

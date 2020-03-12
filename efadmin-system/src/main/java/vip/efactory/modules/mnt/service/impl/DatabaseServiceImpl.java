package vip.efactory.modules.mnt.service.impl;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.efactory.ejpa.base.controller.EPage;
import vip.efactory.ejpa.base.service.impl.BaseServiceImpl;
import vip.efactory.modules.mnt.domain.Database;
import vip.efactory.modules.mnt.repository.DatabaseRepository;
import vip.efactory.modules.mnt.service.DatabaseService;
import vip.efactory.modules.mnt.service.dto.DatabaseDto;
import vip.efactory.modules.mnt.service.dto.DatabaseQueryCriteria;
import vip.efactory.modules.mnt.service.mapper.DatabaseMapper;
import vip.efactory.modules.mnt.util.SqlUtils;
import vip.efactory.utils.FileUtil;
import vip.efactory.utils.QueryHelp;
import vip.efactory.utils.ValidationUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author zhanghouying
 * @date 2019-08-24
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DatabaseServiceImpl extends BaseServiceImpl<Database, String, DatabaseRepository> implements DatabaseService {

    private DatabaseMapper databaseMapper;

    public DatabaseServiceImpl(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    @Override
    public Object queryAll(DatabaseQueryCriteria criteria, Pageable pageable) {
        Page<Database> page = br.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return new EPage(page.map(databaseMapper::toDto));
    }

    @Override
    public List<DatabaseDto> queryAll(DatabaseQueryCriteria criteria) {
        return databaseMapper.toDto(br.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder)));
    }

    @Override
    public DatabaseDto findDtoById(String id) {
        Database database = br.findById(id).orElseGet(Database::new);
        ValidationUtil.isNull(database.getId(), "Database", "id", id);
        return databaseMapper.toDto(database);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DatabaseDto create(Database resources) {
        resources.setId(IdUtil.simpleUUID());
        return databaseMapper.toDto(br.save(resources));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update2(Database resources) {
        Database database = br.findById(resources.getId()).orElseGet(Database::new);
        ValidationUtil.isNull(database.getId(), "Database", "id", resources.getId());
        database.copy(resources);
        br.save(database);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<String> ids) {
        for (String id : ids) {
            br.deleteById(id);
        }
    }

    @Override
    public boolean testConnection(Database resources) {
        try {
            return SqlUtils.testConnection(resources.getJdbcUrl(), resources.getUserName(), resources.getPwd());
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void download(List<DatabaseDto> queryAll, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DatabaseDto databaseDto : queryAll) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("数据库名称", databaseDto.getName());
            map.put("数据库连接地址", databaseDto.getJdbcUrl());
            map.put("用户名", databaseDto.getUserName());
            map.put("创建日期", databaseDto.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}

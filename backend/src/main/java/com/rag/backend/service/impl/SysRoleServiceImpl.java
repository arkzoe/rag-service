package com.rag.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.backend.entity.SysRole;
import com.rag.backend.service.SysRoleService;
import com.rag.backend.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author Acer
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

}





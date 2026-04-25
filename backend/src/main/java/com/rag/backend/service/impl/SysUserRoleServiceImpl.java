package com.rag.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.backend.entity.SysUserRole;
import com.rag.backend.service.SysUserRoleService;
import com.rag.backend.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author Acer
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Service实现
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
    implements SysUserRoleService{

}





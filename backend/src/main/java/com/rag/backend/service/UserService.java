package com.rag.backend.service;

import com.rag.backend.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Acer
* @description 针对表【sys_user(系统用户表)】的数据库操作Service
*/
public interface UserService extends IService<User> {

    User findByUsername(String username);
}

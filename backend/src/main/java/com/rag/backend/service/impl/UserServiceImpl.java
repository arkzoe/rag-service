package com.rag.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.backend.entity.User;
import com.rag.backend.service.UserService;
import com.rag.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author Acer
* @description 针对表【sys_user(系统用户表)】的数据库操作Service实现
* @createDate 2026-04-25 09:02:32
*/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    private final UserMapper userMapper;

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体，若不存在则返回 null
     */
    @Override
    public User findByUsername(String username) {
        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建查询条件
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
    }

}





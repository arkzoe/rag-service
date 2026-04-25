package com.rag.backend.common.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SaTokenConfig implements StpInterface {

    /**
     * 返回当前账号所拥有的角色标识集合
     *  生产环境请从数据库查询
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 模拟：ID 为 1 的是管理员，其他是普通用户
        if ("1".equals(loginId.toString())) {
            return Arrays.asList("admin", "user");
        }
        return Arrays.asList("user");
    }

    /**
     * 返回当前账号所拥有的权限标识集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 模拟：管理员拥有所有权限
        if ("1".equals(loginId.toString())) {
            return Arrays.asList("user:add", "user:delete", "doc:upload", "chat:ask");
        }
        return Arrays.asList("chat:ask");
    }
}
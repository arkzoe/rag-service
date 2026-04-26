package com.rag.backend.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.rag.backend.common.result.Result;
import com.rag.backend.dto.LoginRequest;
import com.rag.backend.entity.User;
import com.rag.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        log.info("用户登录尝试: {}, 密码长度: {}", username, password != null ? password.length() : 0);

        // 1. 校验用户
        User user = userService.findByUsername(username);
        if (user == null) {
            log.warn("登录失败 - 用户不存在: {}", username);
            return Result.error("用户名或密码错误");
        }

        log.info("找到用户: {}, 数据库密码: {}", username, user.getPassword());

        // 2. 使用明文密码验证
        if (!password.equals(user.getPassword())) {
            log.warn("登录失败 - 密码错误: {}", username);
            return Result.error("用户名或密码错误");
        }

        // 3. 登录成功，生成 Token
        StpUtil.login(user.getId());

        // 4. 获取 Token 信息
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        log.info("用户登录成功: {}, Token: {}", username, tokenInfo.tokenValue);
        return Result.success(tokenInfo);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        StpUtil.logout();
        log.info("用户登出: {}", loginId);
        return Result.success("登出成功");
    }

    @GetMapping("/info")
    public Result<?> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
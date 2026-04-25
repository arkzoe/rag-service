package com.rag.backend.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

/**
 * 构建Qdrant过滤器的工具类
 */
@Slf4j
public class QdrantFilterBuilder {

    /**
     * 构建权限过滤器
     * 逻辑：查询 allowed_roles 字段包含用户任一角色的文档
     *
     * @param fieldName 字段名，如 "allowed_roles"
     * @param roles     用户角色列表
     * @return Filter.Expression对象
     */
    public static Filter.Expression buildRoleFilter(String fieldName, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        // 如果只有一个角色，使用 eq
        if (roles.size() == 1) {
            return builder.eq(fieldName, roles.get(0)).build();
        }

        // 如果有多个角色，使用 in
        // 将List转换为Object数组
        Object[] roleArray = roles.toArray(new Object[0]);
        return builder.in(fieldName, roleArray).build();
    }

    /**
     * 构建权限过滤器（返回字符串形式）
     */
    public static String buildRoleFilterString(String fieldName, List<String> roles) {
        Filter.Expression filter = buildRoleFilter(fieldName, roles);
        if (filter == null) {
            return null;
        }
        // Filter.Expression的toString()返回表达式字符串
        return filter.toString();
    }
}

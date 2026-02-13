package com.archivesystem.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyMetaObjectHandler 简单测试.
 * 注意: strictInsertFill/strictUpdateFill 方法需要 MyBatis-Plus TableInfo,
 * 在纯单元测试中难以模拟，完整测试需要集成测试环境。
 */
class MyMetaObjectHandlerTest {

    @Test
    void testHandlerInstance() {
        MyMetaObjectHandler handler = new MyMetaObjectHandler();
        assertNotNull(handler);
    }

    @Test
    void testHandlerImplementsMetaObjectHandler() {
        MyMetaObjectHandler handler = new MyMetaObjectHandler();
        assertTrue(handler instanceof com.baomidou.mybatisplus.core.handlers.MetaObjectHandler);
    }
}

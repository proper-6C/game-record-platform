package com.example.gamerecord;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游戏对局记录分享平台 - 启动类
 *
 * <p>启动前准备：</p>
 * <ol>
 *   <li>执行 sql/init.sql 初始化数据库（MySQL 8）</li>
 *   <li>修改 application.yml 中的数据库密码</li>
 *   <li>运行本类 main 方法，访问 http://localhost:8080</li>
 * </ol>
 */
@SpringBootApplication
@MapperScan("com.example.gamerecord.mapper")
public class GameRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameRecordApplication.class, args);
        System.out.println("""

                ==========================================
                 游戏对局记录平台后端启动成功
                 接口地址: http://localhost:8080
                 接口文档见项目 README.md
                ==========================================
                """);
    }
}

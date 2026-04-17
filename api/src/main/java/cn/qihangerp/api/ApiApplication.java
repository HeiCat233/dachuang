package cn.qihangerp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import java.io.File;

@SpringBootApplication(scanBasePackages = "cn.qihangerp")
public class ApiApplication
{
    public static void main( String[] args )
    {
        // 1. 强制设置 JVM 全局临时目录
        String tempPath = "D:/develop/temp";
        File tempDir = new File(tempPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        System.setProperty("java.io.tmpdir", tempPath);

        System.out.println( "Hello qihang-oms!" );
        SpringApplication.run(ApiApplication.class, args);
    }

    /**
     * 2. 终极方案：通过编程方式强制指定 Undertow 的临时工作目录
     * 这能绕过绝大多数 Windows 权限限制
     */
    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowTempDirCustomizer() {
        return factory -> {
            File tempDir = new File("D:/develop/temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            // 核心：强制设置 Undertow 的工作根目录，避开系统 C:\Windows\Temp
            factory.setDocumentRoot(tempDir);
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
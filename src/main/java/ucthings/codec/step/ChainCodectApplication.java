package ucthings.codec.step;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 启动器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/11/17 10:09
 */
@SpringBootApplication(scanBasePackages = {
		"ucthings.codec",
}
)

public class ChainCodectApplication {

	static {
		System.setProperty("spring.system.name", "chainCodec");
		System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
	}

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(ChainCodectApplication.class);
		try {
			springApplication.run(args);
		} catch (Exception e) {
			System.out.println("启动异常 " + e.getMessage());
		}
	}
}

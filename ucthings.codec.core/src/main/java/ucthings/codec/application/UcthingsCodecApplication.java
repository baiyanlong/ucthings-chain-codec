package ucthings.codec.application;

/**
 * @author byl
 * @since 2022/11/14 09:33
 */

@SpringBootApplication(scanBasePackages = {
		"com.ucthings.wiotarelay.app",
		"com.ucthings.wiotarelay.common",
		"com.ucthings.wiotarelay.db",
		"com.ucthings.wiotarelay.tcp",
		"com.ucthings.wiotarelay.mqtt",
		"com.ucthings.wiotarelay.transfer",
		"com.ucthings.wiotarelay.codec"
})
@MapperScan("com.ucthings.wiotarelay.db.mapper")
@EnableOpenApi
@EnableKnife4j

public class UcthingsCodecApplication {
}

package il.co.boj.currencyRates.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties (prefix = "CurrencyRates")
@Data
public class CurrencyRatesConfig {

	private String inputFolder;
	private String outPutFolder;
	private String archiveFolder;
	private String destination;
	private String FailedFolder;
	private String errorFolder;
	private String TestDecodingFolder;
}

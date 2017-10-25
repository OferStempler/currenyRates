package il.co.boj.currencyRates.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CurrenciesMap {

	
	
	public Map<String, String> currencyMap  = new HashMap<String, String>();
	
	@Bean
	public Map<String, String> currencyMapBean(){
//		Map<String, String> currencyMap = new HashMap<String, String>();
		currencyMap.put("01", "840");
		currencyMap.put("02", "826");
		currencyMap.put("03", "752");
		currencyMap.put("05", "756");
		currencyMap.put("06", "124");
		currencyMap.put("12", "208");
		currencyMap.put("17", "710");
		currencyMap.put("18", "036");
		currencyMap.put("20", "978");
		currencyMap.put("28", "578");
		currencyMap.put("29", "344");
		currencyMap.put("31", "392");
		currencyMap.put("33", "554");
		currencyMap.put("34", "949");
		currencyMap.put("35", "484");
		currencyMap.put("36", "986");
		currencyMap.put("37", "643");
		currencyMap.put("38", "356");
		currencyMap.put("39", "360");
		currencyMap.put("96", "702");
		
		
		return currencyMap;
	}
	
}

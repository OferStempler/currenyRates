package il.co.boj.currencyRates.model;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class FileData {

	
	private String name;
	private String type;
	private String content;
}

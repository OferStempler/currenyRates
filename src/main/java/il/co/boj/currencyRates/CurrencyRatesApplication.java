package il.co.boj.currencyRates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import il.co.boj.currencyRates.service.ConvertToRS2;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@SpringBootApplication
public class CurrencyRatesApplication extends SpringBootServletInitializer {

	//this is not the conventional Spring init. This one start the wanted method after start up.
	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(CurrencyRatesApplication.class, args);

		context.getBean(ConvertToRS2.class).listenToFolder();
	}
}

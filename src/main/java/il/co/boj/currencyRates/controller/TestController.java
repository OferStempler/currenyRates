package il.co.boj.currencyRates.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import il.co.boj.currencyRates.model.FileData;
import il.co.boj.currencyRates.model.PrepaidResponse;
import il.co.boj.currencyRates.service.ConvertToRS2;
import lombok.extern.log4j.Log4j;

@Controller
@Log4j
public class TestController {

	@Autowired
	ConvertToRS2 converToRS2;
	
	//http://localhost:8080/file
	@RequestMapping (path = "/file",  method = RequestMethod.POST)
	public @ResponseBody PrepaidResponse getPost (@RequestBody FileData fileData){
		
		//this is the base64 String
		log.debug("TESTER CLIENT GOT THE CONTENT");		
		boolean decofingTest = converToRS2.DecodeFileFromBase64String(fileData);
		PrepaidResponse response = new PrepaidResponse();
		if (decofingTest){
		response.setErrorCode("0");
		response.setErrorMessage("OK");
		} else {
			response.setErrorCode("1");
			response.setErrorMessage("Base64 content decoding failed.");	
		}
		return response;
			
	}
	//-----------------------------------------------------------------------------------------------------------------
    @RequestMapping(value = {"/test", "/monitor"}, method = RequestMethod.GET, produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody  String test(HttpServletRequest request, HttpServletResponse response) {
        String str = "CurrencyRates is jacked up and good to go. " + new Date();
        log.trace(str);
        return str;
    }
}

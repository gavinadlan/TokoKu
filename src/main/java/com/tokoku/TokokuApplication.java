package com.tokoku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

// Page dikirim sebagai DTO stabil: {content, page:{size,number,totalElements,totalPages}}
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class TokokuApplication {

	public static void main(String[] args) {
		SpringApplication.run(TokokuApplication.class, args);
	}

}

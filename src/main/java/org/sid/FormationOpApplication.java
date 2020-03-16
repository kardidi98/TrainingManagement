



package org.sid;
import org.sid.dao.ClientRepository;
import org.sid.dao.FormationRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class FormationOpApplication {

	public static void main(String[] args) {
		
	
		ApplicationContext ctx =SpringApplication.run(FormationOpApplication.class, args);
		FormationRepository formationdao =ctx.getBean(FormationRepository.class);
		ClientRepository clientRepository=ctx.getBean(ClientRepository.class);
	}

}

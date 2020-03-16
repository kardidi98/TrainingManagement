package org.sid.web;

import java.util.List;

import javax.validation.Valid;

import org.sid.dao.ClientRepository;
import org.sid.entities.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.request.WebRequest;

@Controller
public class ClientController {
	@Autowired
	private ClientRepository clientRepository;
	

	
	@RequestMapping(value="/Register",method=RequestMethod.GET)
	public String formProduit(Model model) {
		model.addAttribute("client",new Client());
		return "register";
	}
	@RequestMapping(value="/Registration",method=RequestMethod.POST)
	public String save(Model model ,@RequestParam("nom") String nom,@RequestParam("prenom") String prenom,@RequestParam("email") String email,@RequestParam("password") String password,@RequestParam("type") String type) {
		
		Client client=new Client(nom,prenom,email,password,type);
		
		clientRepository.save(client);
		model.addAttribute("user",client);
		
		return "index";
	}
	@RequestMapping(value="/login",method=RequestMethod.GET)
	public String authentification(Model model) {
		model.addAttribute("client",new Client());
		return "login";
	}

	

}


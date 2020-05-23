package org.sid.web;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.sid.dao.AdminRepository;
import org.sid.entities.Admin;
import org.sid.entities.Client;
import org.sid.entities.Commentaire;
import org.sid.entities.Formation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

	@Autowired
	private AdminRepository adminRepository;

	@RequestMapping(value="/", method = RequestMethod.GET)
	public String admin(Model model,HttpServletRequest request) {

		HttpSession session=request.getSession(true);

		long countUsers=adminRepository.countUsers();

		Map<String, Integer> surveyMap = new LinkedHashMap<>();
		surveyMap.put("2020", 100);
		surveyMap.put("2021", 350);
		surveyMap.put("2022", 400);
		surveyMap.put("2023", 550);
		surveyMap.put("2024", 600);
		
		model.addAttribute("surveyMap", surveyMap);


		model.addAttribute("onestar", 5);
		model.addAttribute("twostars", 15);
		model.addAttribute("treestars", 30);
		model.addAttribute("fourstars", 40);
		model.addAttribute("fivestars", 10);

		model.addAttribute("countusers", countUsers);
		model.addAttribute("session", session.getAttribute("user"));

		return "admin";		
	}
	
	@RequestMapping(value="/Adminlogin", method = RequestMethod.POST)
	public String login(Model model,HttpServletRequest request,@RequestParam("email") String email, @RequestParam("password")  String password) {

		HttpSession session=request.getSession(true);
		String error=null;
		Admin Admin = adminRepository.findByEmail(email);
		if(Admin.getPassword().equals(password)) {
			session=request.getSession(true);
			session.setAttribute("user", Admin);
		}
		else {
			error="Invalid Information !";
			model.addAttribute("error",error);
			return "admin";
		}

		model.addAttribute("error",error);
		model.addAttribute("session", session.getAttribute("user"));

		return "redirect:/";		
	}

	@RequestMapping(value="/logout",method=RequestMethod.GET)
	public String logout(Model model,HttpSession session) {
		session.invalidate();

		return "redirect:/";
	}

}

package org.sid.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.*;
import org.apache.commons.io.IOUtils;
import org.sid.dao.*;

import org.sid.entities.Formation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class FormationController {
	
	@Autowired
	private FormationRepository formationRepository;
	@Value("${dir.images}")
	private String imageDir;
	
	
	
	
	@RequestMapping(value="/TrainingManagement", method = RequestMethod.GET)
	public String home(Model model) {		
		return "home";		
	}
	@RequestMapping(value="/AddArticle", method = RequestMethod.GET)
	public String formulaireFormation(Model model) {	
		model.addAttribute("formation",new Formation());
		return "Ad-listing";		
	}
	
	
	@RequestMapping(value="/save", method =RequestMethod.POST)
	public String save(Model model, Formation formation,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		if(bindingResult.hasErrors()) {
			return "dashboard-my-ads";
		}
		
		if(!(file.isEmpty())) {
			formation.setSignificantPhoto(file.getOriginalFilename());
		
		}
		formationRepository.save(formation);
		if(!(file.isEmpty())) {
			formation.setSignificantPhoto(file.getOriginalFilename());
			file.transferTo(new File(imageDir+formation.getId()));
		}
		
		return "redirect:EditAds";	
	}
	
	@RequestMapping(value="/listFormation", method =RequestMethod.GET)
	public String listFormation(Model model) {
		List<Formation> formation=formationRepository.findAll();
		model.addAttribute("formation",formation);
		return "category";
	}
	
	@RequestMapping(value="/EditAds", method =RequestMethod.GET)
	public String EditMyAds(Model model) {
		List<Formation> formation=formationRepository.findAll();
		model.addAttribute("myformation",formation);
		return "dashboard-my-ads";
	}
	
	@RequestMapping(value="getPhoto",produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] getPhoto(Long id) throws FileNotFoundException, IOException {
		File f=new File(imageDir+id);
		return IOUtils.toByteArray(new FileInputStream(f));
	}
	
	
	@RequestMapping(value="/delete", method =RequestMethod.GET)
	public String delete(Long id) {
		formationRepository.deleteById(id);
		return "redirect:EditAds";
	}
	
	@RequestMapping(value="/viewArticle", method =RequestMethod.GET)
	public String viewArticle(Model model,Long id) {
		Formation article= formationRepository.getOne(id);
		model.addAttribute("article",article);
		return "single";
	}
	
	@RequestMapping(value="/editArticle", method =RequestMethod.GET)
	public String editArticle(Model model,Long id) {
		Formation a= formationRepository.getOne(id);
		model.addAttribute("article",a);
		return "UpdateArticle";
	}
	
	
	@RequestMapping(value="/UpdateArticle", method =RequestMethod.POST)
	public String update(Model model, @Valid Formation formation,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		if(bindingResult.hasErrors()) {
			return "redirect:editArticle";
		}
		
		if(!(file.isEmpty())) {
			formation.setSignificantPhoto(file.getOriginalFilename());
		
		}
		formationRepository.save(formation);
		if(!(file.isEmpty())) {
			formation.setSignificantPhoto(file.getOriginalFilename());
			file.transferTo(new File(imageDir+formation.getId()));
		}
		
		return "redirect:EditAds";	
	}
	
	
}

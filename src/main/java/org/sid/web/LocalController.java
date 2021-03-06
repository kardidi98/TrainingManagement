package org.sid.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.sid.dao.CategoryLocalRepository;
import org.sid.dao.CategoryRepository;
import org.sid.dao.CityRepository;
import org.sid.dao.FormationRepository;
import org.sid.dao.LocalRepository;
import org.sid.entities.Client;
import org.sid.entities.Formation;
import org.sid.entities.Local;
import org.sid.mailSender.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class LocalController {

	@Autowired
	private NotificationService notificationService;

	private String picture1;
	private String picture2;
	private String picture3;
	private String picture4;
	private String picture5;
	private String picture6;
	@Autowired
	private LocalRepository localRepository;

	@Autowired
	private FormationRepository formationRepository;

	@Autowired
	private CityRepository cityRepository;
	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private CategoryLocalRepository categorylocalRepository;

	@Value("${dir.Localimages}")
	private String localImageDir;

	private boolean ShouldExpandRole=false;


	@RequestMapping(value="/AddLocal", method = RequestMethod.GET)
	public String formulaireLocal(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Client client=(Client) session.getAttribute("user");
		if(session.getAttribute("user")==null) {
			return "login";
		}

		if(!client.getType().equals("Local Provider") && !client.getEtendreRole1().equals("Local Provider") && !client.getEtendreRole2().equals("Local Provider")) {


			if(!client.getType().equals("Trainer") && !client.getEtendreRole1().equals("Trainer") && !client.getEtendreRole2().equals("Trainer") || 
					!client.getType().equals("Local Provider") && !client.getEtendreRole1().equals("Local Provider") && !client.getEtendreRole2().equals("Local Provider")) {
				ShouldExpandRole=true;
			}


			model.addAttribute("ShouldExpandRole", ShouldExpandRole);

			List<Formation> formation=formationRepository.findReservedTraining(client.getId());
			model.addAttribute("myformation",formation);

			model.addAttribute("categories", categoryRepository.findAll());
			model.addAttribute("cities", cityRepository.findAll());

			return "user-profile";

		}
		model.addAttribute("local",new Local());

		model.addAttribute("categoriesLocal", categorylocalRepository.findAll());
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

		return "Local-Listing";		
	}

	@RequestMapping(value="/saveLocal", method =RequestMethod.POST)
	public String saveLocal(Model model,@Valid Local local,
			@RequestParam("category") String category,
			@RequestParam("ville") String city,
			@RequestParam("disponibiliteFrom") java.sql.Date dateFrom,
			@RequestParam(name="photo1") MultipartFile file1,
			@RequestParam(name="photo2") MultipartFile file2,
			@RequestParam(name="photo3") MultipartFile file3,
			@RequestParam(name="photo4") MultipartFile file4,
			@RequestParam(name="photo5") MultipartFile file5,
			@RequestParam(name="photo6") MultipartFile file6, 
			HttpServletRequest request) throws IllegalStateException, IOException {

		HttpSession session=request.getSession(true);
		local.setDisponibiliteFrom(dateFrom);
		local.setCategory(category);
		local.setVille(city);
		local.setOwner((Client) session.getAttribute("user"));
		Client client= (Client) session.getAttribute("user");

		String message="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi "+client.getNom()+" "+client.getPrenom()+"</h1>"+
				"<p>" + 
				"	    Thank you for adding a new local. We will make sure your local article appears to the maximum of participants."+
				"</p>"+
				"<p>See you soon.</p></div></div>";;


				local.setPicture1(file1.getOriginalFilename());
				local.setPicture2(file2.getOriginalFilename());
				local.setPicture3(file3.getOriginalFilename());
				local.setPicture4(file4.getOriginalFilename());
				local.setPicture5(file5.getOriginalFilename());
				local.setPicture6(file6.getOriginalFilename());

				localRepository.save(local);

				local.setPicture1(file1.getOriginalFilename());
				file1.transferTo(new File(localImageDir+local.getId()+"_1"));


				local.setPicture2(file2.getOriginalFilename());
				file2.transferTo(new File(localImageDir+local.getId()+"_2"));


				local.setPicture3(file3.getOriginalFilename());
				file3.transferTo(new File(localImageDir+local.getId()+"_3"));


				local.setPicture4(file4.getOriginalFilename());
				file4.transferTo(new File(localImageDir+local.getId()+"_4"));


				local.setPicture5(file5.getOriginalFilename());
				file5.transferTo(new File(localImageDir+local.getId()+"_5"));

				local.setPicture6(file6.getOriginalFilename());
				file6.transferTo(new File(localImageDir+local.getId()+"_6"));

				String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
						"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
						"<p>" + 
						"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just added a new local: "+
						"</p>"+
						"<table>"
						+ "<tbody>"
						+ "<tr>"
						+ "<td><strong>Name: </strong></td>"+"<td>"+local.getIntitulee()+"</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Surface: </strong></td>"+"<td>"+local.getSuperficie()+" m²</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Price Per Hour: </strong></td>"+"<td>"+local.getPrixParHeure()+" $</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Type: </strong></td>"+"<td>"+local.getCategory()+"</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Address: </strong></td>"+"<td>"+local.getAdresse()+", "+local.getVille()+"</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Description: </strong></td>"+"<td>"+local.getDescription()+"</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td><strong>Other Properties: </strong></td>"+"<td>"+"Chairs: "+local.getChairs()+", Electrical Outlets: "+local.getPrises()+", Microphones: "+local.getMicro()+", Projectors: "+local.getProjecteur()+'.'+"</td>"
						+ "</tr>"
						+ "</tbody>"+"<p>Go check other information.</p>";

				try {
					notificationService.sendNotification(client,message);
					notificationService.sendNotificationToAdmin(messageToAdmin);
				} catch (Exception e) {

				}
				model.addAttribute("categoriesLocal", categorylocalRepository.findAll());
				model.addAttribute("categories", categoryRepository.findAll());
				model.addAttribute("cities", cityRepository.findAll());

				return "redirect:EditAds";	
	}

	@RequestMapping(value="/editlocal", method =RequestMethod.GET)
	public String editlocal(Model model, Long id) throws IllegalStateException, IOException {
		Local local =localRepository.getOne(id);
		model.addAttribute("local",local );
		picture1=local.getPicture1();
		picture2=local.getPicture2();

		picture3=local.getPicture3();
		picture4=local.getPicture4();
		picture5=local.getPicture5();
		picture6=local.getPicture6();

		model.addAttribute("categoriesLocal", categorylocalRepository.findAll());
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

		return "Update-Local";
	}

	@RequestMapping(value="/updateLocal", method =RequestMethod.POST)
	public String updateLocal(Model model, Local local,
			@RequestParam("category") String category,
			@RequestParam("ville") String ville,
			@RequestParam("disponibiliteFrom") java.sql.Date dateFrom,
			@RequestParam(name="photo1") MultipartFile file1,
			@RequestParam(name="photo2") MultipartFile file2,
			@RequestParam(name="photo3") MultipartFile file3,
			@RequestParam(name="photo4") MultipartFile file4,
			@RequestParam(name="photo5") MultipartFile file5,
			@RequestParam(name="photo6") MultipartFile file6, 
			HttpServletRequest request) throws IllegalStateException, IOException {

		local.setPicture1(file1.getOriginalFilename());
		local.setPicture2(file2.getOriginalFilename());
		local.setPicture3(file3.getOriginalFilename());
		local.setPicture4(file4.getOriginalFilename());
		local.setPicture5(file5.getOriginalFilename());
		local.setPicture6(file6.getOriginalFilename());


		if((file1.isEmpty())) {

			local.setPicture1(picture1);

		}
		else {
			local.setPicture1(file1.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_1");
			if(f.exists()) {
				byte[] bytes=file1.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_1");
				Files.write(path, bytes);		
			}
			else 
			{

				file1.transferTo(new File(localImageDir+local.getId()+"_1"));
			}

		}

		if((file2.isEmpty())) {

			local.setPicture2(picture2);

		}

		else {
			local.setPicture1(file2.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_2");
			if(f.exists()) {
				byte[] bytes=file2.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_2");
				Files.write(path, bytes);		
			}
			else 
			{

				file2.transferTo(new File(localImageDir+local.getId()+"_2"));
			}

		}

		if((file3.isEmpty())) {

			local.setPicture3(picture3);

		}

		else {
			local.setPicture1(file3.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_3");
			if(f.exists()) {
				byte[] bytes=file3.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_3");
				Files.write(path, bytes);		
			}
			else 
			{

				file3.transferTo(new File(localImageDir+local.getId()+"_3"));
			}

		}

		if((file4.isEmpty())) {

			local.setPicture4(picture4);

		}

		else {
			local.setPicture1(file4.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_4");
			if(f.exists()) {
				byte[] bytes=file4.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_4");
				Files.write(path, bytes);		
			}
			else 
			{

				file4.transferTo(new File(localImageDir+local.getId()+"_4"));
			}

		}

		if((file5.isEmpty())) {

			local.setPicture5(picture5);

		}

		else {
			local.setPicture1(file5.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_5");
			if(f.exists()) {
				byte[] bytes=file5.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_5");
				Files.write(path, bytes);		
			}
			else 
			{

				file5.transferTo(new File(localImageDir+local.getId()+"_5"));
			}

		}

		if((file6.isEmpty())) {

			local.setPicture6(picture6);

		}

		else {
			local.setPicture1(file6.getOriginalFilename());
			File f=new File(localImageDir+local.getId()+"_6");
			if(f.exists()) {
				byte[] bytes=file6.getBytes();
				Path path=Paths.get(localImageDir+local.getId()+"_6");
				Files.write(path, bytes);		
			}
			else 
			{

				file6.transferTo(new File(localImageDir+local.getId()+"_6"));
			}

		}






		HttpSession session=request.getSession(true);
		Client client =(Client) session.getAttribute("user");
		local.setDisponibiliteFrom(dateFrom);
		local.setCategory(category);
		local.setVille(ville);
		local.setOwner((Client) session.getAttribute("user"));




		localRepository.save(local);

		String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
				"<p>" + 
				"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just updated his local: "+
				"</p>"+
				"<table>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td><strong>Name: </strong></td>"+"<td>"+local.getIntitulee()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Surface: </strong></td>"+"<td>"+local.getSuperficie()+" m²</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Price Per Hour: </strong></td>"+"<td>"+local.getPrixParHeure()+" $</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Type: </strong></td>"+"<td>"+local.getCategory()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Address: </strong></td>"+"<td>"+local.getAdresse()+", "+local.getVille()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Description: </strong></td>"+"<td>"+local.getDescription()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Other Properties: </strong></td>"+"<td>"+"Chairs: "+local.getChairs()+", Electrical Outlets: "+local.getPrises()+", Microphones: "+local.getMicro()+", Projectors: "+local.getProjecteur()+'.'+"</td>"
				+ "</tr>"
				+ "</tbody>"+"<p>Go check other information.</p>";
		List<String> clientsEmails= localRepository.findParticipants(local.getId());

		try {
			notificationService.sendNotificationIfArticleUpdated(clientsEmails, local);
			notificationService.sendNotificationToAdmin(messageToAdmin);
		} catch (Exception e) {

		}
		model.addAttribute("categoriesLocal", categorylocalRepository.findAll());
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

		return "redirect:EditAds";	
	}


	@RequestMapping(value="getLocalPhoto",produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] getLocalPhoto(Long id,Long subId) throws FileNotFoundException, IOException {
		File f=new File(localImageDir+id+"_"+subId);
		return IOUtils.toByteArray(new FileInputStream(f));
	}

	@RequestMapping(value="ListLocals")
	public List<Local> ListeLocals(Long uId) {

		List<Local> local= localRepository.findByUserId(uId);


		return local;

	}


	@RequestMapping(value="/deletelocal", method =RequestMethod.GET)
	public String deletelocal(Long id,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Client client =(Client) session.getAttribute("user");
		Local local = localRepository.getOne(id);
		String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
				"<p>" + 
				"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just deleted his local: "+
				"</p>"+
				"<table>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td><strong>Name: </strong></td>"+"<td>"+local.getIntitulee()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Surface: </strong></td>"+"<td>"+local.getSuperficie()+" m²</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Price Per Hour: </strong></td>"+"<td>"+local.getPrixParHeure()+" $</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Type: </strong></td>"+"<td>"+local.getCategory()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Address: </strong></td>"+"<td>"+local.getAdresse()+", "+local.getVille()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Description: </strong></td>"+"<td>"+local.getDescription()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Other Properties: </strong></td>"+"<td>"+"Chairs: "+local.getChairs()+", Electrical Outlets: "+local.getPrises()+", Microphones: "+local.getMicro()+", Projectors: "+local.getProjecteur()+'.'+"</td>"
				+ "</tr>"
				+ "</tbody>";
		List<String> clientsEmails= localRepository.findParticipants(id);
		try {
			notificationService.sendNotificationIfArticleRemoved(clientsEmails, local);
			notificationService.sendNotificationToAdmin(messageToAdmin);
		} catch (Exception e) {

		}
		localRepository.localDeleted(id);
		localRepository.deleteById(id);



		return "redirect:EditAds";
	}
	@RequestMapping(value="/findAll", method =RequestMethod.GET)
	public List<Local> findAll(){
		return localRepository.findAll();
	}

	@RequestMapping(value="/findAllToAdd", method =RequestMethod.GET)
	public List<Local> findAllToAdd(){
		return localRepository.findAll();
	}
	@RequestMapping(value="/findByCity", method =RequestMethod.GET)
	public List<Local> findByCity(String ville){
		return localRepository.findByCity(ville);
	}
	@RequestMapping(value="/findById", method =RequestMethod.GET)
	public Local findById(Long id){
		return localRepository.getOne(id);
	}





}

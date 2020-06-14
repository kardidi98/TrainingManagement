package org.sid.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;


import org.apache.commons.io.IOUtils;
import org.sid.dao.*;
import org.sid.entities.Admin;
import org.sid.entities.Client;
import org.sid.entities.Commentaire;
import org.sid.entities.Formation;
import org.sid.entities.Local;
import org.sid.entities.Rating;
import org.sid.mailSender.NotificationService;
import org.sid.services.LocalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
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
	private NotificationService notificationService;
	
	@Autowired
	private LocalService localService;

	private String TrainingPicture;
	@Autowired
	private FormationRepository formationRepository;

	@Autowired
	private CommentaireController commentaireController;

	@Autowired
	private RatingRepository ratingRepository;

	@Autowired
	private LocalController localController;
	
	@Autowired
	private CityRepository cityRepository;
	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Value("${dir.images}")
	private String imageDir;

	@Value("${dir.Localimages}")
	private String localimageDir;
	
	@Value("${dir.userimages}")
	private String userimageDir;

	@RequestMapping(value="/TrainingManagement", method = RequestMethod.GET)
	public String home(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Object obj=session.getAttribute("user");
		if(session.getAttribute("user")!=null && obj instanceof Admin) {
			return "redirect:/";
		}
		//***********************************Creation des fichiers images*************************************
		
		 Path path1 = Paths.get(imageDir);
		 Path path2 = Paths.get(localimageDir);
		 Path path3 = Paths.get(userimageDir);
		
		 if (!Files.exists(path1)) {
	            try {
	                Files.createDirectories(path1);
	            } catch (IOException e) {
	                //fail to create directory
	                e.printStackTrace();
	            }
	        }
		 if (!Files.exists(path2)) {
	            try {
	                Files.createDirectories(path1);
	            } catch (IOException e) {
	                //fail to create directory
	                e.printStackTrace();
	            }
	        }
		 if (!Files.exists(path3)) {
	            try {
	                Files.createDirectories(path1);
	            } catch (IOException e) {
	                //fail to create directory
	                e.printStackTrace();
	            }
	        }
		 
		//***********************************Creation des fichiers images*************************************
		List<Formation> trendyTrainings=formationRepository.trendyTrainings();

		List<Commentaire> commentaires=commentaireController.findRecent(5);
		model.addAttribute("commentaires", commentaires);
		model.addAttribute("commentaire",new Commentaire());
		


		List<Client> TrainerList =clientRepository.findTrainers();


		model.addAttribute("trendyTrainings", trendyTrainings);
		model.addAttribute("TrainerList", TrainerList);
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		
		model.addAttribute("session", session.getAttribute("user"));

		return "index";		
	}
	@RequestMapping(value="/AddArticle", method = RequestMethod.GET)
	public String AddArticle(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		List<Local> local=localController.findAllToAdd();

		if(session.getAttribute("user")==null) {
			return "login";
		}
		model.addAttribute("formation",new Formation());
		model.addAttribute("locaux", local);
		
		List<Local> allLocals= localService.getLocals();
		List<String> localVilles=localService.getLocalsVilles();
		List<String> localCategories=localService.getLocalsCategories();
		model.addAttribute("locaux",allLocals);
		model.addAttribute("localVilles",localVilles);
		model.addAttribute("localCategories",localCategories);
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

//		model.addAttribute("activeTraining",formationRepository.findTrainingLocal());
		return "Ad-listing";		
	}


	@RequestMapping(value="/save", method =RequestMethod.POST)
	public String save(Model model,HttpServletRequest request, Formation formation,@RequestParam("LocalId") Long localId,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		HttpSession session =request.getSession(true);
		Client client=(Client) session.getAttribute("user");

		String message="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi "+client.getNom()+" "+client.getPrenom()+"</h1>"+
				"<p>" + 
				"	    Thank you for adding a new training. We will make sure your training article appears to the maximum of participants."+
				"</p>"+
				"<p>See you soon.</p></div></div>";

		formation.setUser(client);
		Local local=localController.findById(localId);

		formation.setLocal(local);
		formation.setEtat("Active");
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
		
		String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
				"<p>" + 
				"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just added a new training:"+
				"</p>"+
				"<table>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td><strong>Title:</strong> </td>"+"<td>"+formation.getTitle()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Location: </strong></td>"+"<td>"+formation.getLocal().getAdresse()+", "+formation.getLocal().getVille()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Price: </strong></td>"+"<td>"+formation.getPrix()+" $</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Category: </strong></td>"+"<td>"+formation.getArticleCat()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Requirements: </strong></td>"+"<td>"+formation.getRequirements()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Difficuty: </strong></td>"+"<td>"+formation.getDifficulty()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Description: </strong></td>"+"<td>"+formation.getDescription()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>From: </strong></td>"+"<td>"+formation.getFirstDay()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>To: </strong></td>"+"<td>"+formation.getLastDay()+"</td>"
				+ "</tr>"
				+ "</tbody>"+"<p>Go check other information.</p>";

	

		try {
			notificationService.sendNotification(client,message);
			notificationService.sendNotificationToAdmin(messageToAdmin);
		} catch (Exception e) {

		}

		return "redirect:EditAds";	
	}

	@RequestMapping(value="/listFormation", method =RequestMethod.GET)
	public String listFormation(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page) {
		//		ByCategory=false;
		//		ByCity=false;
		//		FromSearch=false;
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findByEtat();
		List<Formation> countformation=formationRepository.findByEtat();
		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		
		
		model.addAttribute("formation",formation);
		model.addAttribute("count",countformation.size());

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		
		for (Formation f : formation) {
			if(formationRepository.getOne(f.getId()).getCanStart()==1) {
				
				try {
					notificationService.sendNotificationIfArticleHasReachedMin(f.getUser().getEmail(), f);

					
				} catch (Exception e) {

				}
			}
		}
		
		return "category";
	}




	@RequestMapping(value="/listFormationParCategory", method =RequestMethod.GET)
	public String listFormationParCategory(Model model,HttpServletRequest request,@RequestParam(name="cat",defaultValue = "") String cat,@RequestParam(name="page",defaultValue = "0") int page) {
		//		ByCategory=true;
		//		ByCity=false;
		//		FromSearch=false;
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findByArticleCat(cat);
		Long countFormation = formationRepository.countByArticleCat(cat);


		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		
		model.addAttribute("formation",formation);
		model.addAttribute("count",countFormation);

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "category";
	}


	@RequestMapping(value="/listFormationParVille", method =RequestMethod.GET)
	public String listFormationParVille(Model model,HttpServletRequest request,@RequestParam(name="city",defaultValue = "") String city,@RequestParam(name="page",defaultValue = "0") int page) {
		//		ByCategory=false;
		//		ByCity=true;
		//		FromSearch=false;
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findByArticleCity(city);
		Long countFormation = formationRepository.countByArticleCity(city);


		List<Client> TrainerList =clientRepository.findTrainers();

		
		model.addAttribute("formation",formation);
		model.addAttribute("count",countFormation);

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "category";
	}
	
	@RequestMapping(value="/Search", method =RequestMethod.GET)
	public String Search(Model model,HttpServletRequest request,@RequestParam(name="Title",defaultValue = "") String title) {
		//		ByCategory=true;
		//		ByCity=false;
		//		FromSearch=false;
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findByTitle(title);
		Long countFormation = formationRepository.countByTitle(title);


		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		
		model.addAttribute("formation",formation);
		model.addAttribute("count",countFormation);

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("key", title);
		
		model.addAttribute("session", session.getAttribute("user"));

		return "category";
	}

	@RequestMapping(value="/countByCategory")
	@ResponseBody
	public Long countByCategory(String cat) {

		return  formationRepository.countByArticleCat(cat);
	}


	@RequestMapping(value="/EditAds", method =RequestMethod.GET)
	public String EditMyAds(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Client  client = (Client) session.getAttribute("user");
		if(session.getAttribute("user")==null) {
			return "login";
		}
		List<Formation> formation=formationRepository.findByUserId(client.getId());

		model.addAttribute("myformation",formation);

		List<Local> local=localController.ListeLocals(client.getId());

		model.addAttribute("local",local);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "dashboard-my-ads";
	}

	@RequestMapping(value="getPhoto",produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] getPhoto(Long id) throws FileNotFoundException, IOException {
		File f=new File(imageDir+id);
		return IOUtils.toByteArray(new FileInputStream(f));
	}


	@RequestMapping(value="/delete", method =RequestMethod.GET)
	public String delete(Long id,HttpServletRequest request) {
		
		HttpSession session=request.getSession(true);
		Client client =(Client) session.getAttribute("user");
		Formation formation=formationRepository.getOne(id);
		String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
				"<p>" + 
				"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just deleted his training: "+
				"</p>"+
				"<table>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td><strong>Title:</strong> </td>"+"<td>"+formation.getTitle()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Location: </strong></td>"+"<td>"+formation.getLocal().getAdresse()+", "+formation.getLocal().getVille()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Price: </strong></td>"+"<td>"+formation.getPrix()+" $</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Category: </strong></td>"+"<td>"+formation.getArticleCat()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Requirements: </strong></td>"+"<td>"+formation.getRequirements()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Difficuty: </strong></td>"+"<td>"+formation.getDifficulty()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Description: </strong></td>"+"<td>"+formation.getDescription()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>From: </strong></td>"+"<td>"+formation.getFirstDay()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>To: </strong></td>"+"<td>"+formation.getLastDay()+"</td>"
				+ "</tr>"
				+ "</tbody>";
		
		

		List<String> clientsEmails= formationRepository.findParticipants(id);

		try {
			notificationService.sendNotificationIfArticleRemoved(clientsEmails, formation);
			notificationService.sendNotificationToAdmin(messageToAdmin);
		} catch (Exception e) {

		}
		formationRepository.deleteById(id);
		formationRepository.deleteRequests(id);
		return "redirect:EditAds";
	}

	@RequestMapping(value="/viewArticle", method =RequestMethod.GET)
	public String viewArticle(Model model,HttpServletRequest request,Long id) {
		HttpSession session=request.getSession(true);
		Client client =(Client) session.getAttribute("user");



		Formation article= formationRepository.getOne(id);
		Client formateur=article.getUser();
		Long countFormation=formationRepository.countByIdFormation(id);
		model.addAttribute("countAvailablePlaces",article.getNbPlaces()-countFormation);
		model.addAttribute("article",article);
		model.addAttribute("formateur",formateur);
		Long Duree=article.getLastDay().getTime()-article.getFirstDay().getTime();
		Long Duration=(long) (Duree*(1.15741*Math.pow(10,-8))+1);
		model.addAttribute("Duration",Duration);

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "single";

	}
	
	

	@RequestMapping(value="/editArticle", method =RequestMethod.GET)
	public String editArticle(Model model,Long id,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page) {
		HttpSession session=request.getSession(true);
		Client client=(Client) session.getAttribute("user");
		Formation a= formationRepository.getOne(id);

		model.addAttribute("article",a);
		List<Local> local=localController.findAll();
		
		model.addAttribute("locaux",local);
		TrainingPicture=a.getSignificantPhoto();
		List<Local> allLocals= localService.getLocals();
		List<String> localVilles=localService.getLocalsVilles();
		List<String> localCategories=localService.getLocalsCategories();
		model.addAttribute("locaux",allLocals);
		model.addAttribute("localVilles",localVilles);
		model.addAttribute("localCategories",localCategories);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "UpdateArticle";
	}


	@RequestMapping(value="/UpdateArticle", method =RequestMethod.POST)

	public String update(Model model,HttpServletRequest request, Formation formation,@RequestParam("localId") Long localId,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		HttpSession session =request.getSession(true);
		Client client=(Client) session.getAttribute("user");
		formation.setUser((Client) session.getAttribute("user"));

		Local local=localController.findById(localId);
		formation.setLocal(local);
		if(bindingResult.hasErrors()) {
			return "redirect:editArticle";
		}
		if(!(file.isEmpty())) {

			formation.setSignificantPhoto(file.getOriginalFilename());


		}
		if((file.isEmpty())) {

			formation.setSignificantPhoto(TrainingPicture);

		}

		if(!(file.isEmpty())) {
			formation.setSignificantPhoto(file.getOriginalFilename());
			File f=new File(imageDir+formation.getId());
			if(f.exists()) {
				byte[] bytes=file.getBytes();
				Path path=Paths.get(imageDir+formation.getId());
				Files.write(path, bytes);		
			}
			else 
			{

				file.transferTo(new File(imageDir+formation.getId()));
			}

		}

		formationRepository.save(formation);
		String messageToAdmin="<div class='container'><div style='text-align:center;'><h1 style='color:blue;'>Training Management</h1></div>"+
				"<div style='color: black;box-shadow:0 0 10px rgba(0, 0, 0, 0.5);border-radius:5px;'><h1>Hi Administators</h1>"+
				"<p>" + 
				"<strong>Mr. "+client.getNom()+" "+client.getPrenom()+"</strong> has just updated his training: "+
				"</p>"+
				"<table>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td><strong>Title:</strong> </td>"+"<td>"+formation.getTitle()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Location: </strong></td>"+"<td>"+formation.getLocal().getAdresse()+", "+formation.getLocal().getVille()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Price: </strong></td>"+"<td>"+formation.getPrix()+" $</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Category: </strong></td>"+"<td>"+formation.getArticleCat()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Requirements: </strong></td>"+"<td>"+formation.getRequirements()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Difficuty: </strong></td>"+"<td>"+formation.getDifficulty()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>Description: </strong></td>"+"<td>"+formation.getDescription()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>From: </strong></td>"+"<td>"+formation.getFirstDay()+"</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td><strong>To: </strong></td>"+"<td>"+formation.getLastDay()+"</td>"
				+ "</tr>"
				+ "</tbody>"+"<p>Go check other information.</p>";
		
		
		
		List<String> clientsEmails= formationRepository.findParticipants(formation.getId());

		try {
			notificationService.sendNotificationIfArticleUpdated(clientsEmails, formation);

			notificationService.sendNotificationToAdmin(messageToAdmin);
		} catch (Exception e) {

		}

		return "redirect:EditAds";	
	}


	//********  Pour la reservation des formations ***************

	@RequestMapping(value="/SendRequest", method =RequestMethod.GET)
	public String SendRequest(Model model,Long id,HttpServletRequest request) throws SQLException {
		HttpSession session=request.getSession(true);
		Client client=(Client) session.getAttribute("user");
		Formation formation=formationRepository.getOne(id);
		Long countFormation=formationRepository.countByIdFormation(id);
		List<Long> verifyIfExist=formationRepository.verifyIfAlreadyExist(client.getId(),id);
		model.addAttribute("count", countFormation);
		if(verifyIfExist.size()==0) {
			if(formation.equals(null)) {
				
				formationRepository.insertIntoReservation(client.getId(),id);
				
				return "redirect:listFormation";
			}
			else {
				
				if(formation.getNbPlaces()>countFormation) {
					
					
					formationRepository.insertIntoReservation(client.getId(),id);
					
					
					
					
					return "redirect:TrainingList?id="+id;
				}
				else return "redirect:PlacesPleines";
			}
		}
		else
			return "redirect:DejaPostuler";

	}
	
	@RequestMapping(value="/TrainingList", method =RequestMethod.GET)
	public String TrainingList(Model model,HttpServletRequest request,Long id) {
		Formation formation =formationRepository.getOne(id);
		if(formation.getCanStart()==1) {
			
			try {
				notificationService.sendNotificationIfArticleHasReachedMin(formation.getUser().getEmail(), formation);

				
			} catch (Exception e) {

			}
		}
		
		
		return "redirect:listFormation";
	}

	@RequestMapping(value="/DejaPostuler", method =RequestMethod.GET)
	public String DejaPostuler(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findAll();
		
		model.addAttribute("formation",formation);
		if(session.getAttribute("user")==null) return "CategoryVisiteur";

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		model.addAttribute("count", formation.size());
		return "DejaPostuler";
	}

	@RequestMapping(value="/PlacesPleines", method =RequestMethod.GET)
	public String PlacesPleines(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findAll();
		model.addAttribute("formation",formation);
		if(session.getAttribute("user")==null) return "CategoryVisiteur";

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		model.addAttribute("count", formation.size());
		return "PlacesPleines";
	}
	@RequestMapping(value="/editUserProfile",method=RequestMethod.GET)
	public String editUserProfile(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Client client=(Client) session.getAttribute("user");

		if(session.getAttribute("user")==null) {
			return "login";
		}
		List<Formation> formation=formationRepository.findReservedTraining(client.getId());
		model.addAttribute("myformation",formation);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "user-profile";
	}

	@RequestMapping(value="/deleteMyReservation", method =RequestMethod.GET)
	public String deleteMyReservation(Long tId,Long uId) {

		//System.out.println(tId);
		formationRepository.deleteMyReservation(tId,uId);
		return "redirect:editUserProfile";
	}

	//********  Pour la reservation des formations ***************


	/*---------------------------------RechercheFormation---------------------------------------------*/

	@RequestMapping(value="/ChercherFormation" ,method=RequestMethod.POST)
	public String ChercherFormation(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page,@RequestParam("Trainer") String Trainer,@RequestParam("Location") String Location,@RequestParam(name="Category",defaultValue = "All") String Category) {
		//		ByCategory=false;
		//		ByCity=false;
		//		FromSearch=true;

		List<Formation> formation=formationRepository.rechercherformation(Trainer,Location,Category);
		List<Formation> countformation=formationRepository.findNomberTrainings(Trainer,Location,Category);
		HttpSession session=request.getSession(true);

		List<Client> TrainerList =clientRepository.findTrainers();

		
		model.addAttribute("formation",formation);

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);

		model.addAttribute("count",countformation.size());
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "category";




	}
	@RequestMapping(value="/chercherformation" ,method=RequestMethod.GET)
	public String cherchercormation(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page,@RequestParam("Trainer") String Trainer,@RequestParam("Location") String Location,@RequestParam(name="Category",defaultValue = "All") String Category) {
		//		ByCategory=false;
		//		ByCity=false;
		//		FromSearch=true;

		List<Formation> formation=formationRepository.rechercherformation(Trainer,Location,Category);
		List<Formation> countformation=formationRepository.findNomberTrainings(Trainer,Location,Category);
		HttpSession session=request.getSession(true);

		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

	
		model.addAttribute("formation",formation);

		//		model.addAttribute("ByCategory",ByCategory);
		//		model.addAttribute("ByCity",ByCity);
		//		model.addAttribute("FromSearch",FromSearch);

		model.addAttribute("count",countformation.size());
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "category";




	}

	@RequestMapping(value="/advancedSearch",method=RequestMethod.GET)
	public String advancedSearch(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page) {
		//		FromAdvancedSearch=false;
		HttpSession session=request.getSession(true);
		List<Formation> formation=formationRepository.findByEtat();
		List<Formation> countformation=formationRepository.findByEtat();
		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		model.addAttribute("formation",formation);
		model.addAttribute("count",countformation.size());
		//		model.addAttribute("FromAdvancedSearch",FromAdvancedSearch);

		model.addAttribute("session", session.getAttribute("user"));


		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "RechercheAvancee";
	}

	@RequestMapping(value="/AdvancedResearch",method=RequestMethod.POST)
	public String AdvancedResearch(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page,@RequestParam(name="StartDate",defaultValue = "2020-01-01") Date StartDate,@RequestParam(name="EndDate",defaultValue = "2020-01-01") Date EndDate,@RequestParam(name="Category") String Category,@RequestParam(name="Difficulty") String Difficulty,@RequestParam(name="Rating", defaultValue = "0") int Rating,@RequestParam("City") String City,@RequestParam("TypeLocal") String TypeLocal,@RequestParam("Trainer") String Trainer,@RequestParam("MinPrice") int MinPrice,@RequestParam("MaxPrice")int MaxPrice ) {
		//		FromAdvancedSearch=true;
		List<Formation> formation=formationRepository.rechercherformationAvancee(StartDate, EndDate, Category, Difficulty, Rating, City, TypeLocal, Trainer, MinPrice, MaxPrice);
		List<Formation> countformation = formationRepository.countResultFormation(StartDate, EndDate, Category, Difficulty, Rating, City, TypeLocal, Trainer, MinPrice, MaxPrice);
		HttpSession session=request.getSession(true);

		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		
		model.addAttribute("formation",formation);

		model.addAttribute("count",countformation.size());


		model.addAttribute("MinPrice",MinPrice);
		model.addAttribute("MaxPrice",MaxPrice);
		model.addAttribute("Rating",Rating);
		//		model.addAttribute("FromAdvancedSearch",FromAdvancedSearch);

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));

		return "RechercheAvancee";
	}
	@RequestMapping(value="/advancedresearch",method=RequestMethod.GET)
	public String advancedresearch(Model model,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page,@RequestParam(name="StartDate",defaultValue = "2020-01-01") Date StartDate,@RequestParam(name="EndDate",defaultValue = "2020-01-01") Date EndDate,@RequestParam(name="Category") String Category,@RequestParam(name="Difficulty") String Difficulty,@RequestParam(name="Rating", defaultValue = "0") int Rating,@RequestParam("City") String City,@RequestParam("TypeLocal") String TypeLocal,@RequestParam("Trainer") String Trainer,@RequestParam("MinPrice") int MinPrice,@RequestParam("MaxPrice")int MaxPrice ) {
		//		FromAdvancedSearch=true;
		List<Formation> formation=formationRepository.rechercherformationAvancee(StartDate, EndDate, Category, Difficulty, Rating, City, TypeLocal, Trainer, MinPrice, MaxPrice);
		List<Formation> countformation = formationRepository.countResultFormation(StartDate, EndDate, Category, Difficulty, Rating, City, TypeLocal, Trainer, MinPrice, MaxPrice);
		HttpSession session=request.getSession(true);

		List<Client> TrainerList =clientRepository.findTrainers();

		model.addAttribute("TrainerList", TrainerList);

		
		model.addAttribute("formation",formation);
		//		model.addAttribute("FromAdvancedSearch",FromAdvancedSearch);
		model.addAttribute("MinPrice",MinPrice);
		model.addAttribute("MaxPrice",MaxPrice);
		model.addAttribute("Rating",Rating);
		model.addAttribute("count",countformation.size());

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

		model.addAttribute("session", session.getAttribute("user"));

		return "RechercheAvancee";
	}
	/*---------------------------------RechercheFormation---------------------------------------------*/



}

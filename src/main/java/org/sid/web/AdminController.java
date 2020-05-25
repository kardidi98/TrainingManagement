package org.sid.web;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.sid.dao.AdminRepository;
import org.sid.dao.ClientRepository;
import org.sid.dao.FormationRepository;
import org.sid.dao.LocalRepository;
import org.sid.entities.Admin;
import org.sid.entities.Client;
import org.sid.entities.Commentaire;
import org.sid.entities.Formation;
import org.sid.entities.Local;
import org.sid.mailSender.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class AdminController {

	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private FormationRepository formationRepository;
	@Autowired
	private LocalRepository localRepository;
	@Autowired
	private LocalController localController;
	@Autowired
	private NotificationService notificationService;
	
	private String TrainingPicture;

	@Value("${dir.userimages}")
	private String userimageDir;
	
	@Value("${dir.images}")
	private String imageDir;
	@Value("${dir.Localimages}")
	private String localImageDir;

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


		model.addAttribute("onestar", (adminRepository.countByStarRatings(1)/adminRepository.countUsersFromRating())*100);
		model.addAttribute("twostars", (adminRepository.countByStarRatings(2)/adminRepository.countUsersFromRating())*100);
		model.addAttribute("treestars", (adminRepository.countByStarRatings(3)/adminRepository.countUsersFromRating())*100);
		model.addAttribute("fourstars", (adminRepository.countByStarRatings(4)/adminRepository.countUsersFromRating())*100);
		model.addAttribute("fivestars", (adminRepository.countByStarRatings(5)/adminRepository.countUsersFromRating())*100);

		model.addAttribute("countusers", countUsers);

		
		
		
		model.addAttribute("session", session.getAttribute("user"));

		return "admin";		
	}

	@RequestMapping(value="/Adminlogin", method = RequestMethod.POST)
	public String login(Model model,HttpServletRequest request,@RequestParam("email") String email, @RequestParam("password")  String password) {

		HttpSession session=request.getSession(true);
		String error=null;
		Admin Admin = adminRepository.findByEmail(email);

		if(Admin==null) {
			error="Invalid Information ! Check your email.";
			model.addAttribute("error",error);
			return "admin";
		}
		else {
			if(Admin.getPassword().equals(password)) {
				session=request.getSession(true);
				session.setAttribute("user", Admin);
			}
			else{
				error="Invalid Information ! Check your password.";
				model.addAttribute("error",error);
				return "admin";
			}
		}


		model.addAttribute("error",error);
		model.addAttribute("session", session.getAttribute("user"));

		return "redirect:/";		
	}


	@RequestMapping(value="getPicture",produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] getPicture(Long id) throws FileNotFoundException, IOException {
		File f=new File(userimageDir+id);
		return IOUtils.toByteArray(new FileInputStream(f));
	}
	@RequestMapping(value="/logout",method=RequestMethod.GET)
	public String logout(Model model,HttpSession session) {
		session.invalidate();

		return "redirect:/";
	}
	



	@RequestMapping(value="/deleteTraining", method =RequestMethod.GET)
	public String deleteTraining(Long id) {


		Formation formation=formationRepository.getOne(id);

		List<String> clientsEmails= formationRepository.findParticipants(id);
		String email =formation.getUser().getEmail();

		try {
			notificationService.sendNotificationToTrainer(email, formation);
			notificationService.sendNotificationIfArticleRemoved(clientsEmails, formation);
		} catch (Exception e) {

		}
		
		formationRepository.deleteById(id);
		formationRepository.deleteRequests(id);
		return "redirect:/";
	}

	@RequestMapping(value="/viewTraining", method =RequestMethod.GET)
	public String viewTraining(Model model,HttpServletRequest request,Long id) {
		HttpSession session=request.getSession(true);
		



		Formation article= formationRepository.getOne(id);
		Client formateur=article.getUser();
		Long countFormation=formationRepository.countByIdFormation(id);
		model.addAttribute("countAvailablePlaces",article.getNbPlaces()-countFormation);
		model.addAttribute("article",article);
		model.addAttribute("formateur",formateur);
		Long Duree=article.getLastDay().getTime()-article.getFirstDay().getTime();
		Long Duration=(long) (Duree*(1.15741*Math.pow(10,-8)));
		model.addAttribute("Duration",Duration);

		model.addAttribute("session", session.getAttribute("user"));

		return "singleForAdmin";

	}
	@RequestMapping(value="getLocalPicture",produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] getLocalPicture(Long id,Long subId) throws FileNotFoundException, IOException {
		File f=new File(localImageDir+id+"_"+subId);
		return IOUtils.toByteArray(new FileInputStream(f));
	}

	@RequestMapping(value="/editTraining", method =RequestMethod.GET)
	public String editTraining(Model model,Long id,HttpServletRequest request,@RequestParam(name="page",defaultValue = "0") int page) {
		HttpSession session=request.getSession(true);
		Formation a= formationRepository.getOne(id);

		model.addAttribute("article",a);
		List<Local> local=localController.findAll();
		
		model.addAttribute("locaux",local);
		TrainingPicture=a.getSignificantPhoto();
		return "UpdateArticleForAdmin";
	}


	@RequestMapping(value="/UpdateTraining", method =RequestMethod.POST)

	public String update(Model model,HttpServletRequest request, Formation formation,@RequestParam("localId") Long localId,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		HttpSession session =request.getSession(true);
		formation.setUser((Client) formation.getUser());

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
		List<String> clientsEmails= formationRepository.findParticipants(formation.getId());

		try {
			notificationService.sendNotificationIfArticleUpdated(clientsEmails, formation);
		} catch (Exception e) {

		}

		return "redirect:viewTraining?id="+formation.getId();	
	}
	
	@RequestMapping(value="/BlockUser", method =RequestMethod.GET)
	public String BlockUser(Model model,HttpServletRequest request,Long id) {
		adminRepository.blockUser(id);
		return "redirect:Customers";
	}
	
	@RequestMapping(value="/UnblockUser", method =RequestMethod.GET)
	public String UnblockUser(Model model,HttpServletRequest request,Long id) {
		adminRepository.UnblockUser(id);
		return "redirect:Customers";
	}
	
	@RequestMapping(value="/BlockTrainer", method =RequestMethod.GET)
	public String BlockTrainer(Model model,HttpServletRequest request,Long id) {
		adminRepository.blockUser(id);
		return "redirect:Trainers";
	}
	
	@RequestMapping(value="/UnblockTrainer", method =RequestMethod.GET)
	public String UnblockTrainer(Model model,HttpServletRequest request,Long id) {
		adminRepository.UnblockUser(id);
		return "redirect:Trainers";
	}
	
	@RequestMapping(value="/BlockProvider", method =RequestMethod.GET)
	public String BlockProvider(Model model,HttpServletRequest request,Long id) {
		adminRepository.blockUser(id);
		return "redirect:LocalProviders";
	}
	
	@RequestMapping(value="/UnblockProvider", method =RequestMethod.GET)
	public String UnblockProvider(Model model,HttpServletRequest request,Long id) {
		adminRepository.UnblockUser(id);
		return "redirect:LocalProviders";
	}
	
	
	@RequestMapping(value="/Customers", method =RequestMethod.GET)
	public String Customers(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Client> customers=clientRepository.findByType("Customer");
		model.addAttribute("customers", customers);
		model.addAttribute("session", session.getAttribute("user"));
		return "CustomersForAdmin";
	}
	@RequestMapping(value="/Trainers", method =RequestMethod.GET)
	public String Trainers(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Client> trainers=clientRepository.findByType("Trainer");
		model.addAttribute("trainers", trainers);
		model.addAttribute("session", session.getAttribute("user"));
		return "TrainersForAdmin";
	}
	@RequestMapping(value="/LocalProviders", method =RequestMethod.GET)
	public String LocalProviders(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Client> providers=clientRepository.findByType("Local Provider");
		model.addAttribute("providers", providers);
		model.addAttribute("session", session.getAttribute("user"));
		return "ProvidersForAdmin";
	}
	@RequestMapping(value="/Trainings", method =RequestMethod.GET)
	public String Trainings(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Formation> formations=formationRepository.findAll();
		model.addAttribute("trainings", formations);
		model.addAttribute("session", session.getAttribute("user"));
		return "TrainingsForAdmin";
	}
	@RequestMapping(value="/Locals", method =RequestMethod.GET)
	public String Locals(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Local> locals=localRepository.findAll();
		model.addAttribute("locals", locals);
		model.addAttribute("session", session.getAttribute("user"));
		return "LocalsForAdmin";
	}
	@RequestMapping(value="/Comments", method =RequestMethod.GET)
	public String Comments(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		
		model.addAttribute("session", session.getAttribute("user"));
		return "CommentsForAdmin";
	}
	
	

}

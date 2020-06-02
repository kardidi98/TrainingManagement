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
import org.sid.dao.CategoryRepository;
import org.sid.dao.CityRepository;
import org.sid.dao.ClientRepository;
import org.sid.dao.CommentaireRepository;
import org.sid.dao.FormationRepository;
import org.sid.dao.LocalRepository;
import org.sid.entities.Admin;
import org.sid.entities.Category;
import org.sid.entities.Client;
import org.sid.entities.Commentaire;
import org.sid.entities.Formation;
import org.sid.entities.Local;
import org.sid.entities.Ville;
import org.sid.mailSender.NotificationService;
import org.sid.services.LocalService;
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
	private CityRepository cityRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private CommentaireRepository commentRepository;
	@Autowired
	private LocalService localService;
	@Autowired
	private LocalController localController;
	@Autowired
	private NotificationService notificationService;
	
	private String TrainingPicture;
	private String picture1;
	private String picture2;
	private String picture3;
	private String picture4;
	private String picture5;
	private String picture6;

	@Value("${dir.userimages}")
	private String userimageDir;
	
	@Value("${dir.images}")
	private String imageDir;
	@Value("${dir.Localimages}")
	private String localImageDir;

	@RequestMapping(value="/", method = RequestMethod.GET)
	public String admin(Model model,HttpServletRequest request) {

		HttpSession session=request.getSession(true);
		
		List<Admin> admin= adminRepository.findAll();
		if(admin.size()==0) {
			Admin Admin=new Admin();
			Admin.setEmail("ghikkprojet@gmail.com");
			Admin.setNom("Administrator");
			Admin.setPrenom("Ghikk");
			Admin.setPassword("ghikkghikk");
			adminRepository.save(Admin);
		}
		else {
			Object obj=session.getAttribute("user");
			if(session.getAttribute("user")!=null && obj instanceof Client){
				Client client=(Client) session.getAttribute("user");
				if(client.getEmail()!=admin.get(0).getEmail()) {
					return "redirect:TrainingManagement";
				}
			}
			
		}
		

		long countUsers=adminRepository.countUsers();

		Map<String, Integer> surveyMap = new LinkedHashMap<>();
		surveyMap.put("2020", 100);
		surveyMap.put("2021", 350);
		surveyMap.put("2022", 400);
		surveyMap.put("2023", 550);
		surveyMap.put("2024", 600);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());

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
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
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

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		List<Local> allLocals= localService.getLocals();
		List<String> localVilles=localService.getLocalsVilles();
		List<String> localCategories=localService.getLocalsCategories();
		model.addAttribute("locaux",allLocals);
		model.addAttribute("localVilles",localVilles);
		model.addAttribute("localCategories",localCategories);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "UpdateArticleForAdmin";
	}


	@RequestMapping(value="/UpdateTraining", method =RequestMethod.POST)

	public String update(Model model,HttpServletRequest request, Formation formation,@RequestParam("localId") Long localId,@RequestParam(name="picture") MultipartFile file, BindingResult bindingResult) throws IllegalStateException, IOException {
		HttpSession session =request.getSession(true);
		formation.setUser((Client) formation.getUser());

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
	

	@RequestMapping(value="/deleteLocal", method =RequestMethod.GET)
	public String deleteLocal(Long id,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		Local local = localRepository.getOne(id);
		
		
		
		List<String> clientsEmails= localRepository.findParticipants(id);
		try {
			notificationService.sendNotificationIfArticleRemoved(clientsEmails, local);
		} catch (Exception e) {

		}
		localRepository.localDeleted(id);
		localRepository.deleteById(id);
		return "redirect:Locals";
	}

	
	@RequestMapping(value="/editLocal", method =RequestMethod.GET)
	public String editLocal(Model model, Long id) throws IllegalStateException, IOException {
		Local local =localRepository.getOne(id);
		model.addAttribute("local",local );
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		picture1=local.getPicture1();
		picture2=local.getPicture2();

		picture3=local.getPicture3();
		picture4=local.getPicture4();
		picture5=local.getPicture5();
		picture6=local.getPicture6();
		return "UpdateLocalForAdmin";
	}


	@RequestMapping(value="/updatelocal", method =RequestMethod.POST)
	public String updatelocal(Model model, Local local,
			@RequestParam("category") String category,
			@RequestParam("ville") String ville,
			@RequestParam(name="owner") Client owner,
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

		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		
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
		local.setDisponibiliteFrom(dateFrom);
		local.setCategory(category);
		local.setVille(ville);
//		Client client=clientRepository.getOne(owner);
//		System.out.println(ownerId);
		local.setOwner(owner);
		

		localRepository.save(local);
	
		List<String> clientsEmails= localRepository.findParticipants(local.getId());

		try {
			notificationService.sendNotificationIfArticleUpdated(clientsEmails, local);
		} catch (Exception e) {

		}


		return "redirect:Locals";	
	}
	
	@RequestMapping(value="/BlockUser", method =RequestMethod.GET)
	public String BlockUser(Model model,HttpServletRequest request,Long id) {
		adminRepository.blockUser(id);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "redirect:Trainers";
	}
	
	@RequestMapping(value="/UnblockTrainer", method =RequestMethod.GET)
	public String UnblockTrainer(Model model,HttpServletRequest request,Long id) {
		adminRepository.UnblockUser(id);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "redirect:Trainers";
	}
	
	@RequestMapping(value="/BlockProvider", method =RequestMethod.GET)
	public String BlockProvider(Model model,HttpServletRequest request,Long id) {
		adminRepository.blockUser(id);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "redirect:LocalProviders";
	}
	
	@RequestMapping(value="/UnblockProvider", method =RequestMethod.GET)
	public String UnblockProvider(Model model,HttpServletRequest request,Long id) {
		adminRepository.UnblockUser(id);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
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
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		List<String> localVilles=localService.getLocalsVilles();
		List<String> trainingCategories=formationRepository.getTrainingsCategories();
		
		model.addAttribute("localVilles",localVilles);
		model.addAttribute("trainingCategories",trainingCategories);
		
		return "TrainingsForAdmin";
	}
	@RequestMapping(value="/Locals", method =RequestMethod.GET)
	public String Locals(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		List<Local> locals=localRepository.findAll();
		List<Local> allLocals= localService.getLocals();
		List<String> localVilles=localService.getLocalsVilles();
		List<String> localCategories=localService.getLocalsCategories();
		model.addAttribute("locaux",allLocals);
		model.addAttribute("localVilles",localVilles);
		model.addAttribute("localCategories",localCategories);
		model.addAttribute("locals", locals);
		model.addAttribute("session", session.getAttribute("user"));
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		return "LocalsForAdmin";
	}
	@RequestMapping(value="/Comments", method =RequestMethod.GET)
	public String Comments(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		long countUsers=adminRepository.countUsers();
		model.addAttribute("countusers", countUsers);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("comments",commentRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		return "CommentsForAdmin";
	}
	
	@RequestMapping(value="/addCity", method =RequestMethod.GET)
	public String addCity(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		model.addAttribute("ville", new Ville());
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		return "addCity";
	}
	@RequestMapping(value="/saveCity", method =RequestMethod.POST)
	public String saveCity(Model model,HttpServletRequest request,@RequestParam(name="ville") String ville) {
		HttpSession session=request.getSession(true);
		Ville city= new Ville();
		city.setVille(ville);
		cityRepository.save(city);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		return "redirect:/";
	}
	
	@RequestMapping(value="/addCategory", method =RequestMethod.GET)
	public String addCategory(Model model,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		model.addAttribute("category", new Category());
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		return "addCategory";
	}
	@RequestMapping(value="/saveCategory", method =RequestMethod.POST)
	public String saveCategory(Model model,HttpServletRequest request,@RequestParam(name="category") String cat) {
		HttpSession session=request.getSession(true);
		Category category= new Category();
		category.setCategory(cat);
		categoryRepository.save(category);
		
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("cities", cityRepository.findAll());
		
		model.addAttribute("session", session.getAttribute("user"));
		return "redirect:/";
	}
	
	@RequestMapping(value="/deleteComment", method =RequestMethod.GET)
	public String deleteComment(Long id,HttpServletRequest request) {
		HttpSession session=request.getSession(true);
		commentRepository.deleteById(id);
		return "redirect:Comments";
	}

	

}
